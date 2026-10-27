package ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldCatedraResponse {
    private String holdId;
    private String reservationProcessId;
    private String status;
    private Instant expiresAt;
}
