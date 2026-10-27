package ar.edu.um.programacion2.turnos.disponibilidad.application.exception;

public class CatedraNoDisponibleException extends RuntimeException {
    public CatedraNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
