package ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

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
public class OcupacionCatedraResponse {
    private LocalDate date;
    private LocalTime startTime;
    private String status;
    private Instant expiresAt;
}
