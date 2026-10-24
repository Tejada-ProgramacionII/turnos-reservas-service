package ar.edu.um.programacion2.turnos.usuario.domain.ports.in;

import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;

public interface CreateUsuarioUseCase {
    Usuario createUsuario(Usuario usuario, String password);
}
