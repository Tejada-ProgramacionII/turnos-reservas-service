package ar.edu.um.programacion2.turnos.disponibilidad.application.exception;

public class ProfesionalDeshabilitadoException extends RuntimeException {
    public ProfesionalDeshabilitadoException(Long profesionalId) {
        super("El profesional " + profesionalId + " no admite reservas nuevas");
    }
}
