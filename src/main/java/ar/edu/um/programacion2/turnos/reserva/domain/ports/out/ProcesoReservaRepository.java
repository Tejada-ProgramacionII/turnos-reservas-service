package ar.edu.um.programacion2.turnos.reserva.domain.ports.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;

public interface ProcesoReservaRepository {
    ProcesoReserva save(ProcesoReserva proceso);

    Optional<ProcesoReserva> findById(Long id);

    Optional<ProcesoReserva> findByReservationProcessId(String reservationProcessId);

    List<ProcesoReserva> findByEstados(Set<EstadoProceso> estados);

    List<ProcesoReserva> findConfirmadosDesde(LocalDate fecha);
}
