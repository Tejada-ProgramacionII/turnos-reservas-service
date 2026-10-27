package ar.edu.um.programacion2.turnos.usuario.domain.ports.in;

import java.util.Optional;

public interface AuthenticateUsuarioUseCase {
    Optional<String> authenticateUsuario(String login, String password);
}
