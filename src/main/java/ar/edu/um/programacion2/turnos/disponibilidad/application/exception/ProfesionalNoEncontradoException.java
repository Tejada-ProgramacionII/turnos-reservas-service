package ar.edu.um.programacion2.turnos.disponibilidad.application.exception;

public class ProfesionalNoEncontradoException extends RuntimeException {
    public ProfesionalNoEncontradoException(Long profesionalId) {
        super("No existe el profesional " + profesionalId);
    }
}
