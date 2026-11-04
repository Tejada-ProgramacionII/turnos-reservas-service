package ar.edu.um.programacion2.turnos.reserva.domain.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Respuesta al pedido de teléfono de la cátedra (AdditionalInformationSubmitted, contrato 15.5).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelefonoInformado {
    private String eventId;
    private String reservationProcessId;
    private String holdId;
    private String requestEventId;
    private String telefono;
    private Instant occurredAt;
}
