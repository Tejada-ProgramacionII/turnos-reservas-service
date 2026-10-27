package ar.edu.um.programacion2.turnos.usuario.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.programacion2.turnos.usuario.application.exception.EmailAlreadyUsedException;
import ar.edu.um.programacion2.turnos.usuario.application.exception.LoginAlreadyUsedException;
import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.PasswordHasher;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class CreateUsuarioUseCaseImplTest {

    @Mock
    private UsuarioRepository repository;

    @Mock
    private PasswordHasher passwordHasher;

    @InjectMocks
    private CreateUsuarioUseCaseImpl useCase;

    private static Usuario datosDelFormulario() {
        return Usuario.builder()
                .login("JTejada")
                .firstName("Joaquin")
                .lastName("Tejada")
                .email("JTejada@Ejemplo.com")
                .langKey("es")
                .build();
    }

    @Test
    void creaElUsuarioActivoConRoleUserYLaContrasenaHasheada() {
        when(repository.existsByLogin("jtejada")).thenReturn(false);
        when(repository.existsByEmail("jtejada@ejemplo.com")).thenReturn(false);
        when(passwordHasher.hash("secreta123")).thenReturn("$2a$10$hash");
        when(repository.create(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        useCase.createUsuario(datosDelFormulario(), "secreta123");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).create(captor.capture());
        Usuario guardado = captor.getValue();
        assertThat(guardado.getLogin()).isEqualTo("jtejada");
        assertThat(guardado.getEmail()).isEqualTo("jtejada@ejemplo.com");
        assertThat(guardado.getPasswordHash()).isEqualTo("$2a$10$hash");
        assertThat(guardado.isActivated()).isTrue();
        assertThat(guardado.getAuthorities()).containsExactly("ROLE_USER");
        assertThat(guardado.getExternalPatientId()).isNotNull();
        assertThat(guardado.getCreatedBy()).isEqualTo("jtejada");
        assertThat(guardado.getCreatedDate()).isNotNull();
        assertThat(guardado.getFirstName()).isEqualTo("Joaquin");
        assertThat(guardado.getLangKey()).isEqualTo("es");
    }

    @Test
    void rechazaUnLoginYaRegistradoSinGuardarNada() {
        when(repository.existsByLogin("jtejada")).thenReturn(true);

        assertThatThrownBy(() -> useCase.createUsuario(datosDelFormulario(), "secreta123"))
                .isInstanceOf(LoginAlreadyUsedException.class)
                .hasMessage("usuario ya registrado");
        verify(repository, never()).create(any(Usuario.class));
    }

    @Test
    void rechazaUnEmailYaRegistradoSinGuardarNada() {
        when(repository.existsByLogin("jtejada")).thenReturn(false);
        when(repository.existsByEmail("jtejada@ejemplo.com")).thenReturn(true);

        assertThatThrownBy(() -> useCase.createUsuario(datosDelFormulario(), "secreta123"))
                .isInstanceOf(EmailAlreadyUsedException.class)
                .hasMessage("email ya registrado");
        verify(repository, never()).create(any(Usuario.class));
    }
}
