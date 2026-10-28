package ar.edu.um.programacion2.turnos.disponibilidad.domain.model;

import java.time.LocalDate;
import java.util.List;

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
public class Disponibilidad {
    private Long profesionalId;
    private LocalDate fecha;
    private List<TurnoLibre> turnos;
}
