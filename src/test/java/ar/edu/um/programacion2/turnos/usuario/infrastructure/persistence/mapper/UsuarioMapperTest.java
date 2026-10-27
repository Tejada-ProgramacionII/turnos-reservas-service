package ar.edu.um.programacion2.turnos.usuario.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.infrastructure.persistence.entity.UsuarioEntity;

class UsuarioMapperTest {

    private static final Instant CREADO = Instant.parse("2026-10-23T12:00:00Z");
    private static final UUID EXTERNAL_PATIENT_ID = UUID.fromString("3f2b8c1e-5d4a-4e7b-9a10-2c6d8e4f1b23");

    private final UsuarioMapper mapper = new UsuarioMapper();

    @Test
    void toEntityCopiaTodosLosCampos() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .login("jtejada")
                .passwordHash("$2a$10$hash")
                .firstName("Joaquin")
                .lastName("Tejada")
                .email("jtejada@ejemplo.com")
                .imageUrl("foto.png")
                .activated(true)
                .langKey("es")
                .externalPatientId(EXTERNAL_PATIENT_ID)
                .authorities(Set.of("ROLE_USER"))
                .createdBy("system")
                .createdDate(CREADO)
                .lastModifiedBy("system")
                .lastModifiedDate(CREADO)
                .build();

        UsuarioEntity entity = mapper.toEntity(usuario);

        assertThat(entity).usingRecursiveComparison().isEqualTo(usuario);
    }

    @Test
    void toDomainModelCopiaTodosLosCampos() {
        UsuarioEntity entity = UsuarioEntity.builder()
                .id(2L)
                .login("ana")
                .passwordHash("$2a$10$otro")
                .email("ana@ejemplo.com")
                .activated(true)
                .langKey("es")
                .externalPatientId(EXTERNAL_PATIENT_ID)
                .authorities(Set.of("ROLE_USER"))
                .createdBy("system")
                .createdDate(CREADO)
                .build();

        Usuario usuario = mapper.toDomainModel(entity);

        assertThat(usuario).usingRecursiveComparison().isEqualTo(entity);
    }

    @Test
    void sinAutoridadesDevuelveUnConjuntoVacio() {
        Usuario usuario = Usuario.builder().login("ana").build();

        assertThat(mapper.toEntity(usuario).getAuthorities()).isEmpty();
        assertThat(mapper.toDomainModel(UsuarioEntity.builder().build()).getAuthorities()).isEmpty();
    }

    @Test
    void toEntityDevuelveNullSiRecibeNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toDomainModelDevuelveNullSiRecibeNull() {
        assertThat(mapper.toDomainModel(null)).isNull();
    }
}
