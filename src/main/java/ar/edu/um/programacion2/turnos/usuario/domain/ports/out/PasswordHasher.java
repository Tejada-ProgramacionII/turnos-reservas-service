package ar.edu.um.programacion2.turnos.usuario.domain.ports.out;

public interface PasswordHasher {
    String hash(String password);

    boolean matches(String password, String passwordHash);
}
