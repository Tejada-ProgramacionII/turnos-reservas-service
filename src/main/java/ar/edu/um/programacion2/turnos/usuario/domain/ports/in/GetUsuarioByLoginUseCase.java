package ar.edu.um.programacion2.turnos.usuario.domain.ports.in;

import java.util.Optional;

import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;

public interface GetUsuarioByLoginUseCase {
    Optional<Usuario> getUsuarioByLogin(String login);
}
