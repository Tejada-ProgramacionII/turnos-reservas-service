package ar.edu.um.programacion2.turnos.reserva.infrastructure.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EventoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.model.TipoEventoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.in.ProcesarEventoReservaUseCase;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.messaging.mapper.EventoReservaMessageMapper;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class EventoReservaConsumerTest {

    // Ejemplos de las secciones 15.4 y 15.6 a 15.10 del contrato.
    private static final String PEDIDO = """
            {
              "eventId": "request-event-8a31",
              "eventType": "AdditionalInformationRequested",
              "reservationProcessId": "process-a92b47d0",
              "holdId": "hold-6c0c2f41",
              "requiredField": "PHONE_NUMBER",
              "message": "Ingrese un numero de telefono para continuar.",
              "expiresAt": "2026-09-10T14:44:50Z",
              "occurredAt": "2026-09-10T14:35:10Z",
              "schemaVersion": 1
            }
            """;

    private static final String CONFIRMADO = """
            {
              "eventId": "confirmation-event-7d10",
              "eventType": "AppointmentConfirmed",
              "groupId": "proyecto-juan-perez",
              "reservationProcessId": "process-a92b47d0",
              "holdId": "hold-6c0c2f41",
              "requestEventId": "request-event-8a31",
              "reservationId": 8251,
              "professionalId": 15,
              "date": "2026-09-14",
              "startTime": "09:30",
              "status": "CONFIRMED",
              "occurredAt": "2026-09-10T14:36:01Z",
              "schemaVersion": 1
            }
            """;

    private static final String RECHAZADO = """
            {
              "eventId": "rejection-event-7d11",
              "eventType": "AdditionalInformationRejected",
              "groupId": "proyecto-juan-perez",
              "reservationProcessId": "process-a92b47d0",
              "holdId": "hold-6c0c2f41",
              "requestEventId": "request-event-8a31",
              "reason": "INVALID_PHONE_NUMBER",
              "message": "El telefono ingresado no es valido.",
              "expiresAt": "2026-09-10T14:44:50Z",
              "occurredAt": "2026-09-10T14:36:01Z",
              "schemaVersion": 1
            }
            """;

    private static final String VENCIDO = """
            {
              "eventId": "process-event-7d12",
              "eventType": "AppointmentProcessExpired",
              "groupId": "proyecto-juan-perez",
              "reservationProcessId": "process-a92b47d0",
              "holdId": "hold-6c0c2f41",
              "requestEventId": "request-event-8a31",
              "reason": "HOLD_EXPIRED",
              "occurredAt": "2026-09-10T14:44:52Z",
              "schemaVersion": 1
            }
            """;

    private static final String INVALIDO = """
            {
              "eventId": "process-event-7d13",
              "eventType": "AppointmentProcessInvalid",
              "groupId": "proyecto-juan-perez",
              "reservationProcessId": "process-a92b47d0",
              "holdId": "hold-6c0c2f41",
              "requestEventId": "request-event-8a31",
              "reason": "REQUEST_EVENT_MISMATCH",
              "occurredAt": "2026-09-10T14:36:01Z",
              "schemaVersion": 1
            }
            """;

    private static final String CANCELADO = """
            {
              "eventId": "cancellation-event-7d14",
              "eventType": "AppointmentCancelled",
              "groupId": "proyecto-juan-perez",
              "reservationProcessId": "process-a92b47d0",
              "holdId": "hold-6c0c2f41",
              "requestEventId": "request-event-8a31",
              "reservationId": 8251,
              "professionalId": 15,
              "date": "2026-09-14",
              "startTime": "09:30",
              "status": "CANCELLED",
              "reason": null,
              "occurredAt": "2026-09-11T15:10:00Z",
              "schemaVersion": 1
            }
            """;

    @Mock
    private ProcesarEventoReservaUseCase procesarEventoReservaUseCase;

    private EventoReservaConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new EventoReservaConsumer(JsonMapper.builder().build(), new EventoReservaMessageMapper(),
                procesarEventoReservaUseCase);
    }

    @Test
    void elPedidoDeTelefonoLlegaConSuEventIdMensajeYVencimiento() {
        EventoReserva evento = recibir(PEDIDO);

        assertThat(evento.getTipo()).isEqualTo(TipoEventoReserva.INFORMACION_SOLICITADA);
        assertThat(evento.getEventId()).isEqualTo("request-event-8a31");
        assertThat(evento.getReservationProcessId()).isEqualTo("process-a92b47d0");
        assertThat(evento.getMensaje()).isEqualTo("Ingrese un numero de telefono para continuar.");
        assertThat(evento.getExpiresAt()).isEqualTo(Instant.parse("2026-09-10T14:44:50Z"));
    }

    @Test
    void laConfirmacionLlegaConElReservationId() {
        EventoReserva evento = recibir(CONFIRMADO);

        assertThat(evento.getTipo()).isEqualTo(TipoEventoReserva.TURNO_CONFIRMADO);
        assertThat(evento.getReservationId()).isEqualTo(8251L);
    }

    @Test
    void elRechazoLlegaConMotivoMensajeYVencimiento() {
        EventoReserva evento = recibir(RECHAZADO);

        assertThat(evento.getTipo()).isEqualTo(TipoEventoReserva.INFORMACION_RECHAZADA);
        assertThat(evento.getMotivo()).isEqualTo("INVALID_PHONE_NUMBER");
        assertThat(evento.getMensaje()).isEqualTo("El telefono ingresado no es valido.");
        assertThat(evento.getExpiresAt()).isEqualTo(Instant.parse("2026-09-10T14:44:50Z"));
    }

    @Test
    void elVencimientoLlegaConSuMotivo() {
        EventoReserva evento = recibir(VENCIDO);

        assertThat(evento.getTipo()).isEqualTo(TipoEventoReserva.PROCESO_VENCIDO);
        assertThat(evento.getMotivo()).isEqualTo("HOLD_EXPIRED");
    }

    @Test
    void laInvalidezLlegaConSuMotivo() {
        EventoReserva evento = recibir(INVALIDO);

        assertThat(evento.getTipo()).isEqualTo(TipoEventoReserva.PROCESO_INVALIDO);
        assertThat(evento.getMotivo()).isEqualTo("REQUEST_EVENT_MISMATCH");
    }

    @Test
    void laCancelacionPuedeLlegarSinMotivo() {
        EventoReserva evento = recibir(CANCELADO);

        assertThat(evento.getTipo()).isEqualTo(TipoEventoReserva.TURNO_CANCELADO);
        assertThat(evento.getMotivo()).isNull();
    }

    @Test
    void ignoraLosCamposQueNoConoce() {
        EventoReserva evento = recibir(VENCIDO.replace("\"schemaVersion\": 1", "\"schemaVersion\": 1, \"nuevo\": {\"a\": 1}"));

        assertThat(evento.getTipo()).isEqualTo(TipoEventoReserva.PROCESO_VENCIDO);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"eventId\": \"e-1\", \"eventType\": \"AdditionalInformationSubmitted\", \"reservationProcessId\": \"p-1\"}",
            "{\"eventId\": \"e-1\", \"eventType\": \"CatalogUpdated\", \"reservationProcessId\": \"p-1\"}",
            "{\"eventId\": \"e-1\", \"reservationProcessId\": \"p-1\"}",
            "{\"eventType\": \"AppointmentConfirmed\", \"reservationProcessId\": \"p-1\"}",
            "{\"eventId\": \"e-1\", \"eventType\": \"AppointmentConfirmed\"}",
            "esto no es JSON"})
    void descartaLosMensajesDesconocidosIncompletosOIlegibles(String mensaje) {
        consumer.recibir(mensaje);

        verifyNoInteractions(procesarEventoReservaUseCase);
    }

    @Test
    void siProcesarFallaLaExcepcionLlegaAlContenedorParaQueReintente() {
        doThrow(new IllegalStateException("base caída")).when(procesarEventoReservaUseCase).procesarEvento(any());

        assertThatThrownBy(() -> consumer.recibir(PEDIDO)).isInstanceOf(IllegalStateException.class);
    }

    private EventoReserva recibir(String mensaje) {
        consumer.recibir(mensaje);
        ArgumentCaptor<EventoReserva> captor = ArgumentCaptor.forClass(EventoReserva.class);
        verify(procesarEventoReservaUseCase).procesarEvento(captor.capture());
        return captor.getValue();
    }
}
