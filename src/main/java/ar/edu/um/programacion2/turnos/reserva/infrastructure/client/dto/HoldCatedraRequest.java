package ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto;

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
public class HoldCatedraRequest {
    private Long professionalId;
    private LocalDate date;
    private LocalTime startTime;
    private String externalPatientId;
}
