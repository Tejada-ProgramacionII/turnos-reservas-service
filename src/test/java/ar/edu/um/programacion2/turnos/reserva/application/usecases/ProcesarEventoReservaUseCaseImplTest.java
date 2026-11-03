package ar.edu.um.programacion2.turnos.reserva.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import ar.edu.um.programacion2.turnos.reserva.domain.model.EventoProcesado;
import ar.edu.um.programacion2.turnos.reserva.domain.model.EventoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.model.TipoEventoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.EventoProcesadoRepository;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ProcesoReservaRepository;

@ExtendWith(MockitoExtension.class)
class ProcesarEventoReservaUseCaseImplTest {

    private static final String PROCESO = "process-a92b47d0";
    private static final Instant AHORA = Instant.parse("2026-10-28T14:00:00Z");
    private static final Instant VENCE = Instant.parse("2026-10-28T14:10:00Z");

    @Mock
    private ProcesoReservaRepository procesoReservaRepository;

    @Mock
    private EventoProcesadoRepository eventoProcesadoRepository;

    private ProcesarEventoReservaUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProcesarEventoReservaUseCaseImpl(procesoReservaRepository, eventoProcesadoRepository,
                Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @ParameterizedTest(name = "{0} desde {1}")
    @CsvSource({
            "INFORMACION_SOLICITADA, BLOQUEADO",
            "TURNO_CONFIRMADO, TELEFONO_ENVIADO",
            "INFORMACION_RECHAZADA, TELEFONO_ENVIADO",
            "PROCESO_VENCIDO, ESPERANDO_TELEFONO",
            "PROCESO_INVALIDO, TELEFONO_ENVIADO",
            "TURNO_CANCELADO, CONFIRMADO"})
    void cadaEventoLlevaElProcesoASuEstadoYQuedaRegistrado(TipoEventoReserva tipo, EstadoProceso origen) {
        ProcesoReserva proceso = procesoEn(origen);

        useCase.procesarEvento(evento(tipo).build());

        assertThat(proceso.getEstado()).isEqualTo(tipo.getDestino());
        assertThat(proceso.getActualizadoEn()).isEqualTo(AHORA);
        verify(procesoReservaRepository).save(proceso);
        assertThat(eventoRegistrado().getEventId()).isEqualTo("evento-1");
        assertThat(eventoRegistrado().getProcesadoEn()).isEqualTo(AHORA);
    }

    @Test
    void elPedidoDeTelefonoGuardaElRequestEventIdElMensajeYElVencimiento() {
        ProcesoReserva proceso = procesoEn(EstadoProceso.BLOQUEADO);

        useCase.procesarEvento(evento(TipoEventoReserva.INFORMACION_SOLICITADA)
                .mensaje("Ingrese un numero de telefono para continuar.")
                .expiresAt(VENCE)
                .build());

        assertThat(proceso.getRequestEventId()).isEqualTo("evento-1");
        assertThat(proceso.getMensajePedido()).isEqualTo("Ingrese un numero de telefono para continuar.");
        assertThat(proceso.getExpiresAt()).isEqualTo(VENCE);
    }

    @Test
    void elPedidoDeTelefonoConElProcesoYaEsperandoSoloGuardaLosDatos() {
        ProcesoReserva proceso = procesoEn(EstadoProceso.ESPERANDO_TELEFONO);

        useCase.procesarEvento(evento(TipoEventoReserva.INFORMACION_SOLICITADA).expiresAt(VENCE).build());

        assertThat(proceso.getEstado()).isEqualTo(EstadoProceso.ESPERANDO_TELEFONO);
        assertThat(proceso.getRequestEventId()).isEqualTo("evento-1");
        verify(procesoReservaRepository).save(proceso);
    }

    @Test
    void elRechazoDelTelefonoGuardaMotivoMensajeYVencimiento() {
        ProcesoReserva proceso = procesoEn(EstadoProceso.TELEFONO_ENVIADO);

        useCase.procesarEvento(evento(TipoEventoReserva.INFORMACION_RECHAZADA)
                .motivo("INVALID_PHONE_NUMBER")
                .mensaje("El telefono ingresado no es valido.")
                .expiresAt(VENCE)
                .build());

        assertThat(proceso.getMotivo()).isEqualTo("INVALID_PHONE_NUMBER");
        assertThat(proceso.getMensajePedido()).isEqualTo("El telefono ingresado no es valido.");
        assertThat(proceso.getExpiresAt()).isEqualTo(VENCE);
    }

    @Test
    void laConfirmacionGuardaElReservationIdYBorraElMotivoAnterior() {
        ProcesoReserva proceso = procesoEn(EstadoProceso.TELEFONO_ENVIADO);
        proceso.setMotivo("INVALID_PHONE_NUMBER");

        useCase.procesarEvento(evento(TipoEventoReserva.TURNO_CONFIRMADO).reservationId(8251L).build());

        assertThat(proceso.getReservationId()).isEqualTo(8251L);
        assertThat(proceso.getMotivo()).isNull();
    }

    @ParameterizedTest
    @CsvSource({"PROCESO_VENCIDO, HOLD_EXPIRED", "PROCESO_INVALIDO, REQUEST_EVENT_MISMATCH",
            "TURNO_CANCELADO, Cambio solicitado por el usuario"})
    void losResultadosFinalesGuardanSuMotivo(TipoEventoReserva tipo, String motivo) {
        ProcesoReserva proceso = procesoEn(EstadoProceso.TELEFONO_ENVIADO);

        useCase.procesarEvento(evento(tipo).motivo(motivo).build());

        assertThat(proceso.getMotivo()).isEqualTo(motivo);
    }

    @Test
    void unEventoYaProcesadoNoProduceEfectos() {
        when(eventoProcesadoRepository.existsByEventId("evento-1")).thenReturn(true);

        useCase.procesarEvento(evento(TipoEventoReserva.TURNO_CONFIRMADO).build());

        verifyNoInteractions(procesoReservaRepository);
        verify(eventoProcesadoRepository, never()).save(any());
    }

    @Test
    void unEventoDeUnProcesoInexistenteSeDaPorProcesado() {
        when(procesoReservaRepository.findByReservationProcessId(PROCESO)).thenReturn(Optional.empty());

        useCase.procesarEvento(evento(TipoEventoReserva.TURNO_CONFIRMADO).build());

        verify(procesoReservaRepository, never()).save(any());
        assertThat(eventoRegistrado().getEventId()).isEqualTo("evento-1");
    }

    @ParameterizedTest(name = "{0} con el proceso en {1}")
    @CsvSource({
            "TURNO_CONFIRMADO, ESPERANDO_TELEFONO",       // transición que no está en el diagrama
            "INFORMACION_RECHAZADA, CONFIRMADO",          // retrocedería un proceso confirmado
            "PROCESO_VENCIDO, CANCELADO",                 // el proceso ya es final
            "TURNO_CANCELADO, CANCELADO",                 // mismo resultado que ya llegó por REST
            "INFORMACION_SOLICITADA, VENCIDO"})
    void unEventoQueNoPuedeMoverElProcesoSeIgnoraPeroQuedaRegistrado(TipoEventoReserva tipo, EstadoProceso estado) {
        ProcesoReserva proceso = procesoEn(estado);

        useCase.procesarEvento(evento(tipo).motivo("motivo").reservationId(1L).build());

        assertThat(proceso.getEstado()).isEqualTo(estado);
        assertThat(proceso.getMotivo()).isNull();
        verify(procesoReservaRepository, never()).save(any());
        assertThat(eventoRegistrado().getEventId()).isEqualTo("evento-1");
    }

    private ProcesoReserva procesoEn(EstadoProceso estado) {
        ProcesoReserva proceso = ProcesoReserva.builder().id(1L).reservationProcessId(PROCESO).estado(estado).build();
        when(procesoReservaRepository.findByReservationProcessId(PROCESO)).thenReturn(Optional.of(proceso));
        return proceso;
    }

    private static EventoReserva.EventoReservaBuilder evento(TipoEventoReserva tipo) {
        return EventoReserva.builder().eventId("evento-1").tipo(tipo).reservationProcessId(PROCESO);
    }

    private EventoProcesado eventoRegistrado() {
        ArgumentCaptor<EventoProcesado> captor = ArgumentCaptor.forClass(EventoProcesado.class);
        verify(eventoProcesadoRepository).save(captor.capture());
        return captor.getValue();
    }
}
