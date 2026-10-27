package ar.edu.um.programacion2.turnos.disponibilidad.application.exception;

public class CatalogoRechazoException extends RuntimeException {
    public CatalogoRechazoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
