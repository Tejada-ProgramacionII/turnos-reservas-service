package ar.edu.um.programacion2.turnos.usuario.domain.ports.out;

import java.util.Optional;

import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;

public interface UsuarioRepository {
    Usuario create(Usuario usuario);

    Optional<Usuario> findByLogin(String login);

    boolean existsByLogin(String login);

    boolean existsByEmail(String email);
}
