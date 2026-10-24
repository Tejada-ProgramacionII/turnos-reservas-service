package ar.edu.um.programacion2.turnos.usuario.application.usecases;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.usuario.application.exception.EmailAlreadyUsedException;
import ar.edu.um.programacion2.turnos.usuario.application.exception.LoginAlreadyUsedException;
import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.in.CreateUsuarioUseCase;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.PasswordHasher;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CreateUsuarioUseCaseImpl implements CreateUsuarioUseCase {

    private static final String ROLE_USER = "ROLE_USER";

    private final UsuarioRepository repository;
    private final PasswordHasher passwordHasher;

    @Override
    public Usuario createUsuario(Usuario usuario, String password) {
        String login = usuario.getLogin().toLowerCase(Locale.ROOT);
        String email = usuario.getEmail().toLowerCase(Locale.ROOT);
        if (repository.existsByLogin(login)) {
            throw new LoginAlreadyUsedException();
        }
        if (repository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }
        Instant ahora = Instant.now();
        Usuario nuevo = Usuario.builder()
                .login(login)
                .passwordHash(passwordHasher.hash(password))
                .firstName(usuario.getFirstName())
                .lastName(usuario.getLastName())
                .email(email)
                .imageUrl(usuario.getImageUrl())
                .activated(true)
                .langKey(usuario.getLangKey())
                .externalPatientId(UUID.randomUUID())
                .authorities(Set.of(ROLE_USER))
                .createdBy(login)
                .createdDate(ahora)
                .lastModifiedBy(login)
                .lastModifiedDate(ahora)
                .build();
        return repository.create(nuevo);
    }
}
