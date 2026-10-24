package ar.edu.um.programacion2.turnos.usuario.application.service;

import org.springframework.stereotype.Service;

import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.in.CreateUsuarioUseCase;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final CreateUsuarioUseCase createUsuarioUseCase;

    public Usuario createUsuario(Usuario usuario, String password) {
        return createUsuarioUseCase.createUsuario(usuario, password);
    }
}
