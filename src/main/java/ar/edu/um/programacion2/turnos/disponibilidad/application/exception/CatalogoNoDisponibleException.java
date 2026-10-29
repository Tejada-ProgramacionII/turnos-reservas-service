package ar.edu.um.programacion2.turnos.disponibilidad.application.exception;

public class CatalogoNoDisponibleException extends RuntimeException {
    public CatalogoNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
