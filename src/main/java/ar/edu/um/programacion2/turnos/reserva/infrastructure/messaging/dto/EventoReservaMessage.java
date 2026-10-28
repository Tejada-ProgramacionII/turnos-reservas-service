package ar.edu.um.programacion2.turnos.reserva.infrastructure.messaging.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Un solo DTO para todos los eventos del topic de acciones: cada eventType usa una parte de los campos.
// Los campos que no se usan o que la cátedra agregue después se ignoran (contrato 15.1).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventoReservaMessage {
    private String eventId;
    private String eventType;
    private String reservationProcessId;
    private Long reservationId;
    private String reason;
    private String message;
    private Instant expiresAt;
}
