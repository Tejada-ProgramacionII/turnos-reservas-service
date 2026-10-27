package ar.edu.um.programacion2.turnos.usuario.infrastructure.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.infrastructure.web.dto.UsuarioRequest;
import ar.edu.um.programacion2.turnos.usuario.infrastructure.web.dto.UsuarioResponse;

class UsuarioDtoMapperTest {

    private final UsuarioDtoMapper mapper = new UsuarioDtoMapper();

    @Test
    void toDomainCopiaLosDatosDelFormularioSinLaContrasena() {
        UsuarioRequest request = UsuarioRequest.builder()
                .login("jtejada")
                .password("secreta123")
                .firstName("Joaquin")
                .lastName("Tejada")
                .email("jtejada@ejemplo.com")
                .imageUrl("foto.png")
                .langKey("es")
                .build();

        Usuario usuario = mapper.toDomain(request);

        assertThat(usuario.getLogin()).isEqualTo("jtejada");
        assertThat(usuario.getFirstName()).isEqualTo("Joaquin");
        assertThat(usuario.getLastName()).isEqualTo("Tejada");
        assertThat(usuario.getEmail()).isEqualTo("jtejada@ejemplo.com");
        assertThat(usuario.getImageUrl()).isEqualTo("foto.png");
        assertThat(usuario.getLangKey()).isEqualTo("es");
        assertThat(usuario.getPasswordHash()).isNull();
    }

    @Test
    void toDomainDevuelveNullSiRecibeNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toResponseCopiaLosDatosPublicosDelUsuario() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .login("jtejada")
                .passwordHash("$2a$10$hash")
                .firstName("Joaquin")
                .lastName("Tejada")
                .email("jtejada@ejemplo.com")
                .imageUrl("foto.png")
                .langKey("es")
                .activated(true)
                .authorities(Set.of("ROLE_USER"))
                .build();

        UsuarioResponse response = mapper.toResponse(usuario);

        assertThat(response).usingRecursiveComparison()
                .isEqualTo(UsuarioResponse.builder()
                        .login("jtejada")
                        .firstName("Joaquin")
                        .lastName("Tejada")
                        .email("jtejada@ejemplo.com")
                        .imageUrl("foto.png")
                        .langKey("es")
                        .activated(true)
                        .authorities(Set.of("ROLE_USER"))
                        .build());
    }

    @Test
    void toResponseDevuelveNullSiRecibeNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
