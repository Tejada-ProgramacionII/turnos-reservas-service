package ar.edu.um.programacion2.turnos.reserva.infrastructure.messaging.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EventoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.model.TipoEventoReserva;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.messaging.dto.EventoReservaMessage;

class EventoReservaMessageMapperTest {

    private final EventoReservaMessageMapper mapper = new EventoReservaMessageMapper();

    @ParameterizedTest
    @EnumSource(TipoEventoReserva.class)
    void reconoceCadaEventTypeDelContrato(TipoEventoReserva tipo) {
        EventoReservaMessage message = EventoReservaMessage.builder().eventType(tipo.getEventType()).build();

        assertThat(mapper.toDomain(message).getTipo()).isEqualTo(tipo);
    }

    @Test
    void toDomainCopiaTodosLosCampos() {
        EventoReservaMessage message = EventoReservaMessage.builder()
                .eventId("rejection-event-7d11")
                .eventType("AdditionalInformationRejected")
                .reservationProcessId("process-a92b47d0")
                .reservationId(8251L)
                .reason("INVALID_PHONE_NUMBER")
                .message("El telefono ingresado no es valido.")
                .expiresAt(Instant.parse("2026-09-10T14:44:50Z"))
                .build();

        EventoReserva evento = mapper.toDomain(message);

        assertThat(evento.getEventId()).isEqualTo("rejection-event-7d11");
        assertThat(evento.getReservationProcessId()).isEqualTo("process-a92b47d0");
        assertThat(evento.getReservationId()).isEqualTo(8251L);
        assertThat(evento.getMotivo()).isEqualTo("INVALID_PHONE_NUMBER");
        assertThat(evento.getMensaje()).isEqualTo("El telefono ingresado no es valido.");
        assertThat(evento.getExpiresAt()).isEqualTo(Instant.parse("2026-09-10T14:44:50Z"));
    }

    @Test
    void toDomainDevuelveNullSiElEventTypeEsDesconocido() {
        assertThat(mapper.toDomain(EventoReservaMessage.builder().eventType("CatalogUpdated").build())).isNull();
    }

    @Test
    void toDomainDevuelveNullSiRecibeNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }
}
