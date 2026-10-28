package ar.edu.um.programacion2.turnos.disponibilidad.domain.model;

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
public class Ocupacion {
    private LocalDate fecha;
    private LocalTime horaInicio;
    private EstadoOcupacion estado;
    private Instant venceEn;
}
