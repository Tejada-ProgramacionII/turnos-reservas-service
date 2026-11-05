package ar.edu.um.programacion2.turnos.reserva.infrastructure.messaging.mapper;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.reserva.domain.model.TelefonoInformado;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.messaging.dto.InformacionAdicionalMessage;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.messaging.dto.TelefonoInformadoMessage;

@Component
public class TelefonoInformadoMessageMapper {

    static final String EVENT_TYPE = "AdditionalInformationSubmitted";
    static final int SCHEMA_VERSION = 1;

    public TelefonoInformadoMessage toMessage(TelefonoInformado evento, String groupId) {
        if (evento == null) {
            return null;
        }
        return TelefonoInformadoMessage.builder()
                .eventId(evento.getEventId())
                .eventType(EVENT_TYPE)
                .groupId(groupId)
                .reservationProcessId(evento.getReservationProcessId())
                .holdId(evento.getHoldId())
                .requestEventId(evento.getRequestEventId())
                .information(InformacionAdicionalMessage.builder().phoneNumber(evento.getTelefono()).build())
                .occurredAt(evento.getOccurredAt())
                .schemaVersion(SCHEMA_VERSION)
                .build();
    }
}
