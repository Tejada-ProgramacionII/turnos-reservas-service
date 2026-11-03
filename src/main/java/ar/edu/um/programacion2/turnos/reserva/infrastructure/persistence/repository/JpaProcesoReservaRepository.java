package ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.entity.ProcesoReservaEntity;

@Repository
public interface JpaProcesoReservaRepository extends JpaRepository<ProcesoReservaEntity, Long> {
    Optional<ProcesoReservaEntity> findByReservationProcessId(String reservationProcessId);

    List<ProcesoReservaEntity> findByEstadoIn(Collection<EstadoProceso> estados);

    List<ProcesoReservaEntity> findByEstadoAndFechaGreaterThanEqual(EstadoProceso estado, LocalDate fecha);
}
