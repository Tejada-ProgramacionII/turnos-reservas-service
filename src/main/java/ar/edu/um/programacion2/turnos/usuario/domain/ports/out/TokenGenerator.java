package ar.edu.um.programacion2.turnos.usuario.domain.ports.out;

import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;

public interface TokenGenerator {
    String generate(Usuario usuario);
}
