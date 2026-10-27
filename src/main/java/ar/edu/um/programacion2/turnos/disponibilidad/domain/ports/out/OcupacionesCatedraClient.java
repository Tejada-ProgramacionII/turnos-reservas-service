package ar.edu.um.programacion2.turnos.disponibilidad.domain.ports.out;

import java.time.LocalDate;
import java.util.List;

import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.Ocupacion;

public interface OcupacionesCatedraClient {
    List<Ocupacion> getOcupaciones(Long profesionalId, LocalDate desde, LocalDate hasta);
}
