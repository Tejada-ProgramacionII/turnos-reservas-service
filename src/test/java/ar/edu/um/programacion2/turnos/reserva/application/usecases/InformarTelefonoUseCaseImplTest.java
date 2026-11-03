package ar.edu.um.programacion2.turnos.reserva.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.programacion2.turnos.reserva.application.exception.MensajeriaNoDisponibleException;
import ar.edu.um.programacion2.turnos.reserva.application.exception.OperacionNoPermitidaException;
import ar.edu.um.programacion2.turnos.reserva.application.exception.TelefonoInvalidoException;
import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.model.TelefonoInformado;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ProcesoReservaRepository;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.TelefonoEventPublisher;
import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class InformarTelefonoUseCaseImplTest {

    private static final String LOGIN = "jtejada";
    private static final Long USUARIO_ID = 7L;
    private static final Long PROCESO_ID = 1L;
    private static final String TELEFONO = "+54 9 261 555-1234";
    private static final String TELEFONO_NORMALIZADO = "+5492615551234";
    private static final Instant AHORA = Instant.parse("2026-10-27T18:00:00Z");

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProcesoReservaRepository procesoReservaRepository;

    @Mock
    private TelefonoEventPublisher telefonoEventPublisher;

    private InformarTelefonoUseCaseImpl useCase;
    private final List<EstadoProceso> estadosGuardados = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AHORA, ZoneId.of("America/Argentina/Mendoza"));
        useCase = new InformarTelefonoUseCaseImpl(usuarioRepository, procesoReservaRepository,
                telefonoEventPublisher, clock);
        when(usuarioRepository.findByLogin(LOGIN)).thenReturn(Optional.of(Usuario.builder().id(USUARIO_ID).build()));
    }

    @Test
    void guardaTelefonoEnviadoAntesDePublicarYMandaTodosLosDatos() {
        ProcesoReserva proceso = esperandoTelefono();
        when(procesoReservaRepository.findById(PROCESO_ID)).thenReturn(Optional.of(proceso));
        registrarGuardados();
        doAnswer(invocacion -> {
            assertThat(estadosGuardados).containsExactly(EstadoProceso.TELEFONO_ENVIADO);
            return null;
        }).when(telefonoEventPublisher).publicar(any());

        Optional<ProcesoReserva> resultado = useCase.informarTelefono(LOGIN, PROCESO_ID, TELEFONO);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getEstado()).isEqualTo(EstadoProceso.TELEFONO_ENVIADO);
        assertThat(resultado.get().getTelefono()).isEqualTo(TELEFONO_NORMALIZADO);
        assertThat(resultado.get().getActualizadoEn()).isEqualTo(AHORA);
        TelefonoInformado evento = eventoPublicado();
        assertThat(evento.getEventId()).isNotBlank();
        assertThat(evento.getReservationProcessId()).isEqualTo("process-1");
        assertThat(evento.getHoldId()).isEqualTo("hold-1");
        assertThat(evento.getRequestEventId()).isEqualTo("request-event-1");
        assertThat(evento.getTelefono()).isEqualTo(TELEFONO_NORMALIZADO);
        assertThat(evento.getOccurredAt()).isEqualTo(AHORA);
    }

    @Test
    void elReenvioDespuesDeUnRechazoUsaUnEventIdNuevoYBorraElMotivo() {
        ProcesoReserva proceso = esperandoTelefono();
        proceso.setMotivo("INVALID_PHONE_NUMBER");
        when(procesoReservaRepository.findById(PROCESO_ID)).thenReturn(Optional.of(proceso));
        registrarGuardados();

        useCase.informarTelefono(LOGIN, PROCESO_ID, TELEFONO);
        String primerEventId = eventoPublicado().getEventId();
        proceso.pasarA(EstadoProceso.ESPERANDO_TELEFONO);
        useCase.informarTelefono(LOGIN, PROCESO_ID, TELEFONO);

        ArgumentCaptor<TelefonoInformado> captor = ArgumentCaptor.forClass(TelefonoInformado.class);
        verify(telefonoEventPublisher, times(2)).publicar(captor.capture());
        assertThat(captor.getAllValues().get(1).getEventId()).isNotEqualTo(primerEventId);
        assertThat(captor.getAllValues().get(1).getRequestEventId()).isEqualTo("request-event-1");
        assertThat(proceso.getMotivo()).isNull();
    }

    @Test
    void siKafkaNoAceptaElMensajeElProcesoVuelveAEsperandoTelefono() {
        ProcesoReserva proceso = esperandoTelefono();
        proceso.setMotivo("INVALID_PHONE_NUMBER");
        when(procesoReservaRepository.findById(PROCESO_ID)).thenReturn(Optional.of(proceso));
        registrarGuardados();
        MensajeriaNoDisponibleException falla = new MensajeriaNoDisponibleException("sin broker", null);
        doThrow(falla).when(telefonoEventPublisher).publicar(any());

        assertThatThrownBy(() -> useCase.informarTelefono(LOGIN, PROCESO_ID, TELEFONO)).isSameAs(falla);

        assertThat(estadosGuardados).containsExactly(EstadoProceso.TELEFONO_ENVIADO, EstadoProceso.ESPERANDO_TELEFONO);
        assertThat(proceso.getEstado()).isEqualTo(EstadoProceso.ESPERANDO_TELEFONO);
        assertThat(proceso.getTelefono()).isNull();
        assertThat(proceso.getMotivo()).isEqualTo("INVALID_PHONE_NUMBER");
    }

    @Test
    void siKafkaFallaPeroLlegoOtroResultadoNoSeTocaElProceso() {
        ProcesoReserva proceso = esperandoTelefono();
        ProcesoReserva confirmado = esperandoTelefono();
        confirmado.setEstado(EstadoProceso.CONFIRMADO);
        when(procesoReservaRepository.findById(PROCESO_ID))
                .thenReturn(Optional.of(proceso), Optional.of(confirmado));
        registrarGuardados();
        doThrow(new MensajeriaNoDisponibleException("sin broker", null)).when(telefonoEventPublisher).publicar(any());

        assertThatThrownBy(() -> useCase.informarTelefono(LOGIN, PROCESO_ID, TELEFONO))
                .isInstanceOf(MensajeriaNoDisponibleException.class);

        assertThat(estadosGuardados).containsExactly(EstadoProceso.TELEFONO_ENVIADO);
        assertThat(confirmado.getEstado()).isEqualTo(EstadoProceso.CONFIRMADO);
    }

    @Test
    void unTelefonoInvalidoNoCambiaElProcesoNiPublica() {
        ProcesoReserva proceso = esperandoTelefono();
        when(procesoReservaRepository.findById(PROCESO_ID)).thenReturn(Optional.of(proceso));

        assertThatThrownBy(() -> useCase.informarTelefono(LOGIN, PROCESO_ID, "261-555"))
                .isInstanceOf(TelefonoInvalidoException.class)
                .hasMessageContaining("de 7 a 15 dígitos");

        assertThat(proceso.getEstado()).isEqualTo(EstadoProceso.ESPERANDO_TELEFONO);
        verify(procesoReservaRepository, never()).save(any());
        verifyNoInteractions(telefonoEventPublisher);
    }

    @Test
    void sinElPedidoDeLaCatedraTodaviaNoSePuedeInformar() {
        ProcesoReserva proceso = esperandoTelefono();
        proceso.setRequestEventId(null);
        when(procesoReservaRepository.findById(PROCESO_ID)).thenReturn(Optional.of(proceso));

        assertNoPermitido(EstadoProceso.ESPERANDO_TELEFONO, "Todavía no se puede informar");
    }

    @ParameterizedTest
    @EnumSource(value = EstadoProceso.class, names = {"INICIADO", "BLOQUEADO"})
    void antesDelPedidoTodaviaNoSePuedeInformar(EstadoProceso estado) {
        ProcesoReserva proceso = esperandoTelefono();
        proceso.setEstado(estado);
        when(procesoReservaRepository.findById(PROCESO_ID)).thenReturn(Optional.of(proceso));

        assertNoPermitido(estado, "Todavía no se puede informar");
    }

    @ParameterizedTest
    @EnumSource(value = EstadoProceso.class,
            names = {"TELEFONO_ENVIADO", "CONFIRMADO", "CANCELADO", "VENCIDO", "INVALIDO", "FALLIDO"})
    void enOtroEstadoSeRechazaInformandoElEstado(EstadoProceso estado) {
        ProcesoReserva proceso = esperandoTelefono();
        proceso.setEstado(estado);
        when(procesoReservaRepository.findById(PROCESO_ID)).thenReturn(Optional.of(proceso));

        assertNoPermitido(estado, "no está esperando el teléfono");
    }

    @Test
    void siVencioElPlazoSeRechaza() {
        ProcesoReserva proceso = esperandoTelefono();
        proceso.setExpiresAt(AHORA);
        when(procesoReservaRepository.findById(PROCESO_ID)).thenReturn(Optional.of(proceso));

        assertNoPermitido(EstadoProceso.ESPERANDO_TELEFONO, "Venció el plazo");
    }

    @Test
    void unProcesoAjenoSeTrataComoInexistente() {
        ProcesoReserva ajeno = esperandoTelefono();
        ajeno.setUsuarioId(99L);
        when(procesoReservaRepository.findById(PROCESO_ID)).thenReturn(Optional.of(ajeno));

        assertThat(useCase.informarTelefono(LOGIN, PROCESO_ID, TELEFONO)).isEmpty();
        verifyNoInteractions(telefonoEventPublisher);
    }

    @Test
    void unProcesoInexistenteDevuelveVacio() {
        when(procesoReservaRepository.findById(PROCESO_ID)).thenReturn(Optional.empty());

        assertThat(useCase.informarTelefono(LOGIN, PROCESO_ID, TELEFONO)).isEmpty();
    }

    private void assertNoPermitido(EstadoProceso estado, String mensaje) {
        assertThatThrownBy(() -> useCase.informarTelefono(LOGIN, PROCESO_ID, TELEFONO))
                .isInstanceOfSatisfying(OperacionNoPermitidaException.class,
                        e -> assertThat(e.getEstado()).isEqualTo(estado))
                .hasMessageContaining(mensaje);
        verify(procesoReservaRepository, never()).save(any());
        verifyNoInteractions(telefonoEventPublisher);
    }

    private void registrarGuardados() {
        when(procesoReservaRepository.save(any())).thenAnswer(invocacion -> {
            ProcesoReserva guardado = invocacion.getArgument(0);
            estadosGuardados.add(guardado.getEstado());
            return guardado;
        });
    }

    private TelefonoInformado eventoPublicado() {
        ArgumentCaptor<TelefonoInformado> captor = ArgumentCaptor.forClass(TelefonoInformado.class);
        verify(telefonoEventPublisher).publicar(captor.capture());
        return captor.getValue();
    }

    private static ProcesoReserva esperandoTelefono() {
        return ProcesoReserva.builder()
                .id(PROCESO_ID)
                .usuarioId(USUARIO_ID)
                .holdId("hold-1")
                .reservationProcessId("process-1")
                .requestEventId("request-event-1")
                .expiresAt(AHORA.plusSeconds(60))
                .estado(EstadoProceso.ESPERANDO_TELEFONO)
                .build();
    }
}
