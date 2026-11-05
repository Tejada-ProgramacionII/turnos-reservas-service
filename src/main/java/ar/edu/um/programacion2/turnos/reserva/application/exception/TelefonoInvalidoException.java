package ar.edu.um.programacion2.turnos.reserva.application.exception;

public class TelefonoInvalidoException extends RuntimeException {
    public TelefonoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
