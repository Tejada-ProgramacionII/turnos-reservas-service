package ar.edu.um.programacion2.turnos.reserva.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.programacion2.turnos.reserva.application.exception.CatedraNoDisponibleException;
import ar.edu.um.programacion2.turnos.reserva.application.exception.CatedraRechazoException;
import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoReservaCatedra;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ReservaCatedra;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ResultadoReconciliacion;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ProcesoReservaRepository;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ReservasCatedraClient;
import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ReconciliarProcesosUseCaseImplTest {

    // Martes 3/11/2026 a las 15:00 en Mendoza.
    private static final Instant AHORA = Instant.parse("2026-11-03T18:00:00Z");
    private static final LocalDate HOY = LocalDate.of(2026, 11, 3);
    private static final Long USUARIO_ID = 7L;
    private static final UUID PACIENTE = UUID.fromString("3f2b8c1e-5d4a-4e7b-9a10-2c6d8e4f1b23");
    private static final String NO_APARECE = "NO_APARECE";
    private static final String DESCONOCIDO = "DESCONOCIDO";
    private static final String SIN_CAMBIO = "SIN_CAMBIO";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProcesoReservaRepository procesoReservaRepository;

    @Mock
    private ReservasCatedraClient reservasCatedraClient;

    private ReconciliarProcesosUseCaseImpl useCase;
    private final List<ProcesoReserva> pendientes = new ArrayList<>();
    private final List<ProcesoReserva> confirmados = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(AHORA, ZoneId.of("America/Argentina/Mendoza"));
        useCase = new ReconciliarProcesosUseCaseImpl(usuarioRepository, procesoReservaRepository,
                reservasCatedraClient, clock);
        lenient().when(procesoReservaRepository.findByEstados(anySet())).thenReturn(pendientes);
        lenient().when(procesoReservaRepository.findConfirmadosDesde(HOY)).thenReturn(confirmados);
        lenient().when(procesoReservaRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        lenient().when(usuarioRepository.findById(USUARIO_ID))
                .thenReturn(Optional.of(Usuario.builder().id(USUARIO_ID).externalPatientId(PACIENTE).build()));
    }

    @ParameterizedTest(name = "{0} y la cátedra {1} (vencido: {2}) -> {3}")
    @CsvSource({
            "BLOQUEADO,          CONFIRMADA,         false, CONFIRMADO",
            "ESPERANDO_TELEFONO, CONFIRMADA,         false, CONFIRMADO",
            "TELEFONO_ENVIADO,   CONFIRMADA,         true,  CONFIRMADO",
            "BLOQUEADO,          CANCELADA,          false, CANCELADO",
            "TELEFONO_ENVIADO,   CANCELADA,          false, CANCELADO",
            "ESPERANDO_TELEFONO, FALLIDA,            true,  VENCIDO",
            "TELEFONO_ENVIADO,   FALLIDA,            false, FALLIDO",
            "BLOQUEADO,          TELEFONO_PENDIENTE, false, ESPERANDO_TELEFONO",
            "ESPERANDO_TELEFONO, TELEFONO_PENDIENTE, false, SIN_CAMBIO",
            "TELEFONO_ENVIADO,   TELEFONO_PENDIENTE, false, SIN_CAMBIO",
            "ESPERANDO_TELEFONO, TELEFONO_PENDIENTE, true,  VENCIDO",
            "BLOQUEADO,          NO_APARECE,         false, SIN_CAMBIO",
            "BLOQUEADO,          NO_APARECE,         true,  VENCIDO",
            "TELEFONO_ENVIADO,   NO_APARECE,         true,  VENCIDO",
            "BLOQUEADO,          DESCONOCIDO,        true,  SIN_CAMBIO",
            "CONFIRMADO,         CANCELADA,          false, CANCELADO",
            "CONFIRMADO,         CONFIRMADA,         false, SIN_CAMBIO",
            "CONFIRMADO,         NO_APARECE,         true,  SIN_CAMBIO"})
    void llevaElProcesoAlEstadoQueInformaLaCatedra(EstadoProceso local, String enCatedra, boolean vencido,
            String esperado) {
        ProcesoReserva proceso = proceso(1L, local, vencido);
        responderCatedra(enCatedra);

        Optional<ResultadoReconciliacion> resultado = useCase.reconciliar();

        boolean cambia = !SIN_CAMBIO.equals(esperado);
        assertThat(proceso.getEstado()).isEqualTo(cambia ? EstadoProceso.valueOf(esperado) : local);
        verify(procesoReservaRepository, times(cambia ? 1 : 0)).save(proceso);
        assertThat(resultado).hasValueSatisfying(r -> {
            assertThat(r.getRevisados()).isEqualTo(1);
            assertThat(r.getCambiados()).isEqualTo(cambia ? 1 : 0);
        });
        if (cambia) {
            assertThat(proceso.getMotivo()).startsWith("Reconciliación:");
            assertThat(proceso.getActualizadoEn()).isEqualTo(AHORA);
        }
    }

    @Test
    void alConfirmarGuardaElReservationIdDeLaCatedra() {
        ProcesoReserva proceso = proceso(1L, EstadoProceso.ESPERANDO_TELEFONO, false);
        responderCatedra("CONFIRMADA");

        useCase.reconciliar();

        assertThat(proceso.getReservationId()).isEqualTo(1251L);
        assertThat(proceso.getMotivo()).isEqualTo(ReconciliarProcesosUseCaseImpl.MOTIVO_CONFIRMADA);
    }

    @Test
    void pasarAEsperandoTelefonoNoInventaElRequestEventId() {
        ProcesoReserva proceso = proceso(1L, EstadoProceso.BLOQUEADO, false);
        responderCatedra("TELEFONO_PENDIENTE");

        useCase.reconciliar();

        assertThat(proceso.getEstado()).isEqualTo(EstadoProceso.ESPERANDO_TELEFONO);
        assertThat(proceso.getRequestEventId()).isNull();
    }

    @ParameterizedTest(name = "creado hace {0} minutos -> {1}")
    @CsvSource({"10, FALLIDO", "11, FALLIDO", "9, INICIADO"})
    void unIniciadoFallaRecienPasadaLaVigenciaMaximaDeUnHoldSinConsultarALaCatedra(long minutos,
            EstadoProceso esperado) {
        ProcesoReserva proceso = ProcesoReserva.builder().id(1L).usuarioId(USUARIO_ID)
                .estado(EstadoProceso.INICIADO).creadoEn(AHORA.minusSeconds(minutos * 60)).build();
        pendientes.add(proceso);
        lenient().when(procesoReservaRepository.findById(1L)).thenReturn(Optional.of(proceso));

        useCase.reconciliar();

        assertThat(proceso.getEstado()).isEqualTo(esperado);
        if (esperado == EstadoProceso.FALLIDO) {
            assertThat(proceso.getMotivo()).isEqualTo("Resultado incierto al crear el hold");
        }
        verifyNoInteractions(reservasCatedraClient);
    }

    @Test
    void siLaCatedraNoRespondeNoTocaLosProcesosDeEseUsuarioYSigueConLosDemas() {
        ProcesoReserva delUsuario = proceso(1L, EstadoProceso.ESPERANDO_TELEFONO, true);
        Long otroUsuario = 8L;
        UUID otroPaciente = UUID.fromString("619d94d7-1e87-44bf-ac41-76abccf11eab");
        ProcesoReserva delOtro = proceso(2L, EstadoProceso.ESPERANDO_TELEFONO, true);
        delOtro.setUsuarioId(otroUsuario);
        when(usuarioRepository.findById(otroUsuario))
                .thenReturn(Optional.of(Usuario.builder().id(otroUsuario).externalPatientId(otroPaciente).build()));
        when(reservasCatedraClient.listarReservas(PACIENTE.toString()))
                .thenThrow(new CatedraNoDisponibleException("HTTP 500", null));
        when(reservasCatedraClient.listarReservas(otroPaciente.toString())).thenReturn(List.of());

        Optional<ResultadoReconciliacion> resultado = useCase.reconciliar();

        assertThat(delUsuario.getEstado()).isEqualTo(EstadoProceso.ESPERANDO_TELEFONO);
        assertThat(delOtro.getEstado()).isEqualTo(EstadoProceso.VENCIDO);
        assertThat(resultado).hasValueSatisfying(r -> assertThat(r.getCambiados()).isEqualTo(1));
    }

    @Test
    void unRechazoDeLaCatedraTampocoCortaLaReconciliacion() {
        ProcesoReserva proceso = proceso(1L, EstadoProceso.BLOQUEADO, true);
        when(reservasCatedraClient.listarReservas(PACIENTE.toString()))
                .thenThrow(new CatedraRechazoException("HTTP 401", null, null));

        assertThat(useCase.reconciliar()).isPresent();
        assertThat(proceso.getEstado()).isEqualTo(EstadoProceso.BLOQUEADO);
    }

    @Test
    void unProcesoQueKafkaMovioMientrasTantoNoSeToca() {
        ProcesoReserva leido = proceso(1L, EstadoProceso.ESPERANDO_TELEFONO, true);
        ProcesoReserva actual = ProcesoReserva.builder().id(1L).estado(EstadoProceso.TELEFONO_ENVIADO).build();
        when(procesoReservaRepository.findById(1L)).thenReturn(Optional.of(actual));
        responderCatedra("FALLIDA");

        useCase.reconciliar();

        assertThat(leido.getEstado()).isEqualTo(EstadoProceso.ESPERANDO_TELEFONO);
        verify(procesoReservaRepository, never()).save(any());
    }

    @Test
    void sinElUsuarioNoSeConsultaALaCatedra() {
        proceso(1L, EstadoProceso.BLOQUEADO, true);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.empty());

        assertThat(useCase.reconciliar()).isPresent();
        verifyNoInteractions(reservasCatedraClient);
    }

    @Test
    void sinProcesosPendientesNoHaceNada() {
        assertThat(useCase.reconciliar())
                .hasValueSatisfying(r -> assertThat(r.getRevisados()).isZero());
        verifyNoInteractions(reservasCatedraClient);
    }

    @Test
    void unaSegundaVueltaMientrasCorreLaPrimeraSeSaltea() throws Exception {
        proceso(1L, EstadoProceso.BLOQUEADO, false);
        CountDownLatch adentro = new CountDownLatch(1);
        CountDownLatch soltar = new CountDownLatch(1);
        when(reservasCatedraClient.listarReservas(PACIENTE.toString())).thenAnswer(invocacion -> {
            adentro.countDown();
            assertThat(soltar.await(5, TimeUnit.SECONDS)).isTrue();
            return List.of();
        });

        CompletableFuture<Optional<ResultadoReconciliacion>> primera = CompletableFuture.supplyAsync(useCase::reconciliar);
        assertThat(adentro.await(5, TimeUnit.SECONDS)).isTrue();
        Optional<ResultadoReconciliacion> segunda = CompletableFuture.supplyAsync(useCase::reconciliar)
                .get(5, TimeUnit.SECONDS);
        soltar.countDown();

        assertThat(segunda).isEmpty();
        assertThat(primera.get(5, TimeUnit.SECONDS)).isPresent();
        verify(reservasCatedraClient, times(1)).listarReservas(any());
        assertThat(useCase.reconciliar()).isPresent();
    }

    private ProcesoReserva proceso(Long id, EstadoProceso estado, boolean vencido) {
        ProcesoReserva proceso = ProcesoReserva.builder()
                .id(id)
                .usuarioId(USUARIO_ID)
                .reservationProcessId("process-" + id)
                .fecha(HOY.plusDays(6))
                .expiresAt(vencido ? AHORA.minusSeconds(60) : AHORA.plusSeconds(60))
                .estado(estado)
                .build();
        (estado == EstadoProceso.CONFIRMADO ? confirmados : pendientes).add(proceso);
        lenient().when(procesoReservaRepository.findById(id)).thenReturn(Optional.of(proceso));
        return proceso;
    }

    private void responderCatedra(String enCatedra) {
        List<ReservaCatedra> reservas = switch (enCatedra) {
            case NO_APARECE -> List.of(reserva("process-otro", EstadoReservaCatedra.CONFIRMADA));
            case DESCONOCIDO -> List.of(reserva("process-1", null));
            default -> List.of(reserva("process-1", EstadoReservaCatedra.valueOf(enCatedra)));
        };
        when(reservasCatedraClient.listarReservas(PACIENTE.toString())).thenReturn(reservas);
    }

    private static ReservaCatedra reserva(String reservationProcessId, EstadoReservaCatedra estado) {
        return ReservaCatedra.builder().reservationProcessId(reservationProcessId).reservationId(1251L)
                .estado(estado).build();
    }
}
