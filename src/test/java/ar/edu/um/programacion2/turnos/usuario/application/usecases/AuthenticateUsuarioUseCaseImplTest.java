package ar.edu.um.programacion2.turnos.usuario.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.PasswordHasher;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.TokenGenerator;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AuthenticateUsuarioUseCaseImplTest {

    @Mock
    private UsuarioRepository repository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenGenerator tokenGenerator;

    @InjectMocks
    private AuthenticateUsuarioUseCaseImpl useCase;

    private static Usuario usuario(boolean activo) {
        return Usuario.builder().login("jtejada").passwordHash("$2a$10$hash").activated(activo).build();
    }

    @Test
    void devuelveElTokenSiLasCredencialesSonCorrectas() {
        Usuario usuario = usuario(true);
        when(repository.findByLogin("jtejada")).thenReturn(Optional.of(usuario));
        when(passwordHasher.matches("secreta123", "$2a$10$hash")).thenReturn(true);
        when(tokenGenerator.generate(usuario)).thenReturn("token");

        assertThat(useCase.authenticateUsuario("JTejada", "secreta123")).contains("token");
    }

    @Test
    void devuelveVacioSiElLoginNoExiste() {
        when(repository.findByLogin("nadie")).thenReturn(Optional.empty());

        assertThat(useCase.authenticateUsuario("nadie", "secreta123")).isEmpty();
        verify(tokenGenerator, never()).generate(any(Usuario.class));
    }

    @Test
    void devuelveVacioSiLaContrasenaNoCoincide() {
        when(repository.findByLogin("jtejada")).thenReturn(Optional.of(usuario(true)));
        when(passwordHasher.matches("incorrecta", "$2a$10$hash")).thenReturn(false);

        assertThat(useCase.authenticateUsuario("jtejada", "incorrecta")).isEmpty();
        verify(tokenGenerator, never()).generate(any(Usuario.class));
    }

    @Test
    void devuelveVacioSiElUsuarioNoEstaActivo() {
        when(repository.findByLogin("jtejada")).thenReturn(Optional.of(usuario(false)));

        assertThat(useCase.authenticateUsuario("jtejada", "secreta123")).isEmpty();
        verify(tokenGenerator, never()).generate(any(Usuario.class));
    }
}
