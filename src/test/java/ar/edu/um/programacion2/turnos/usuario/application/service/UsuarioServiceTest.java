package ar.edu.um.programacion2.turnos.usuario.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.programacion2.turnos.usuario.application.exception.InvalidCredentialsException;
import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.in.AuthenticateUsuarioUseCase;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.in.CreateUsuarioUseCase;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private CreateUsuarioUseCase createUsuarioUseCase;

    @Mock
    private AuthenticateUsuarioUseCase authenticateUsuarioUseCase;

    @InjectMocks
    private UsuarioService service;

    @Test
    void createUsuarioDelegaEnElCasoDeUso() {
        Usuario datos = Usuario.builder().login("jtejada").build();
        Usuario creado = Usuario.builder().id(1L).login("jtejada").build();
        when(createUsuarioUseCase.createUsuario(datos, "secreta123")).thenReturn(creado);

        assertThat(service.createUsuario(datos, "secreta123")).isSameAs(creado);
    }

    @Test
    void authenticateUsuarioDevuelveElToken() {
        when(authenticateUsuarioUseCase.authenticateUsuario("jtejada", "secreta123"))
                .thenReturn(Optional.of("token"));

        assertThat(service.authenticateUsuario("jtejada", "secreta123")).isEqualTo("token");
    }

    @Test
    void authenticateUsuarioLanzaExcepcionSiLasCredencialesNoSirven() {
        when(authenticateUsuarioUseCase.authenticateUsuario("jtejada", "incorrecta"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticateUsuario("jtejada", "incorrecta"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("credenciales inválidas");
    }
}
