package ar.edu.um.programacion2.turnos.reserva.application.exception;

import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import lombok.Getter;

// La cátedra rechazó el turno: el proceso ya quedó guardado en un estado final con su motivo.
@Getter
public class ReservaRechazadaException extends RuntimeException {

    private final transient ProcesoReserva proceso;

    public ReservaRechazadaException(String mensaje, ProcesoReserva proceso) {
        super(mensaje);
        this.proceso = proceso;
    }
}
