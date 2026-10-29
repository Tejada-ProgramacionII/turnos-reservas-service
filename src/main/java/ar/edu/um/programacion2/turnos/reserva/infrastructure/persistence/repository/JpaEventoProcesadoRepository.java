package ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.entity.EventoProcesadoEntity;

@Repository
public interface JpaEventoProcesadoRepository extends JpaRepository<EventoProcesadoEntity, String> {
}
