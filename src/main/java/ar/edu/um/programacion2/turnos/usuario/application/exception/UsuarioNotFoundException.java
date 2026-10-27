package ar.edu.um.programacion2.turnos.usuario.application.exception;

public class UsuarioNotFoundException extends RuntimeException {
    public UsuarioNotFoundException(String login) {
        super("No se encontró el usuario con login: " + login);
    }
}
