package ar.edu.um.programacion2.turnos.usuario.application.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("credenciales inválidas");
    }
}
