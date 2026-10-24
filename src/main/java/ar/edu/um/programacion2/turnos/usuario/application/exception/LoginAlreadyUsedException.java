package ar.edu.um.programacion2.turnos.usuario.application.exception;

public class LoginAlreadyUsedException extends RuntimeException {
    public LoginAlreadyUsedException() {
        super("usuario ya registrado");
    }
}
