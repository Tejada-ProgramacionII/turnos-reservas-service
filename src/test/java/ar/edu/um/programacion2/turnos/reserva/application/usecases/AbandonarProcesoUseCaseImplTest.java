package ar.edu.um.programacion2.turnos.reserva.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.programacion2.turnos.reserva.application.exception.OperacionNoPermitidaException;
import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ProcesoReservaRepository;
import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AbandonarProcesoUseCaseImplTest {

    private static final String LOGIN = "jtejada";
    private static final Long USUARIO_ID = 7L;
    private static final Long PROCESO_ID = 1L;
    private static final Instant AHORA = Instant.parse("2026-10-27T18:00:00Z");

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProcesoReservaRepository procesoReservaRepository;

    private AbandonarProcesoUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AHORA, ZoneId.of("America/Argentina/Mendoza"));
        useCase = new AbandonarProcesoUseCaseImpl(usuarioRepository, procesoReservaRepository, clock);
        when(usuarioRepository.findByLogin(LOGIN)).thenReturn(Optional.of(Usuario.builder().id(USUARIO_ID).build()));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoProceso.class,
            names = {"INICIADO", "BLOQUEADO", "ESPERANDO_TELEFONO"})
    void unProcesoPendienteQuedaCanceladoComoAbandonado(EstadoProceso estado) {
        when(procesoReservaRepository.findById(PROCESO_ID)).thenReturn(Optional.of(proceso(estado)));
        when(procesoReservaRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        Optional<ProcesoReserva> resultado = useCase.abandonarProceso(LOGIN, PROCESO_ID);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getEstado()).isEqualTo(EstadoProceso.CANCELADO);
        assertThat(resultado.get().getMotivo()).isEqualTo("Abandonado por el usuario");
        assertThat(resultado.get().getActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    void unProcesoConfirmadoSeRechazaIndicandoLaCancelacion() {
        when(procesoReservaRepository.findById(PROCESO_ID)).thenReturn(Optional.of(proceso(EstadoProceso.CONFIRMADO)));

        assertNoPermitido(EstadoProceso.CONFIRMADO, "usá la cancelación de la reserva (CU-08)");
    }

    @Test
    void conElTelefonoEnviadoSeRechazaIndicandoEsperarElResultado() {
        when(procesoReservaRepository.findById(PROCESO_ID))
                .thenReturn(Optional.of(proceso(EstadoProceso.TELEFONO_ENVIADO)));

        assertNoPermitido(EstadoProceso.TELEFONO_ENVIADO, "esperá el resultado y, si se confirma, cancelala (CU-08)");
    }

    @ParameterizedTest
    @EnumSource(value = EstadoProceso.class, names = {"CANCELADO", "VENCIDO", "INVALIDO", "FALLIDO"})
    void unProcesoFinalSeRechazaInformandoElEstado(EstadoProceso estado) {
        when(procesoReservaRepository.findById(PROCESO_ID)).thenReturn(Optional.of(proceso(estado)));

        assertNoPermitido(estado, "El proceso ya terminó");
    }

    @Test
    void unProcesoAjenoSeTrataComoInexistente() {
        ProcesoReserva ajeno = proceso(EstadoProceso.BLOQUEADO);
        ajeno.setUsuarioId(99L);
        when(procesoReservaRepository.findById(PROCESO_ID)).thenReturn(Optional.of(ajeno));

        assertThat(useCase.abandonarProceso(LOGIN, PROCESO_ID)).isEmpty();
        assertThat(ajeno.getEstado()).isEqualTo(EstadoProceso.BLOQUEADO);
        verify(procesoReservaRepository, never()).save(any());
    }

    @Test
    void unProcesoInexistenteDevuelveVacio() {
        when(procesoReservaRepository.findById(PROCESO_ID)).thenReturn(Optional.empty());

        assertThat(useCase.abandonarProceso(LOGIN, PROCESO_ID)).isEmpty();
    }

    private void assertNoPermitido(EstadoProceso estado, String mensaje) {
        assertThatThrownBy(() -> useCase.abandonarProceso(LOGIN, PROCESO_ID))
                .isInstanceOfSatisfying(OperacionNoPermitidaException.class,
                        e -> assertThat(e.getEstado()).isEqualTo(estado))
                .hasMessageContaining(mensaje);
        verify(procesoReservaRepository, never()).save(any());
    }

    private static ProcesoReserva proceso(EstadoProceso estado) {
        return ProcesoReserva.builder()
                .id(PROCESO_ID)
                .usuarioId(USUARIO_ID)
                .estado(estado)
                .build();
    }
}
