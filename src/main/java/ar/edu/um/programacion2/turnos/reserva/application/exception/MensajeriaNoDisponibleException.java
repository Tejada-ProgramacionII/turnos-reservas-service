package ar.edu.um.programacion2.turnos.reserva.application.exception;

public class MensajeriaNoDisponibleException extends RuntimeException {
    public MensajeriaNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
