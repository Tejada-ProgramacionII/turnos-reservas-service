package ar.edu.um.programacion2.turnos.reserva.application.exception;

public class ReservaInvalidaException extends RuntimeException {
    public ReservaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
