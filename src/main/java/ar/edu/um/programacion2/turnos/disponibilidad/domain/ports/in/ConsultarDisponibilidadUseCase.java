package ar.edu.um.programacion2.turnos.disponibilidad.domain.ports.in;

import java.time.LocalDate;
import java.util.Optional;

import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.Disponibilidad;

public interface ConsultarDisponibilidadUseCase {
    Optional<Disponibilidad> consultarDisponibilidad(Long profesionalId, LocalDate fecha);
}
