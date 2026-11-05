package ar.edu.um.programacion2.turnos.reserva.domain.ports.in;

import java.util.Optional;

import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;

public interface InformarTelefonoUseCase {
    Optional<ProcesoReserva> informarTelefono(String login, Long id, String telefono);
}
