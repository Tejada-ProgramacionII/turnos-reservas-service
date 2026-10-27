package ar.edu.um.programacion2.turnos.disponibilidad.domain.ports.out;

import java.util.Optional;

import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.AgendaProfesional;

public interface CatalogoClient {
    Optional<AgendaProfesional> consultarAgenda(Long profesionalId);
}
