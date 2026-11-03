package ar.edu.um.programacion2.turnos.reserva.domain.ports.out;

import java.util.Optional;

import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;

public interface ProcesoReservaRepository {
    ProcesoReserva save(ProcesoReserva proceso);

    Optional<ProcesoReserva> findById(Long id);

    Optional<ProcesoReserva> findByReservationProcessId(String reservationProcessId);
}
