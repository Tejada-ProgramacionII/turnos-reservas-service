package ar.edu.um.programacion2.turnos.usuario.application.service;

import org.springframework.stereotype.Service;

import ar.edu.um.programacion2.turnos.usuario.application.exception.InvalidCredentialsException;
import ar.edu.um.programacion2.turnos.usuario.application.exception.UsuarioNotFoundException;
import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.in.AuthenticateUsuarioUseCase;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.in.CreateUsuarioUseCase;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.in.GetUsuarioByLoginUseCase;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final CreateUsuarioUseCase createUsuarioUseCase;
    private final AuthenticateUsuarioUseCase authenticateUsuarioUseCase;
    private final GetUsuarioByLoginUseCase getUsuarioByLoginUseCase;

    public Usuario createUsuario(Usuario usuario, String password) {
        return createUsuarioUseCase.createUsuario(usuario, password);
    }

    public String authenticateUsuario(String login, String password) {
        return authenticateUsuarioUseCase.authenticateUsuario(login, password)
                .orElseThrow(InvalidCredentialsException::new);
    }

    public Usuario getUsuarioByLogin(String login) {
        return getUsuarioByLoginUseCase.getUsuarioByLogin(login)
                .orElseThrow(() -> new UsuarioNotFoundException(login));
    }
}
