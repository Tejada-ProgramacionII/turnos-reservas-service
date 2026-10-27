package ar.edu.um.programacion2.turnos.usuario.application.exception;

public class EmailAlreadyUsedException extends RuntimeException {
    public EmailAlreadyUsedException() {
        super("email ya registrado");
    }
}
