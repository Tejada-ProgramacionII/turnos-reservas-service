package ar.edu.um.programacion2.turnos.usuario.application.usecases;

import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.in.AuthenticateUsuarioUseCase;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.PasswordHasher;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.TokenGenerator;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthenticateUsuarioUseCaseImpl implements AuthenticateUsuarioUseCase {

    private final UsuarioRepository repository;
    private final PasswordHasher passwordHasher;
    private final TokenGenerator tokenGenerator;

    @Override
    public Optional<String> authenticateUsuario(String login, String password) {
        return repository.findByLogin(login.toLowerCase(Locale.ROOT))
                .filter(Usuario::isActivated)
                .filter(usuario -> passwordHasher.matches(password, usuario.getPasswordHash()))
                .map(tokenGenerator::generate);
    }
}
