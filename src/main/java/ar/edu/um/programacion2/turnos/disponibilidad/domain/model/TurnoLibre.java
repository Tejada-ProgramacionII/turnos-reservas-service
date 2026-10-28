package ar.edu.um.programacion2.turnos.disponibilidad.domain.model;

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
public class TurnoLibre {
    private LocalTime horaInicio;
    private LocalTime horaFin;
}
