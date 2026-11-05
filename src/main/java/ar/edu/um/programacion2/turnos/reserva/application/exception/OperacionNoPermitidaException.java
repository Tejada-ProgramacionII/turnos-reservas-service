package ar.edu.um.programacion2.turnos.reserva.application.exception;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import lombok.Getter;

// El estado del proceso no admite la operación pedida; el proceso no cambió.
@Getter
public class OperacionNoPermitidaException extends RuntimeException {

    private final EstadoProceso estado;

    public OperacionNoPermitidaException(String mensaje, EstadoProceso estado) {
        super(mensaje);
        this.estado = estado;
    }
}
