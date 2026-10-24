package ar.edu.um.programacion2.turnos.usuario.infrastructure.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class UsuarioRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void crearValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        factory.close();
    }

    private static UsuarioRequest.UsuarioRequestBuilder valido() {
        return UsuarioRequest.builder()
                .login("jtejada")
                .password("secreta123")
                .email("jtejada@ejemplo.com")
                .langKey("es");
    }

    private static List<String> camposInvalidos(UsuarioRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .toList();
    }

    @Test
    void unFormularioCompletoEsValido() {
        assertThat(camposInvalidos(valido().build())).isEmpty();
    }

    @Test
    void rechazaUnLoginConCaracteresNoPermitidos() {
        assertThat(camposInvalidos(valido().login("j tejada!").build())).containsExactly("login");
    }

    @Test
    void rechazaUnaContrasenaDeMenosDeCuatroCaracteres() {
        assertThat(camposInvalidos(valido().password("abc").build())).containsExactly("password");
    }

    @Test
    void rechazaUnEmailConFormatoInvalido() {
        assertThat(camposInvalidos(valido().email("no-es-un-email").build())).containsExactly("email");
    }

    @Test
    void rechazaUnFormularioSinLangKey() {
        assertThat(camposInvalidos(valido().langKey(null).build())).containsExactly("langKey");
    }
}
