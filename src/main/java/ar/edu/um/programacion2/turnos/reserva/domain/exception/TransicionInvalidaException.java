package ar.edu.um.programacion2.turnos.reserva.domain.exception;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;

public class TransicionInvalidaException extends RuntimeException {
    public TransicionInvalidaException(EstadoProceso actual, EstadoProceso nuevo) {
        super("El proceso no puede pasar de " + actual + " a " + nuevo);
    }
}
