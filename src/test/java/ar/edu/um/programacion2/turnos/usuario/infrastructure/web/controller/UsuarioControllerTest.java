package ar.edu.um.programacion2.turnos.usuario.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import ar.edu.um.programacion2.turnos.usuario.application.exception.EmailAlreadyUsedException;
import ar.edu.um.programacion2.turnos.usuario.application.exception.InvalidCredentialsException;
import ar.edu.um.programacion2.turnos.usuario.application.exception.LoginAlreadyUsedException;
import ar.edu.um.programacion2.turnos.usuario.application.exception.UsuarioNotFoundException;
import ar.edu.um.programacion2.turnos.usuario.application.service.UsuarioService;
import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.infrastructure.web.dto.LoginRequest;
import ar.edu.um.programacion2.turnos.usuario.infrastructure.web.dto.TokenResponse;
import ar.edu.um.programacion2.turnos.usuario.infrastructure.web.dto.UsuarioRequest;
import ar.edu.um.programacion2.turnos.usuario.infrastructure.web.dto.UsuarioResponse;
import ar.edu.um.programacion2.turnos.usuario.infrastructure.web.mapper.UsuarioDtoMapper;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;

    private UsuarioController controller;

    @BeforeEach
    void setUp() {
        controller = new UsuarioController(usuarioService, new UsuarioDtoMapper());
    }

    private static UsuarioRequest request() {
        return UsuarioRequest.builder()
                .login("jtejada")
                .password("secreta123")
                .email("jtejada@ejemplo.com")
                .langKey("es")
                .build();
    }

    @Test
    void registroCorrectoResponde201SinCuerpo() {
        ResponseEntity<ProblemDetail> respuesta = controller.registerUsuario(request());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isNull();
        verify(usuarioService).createUsuario(any(Usuario.class), eq("secreta123"));
    }

    @Test
    void loginRepetidoResponde400ConElMotivo() {
        when(usuarioService.createUsuario(any(Usuario.class), eq("secreta123")))
                .thenThrow(new LoginAlreadyUsedException());

        ResponseEntity<ProblemDetail> respuesta = controller.registerUsuario(request());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).extracting(ProblemDetail::getDetail).isEqualTo("usuario ya registrado");
    }

    @Test
    void emailRepetidoResponde400ConElMotivo() {
        when(usuarioService.createUsuario(any(Usuario.class), eq("secreta123")))
                .thenThrow(new EmailAlreadyUsedException());

        ResponseEntity<ProblemDetail> respuesta = controller.registerUsuario(request());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).extracting(ProblemDetail::getDetail).isEqualTo("email ya registrado");
    }

    @Test
    void loginCorrectoResponde200ConElToken() {
        when(usuarioService.authenticateUsuario("jtejada", "secreta123")).thenReturn("token");

        ResponseEntity<Object> respuesta = controller.authenticateUsuario(new LoginRequest("jtejada", "secreta123"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isInstanceOf(TokenResponse.class)
                .extracting("idToken").isEqualTo("token");
        assertThat(respuesta.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token");
    }

    @Test
    void credencialesInvalidasResponden401ConElMotivo() {
        when(usuarioService.authenticateUsuario("jtejada", "incorrecta"))
                .thenThrow(new InvalidCredentialsException());

        ResponseEntity<Object> respuesta = controller.authenticateUsuario(new LoginRequest("jtejada", "incorrecta"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta.getBody()).isInstanceOf(ProblemDetail.class)
                .extracting("detail").isEqualTo("credenciales inválidas");
    }

    private static Jwt jwtDe(String login) {
        return Jwt.withTokenValue("token").header("alg", "HS512").subject(login).build();
    }

    @Test
    void getAccountDevuelveLosDatosDelUsuarioDelToken() {
        when(usuarioService.getUsuarioByLogin("jtejada"))
                .thenReturn(Usuario.builder().login("jtejada").email("jtejada@ejemplo.com").build());

        ResponseEntity<UsuarioResponse> respuesta = controller.getAccount(jwtDe("jtejada"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).extracting(UsuarioResponse::getLogin).isEqualTo("jtejada");
    }

    @Test
    void getAccountResponde404SiElUsuarioDelTokenYaNoExiste() {
        when(usuarioService.getUsuarioByLogin("fantasma")).thenThrow(new UsuarioNotFoundException("fantasma"));

        Jwt jwt = jwtDe("fantasma");
        assertThatThrownBy(() -> controller.getAccount(jwt))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
