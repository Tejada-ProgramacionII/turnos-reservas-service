package ar.edu.um.programacion2.turnos.reserva.infrastructure.messaging.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AdditionalInformationSubmitted tal cual el contrato 15.5: todos los campos son obligatorios.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelefonoInformadoMessage {
    private String eventId;
    private String eventType;
    private String groupId;
    private String reservationProcessId;
    private String holdId;
    private String requestEventId;
    private InformacionAdicionalMessage information;
    private Instant occurredAt;
    private Integer schemaVersion;
}
