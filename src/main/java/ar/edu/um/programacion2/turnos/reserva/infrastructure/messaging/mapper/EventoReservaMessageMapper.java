package ar.edu.um.programacion2.turnos.reserva.infrastructure.messaging.mapper;

import java.util.Arrays;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EventoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.model.TipoEventoReserva;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.messaging.dto.EventoReservaMessage;

@Component
public class EventoReservaMessageMapper {

    // Devuelve null si el eventType no es uno de los que este servicio procesa.
    public EventoReserva toDomain(EventoReservaMessage message) {
        if (message == null) {
            return null;
        }
        return Arrays.stream(TipoEventoReserva.values())
                .filter(tipo -> tipo.getEventType().equals(message.getEventType()))
                .findFirst()
                .map(tipo -> EventoReserva.builder()
                        .eventId(message.getEventId())
                        .tipo(tipo)
                        .reservationProcessId(message.getReservationProcessId())
                        .reservationId(message.getReservationId())
                        .motivo(message.getReason())
                        .mensaje(message.getMessage())
                        .expiresAt(message.getExpiresAt())
                        .build())
                .orElse(null);
    }
}
