package ar.edu.um.programacion2.turnos.reserva.domain.ports.out;

import java.util.Optional;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import ar.edu.um.programacion2.turnos.reserva.domain.model.Pagina;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;

public interface ProcesoReservaRepository {
    ProcesoReserva save(ProcesoReserva proceso);

    Optional<ProcesoReserva> findById(Long id);

    Optional<ProcesoReserva> findByReservationProcessId(String reservationProcessId);

    // estado null = todos; más recientes primero.
    Pagina<ProcesoReserva> findByUsuarioId(Long usuarioId, EstadoProceso estado, int pagina, int tamanio);
}
