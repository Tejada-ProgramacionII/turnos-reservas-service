package ar.edu.um.programacion2.turnos.usuario.application.service;

import org.springframework.stereotype.Service;

import ar.edu.um.programacion2.turnos.usuario.application.exception.InvalidCredentialsException;
import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.in.AuthenticateUsuarioUseCase;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.in.CreateUsuarioUseCase;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final CreateUsuarioUseCase createUsuarioUseCase;
    private final AuthenticateUsuarioUseCase authenticateUsuarioUseCase;

    public Usuario createUsuario(Usuario usuario, String password) {
        return createUsuarioUseCase.createUsuario(usuario, password);
    }

    public String authenticateUsuario(String login, String password) {
        return authenticateUsuarioUseCase.authenticateUsuario(login, password)
                .orElseThrow(InvalidCredentialsException::new);
    }
}
