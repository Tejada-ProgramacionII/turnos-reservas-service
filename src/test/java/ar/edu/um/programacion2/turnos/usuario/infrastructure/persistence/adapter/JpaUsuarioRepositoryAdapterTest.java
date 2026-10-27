package ar.edu.um.programacion2.turnos.usuario.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.infrastructure.persistence.entity.UsuarioEntity;
import ar.edu.um.programacion2.turnos.usuario.infrastructure.persistence.mapper.UsuarioMapper;
import ar.edu.um.programacion2.turnos.usuario.infrastructure.persistence.repository.JpaUsuarioRepository;

@ExtendWith(MockitoExtension.class)
class JpaUsuarioRepositoryAdapterTest {

    private static final Instant CREADO = Instant.parse("2026-10-23T12:00:00Z");

    @Mock
    private JpaUsuarioRepository jpaUsuarioRepository;

    private JpaUsuarioRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaUsuarioRepositoryAdapter(jpaUsuarioRepository, new UsuarioMapper());
    }

    @Test
    void createGuardaElUsuarioYDevuelveElIdGenerado() {
        Usuario nuevo = Usuario.builder()
                .login("jtejada")
                .passwordHash("$2a$10$hash")
                .email("jtejada@ejemplo.com")
                .activated(true)
                .langKey("es")
                .externalPatientId(UUID.randomUUID())
                .authorities(Set.of("ROLE_USER"))
                .createdBy("system")
                .createdDate(CREADO)
                .build();
        when(jpaUsuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(invocacion -> {
            UsuarioEntity entity = invocacion.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        Usuario guardado = adapter.create(nuevo);

        assertThat(guardado.getId()).isEqualTo(1L);
        assertThat(guardado).usingRecursiveComparison().ignoringFields("id").isEqualTo(nuevo);
    }

    @Test
    void findByLoginDevuelveElUsuarioCuandoExiste() {
        UsuarioEntity entity = UsuarioEntity.builder()
                .id(1L)
                .login("jtejada")
                .authorities(Set.of("ROLE_USER"))
                .build();
        when(jpaUsuarioRepository.findByLogin("jtejada")).thenReturn(Optional.of(entity));

        Optional<Usuario> encontrado = adapter.findByLogin("jtejada");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getAuthorities()).containsExactly("ROLE_USER");
    }

    @Test
    void findByLoginDevuelveVacioCuandoNoExiste() {
        when(jpaUsuarioRepository.findByLogin("nadie")).thenReturn(Optional.empty());

        assertThat(adapter.findByLogin("nadie")).isEmpty();
    }

    @Test
    void existsByLoginDelegaEnElRepositorio() {
        when(jpaUsuarioRepository.existsByLogin("jtejada")).thenReturn(true);

        assertThat(adapter.existsByLogin("jtejada")).isTrue();
    }

    @Test
    void existsByEmailDelegaEnElRepositorio() {
        when(jpaUsuarioRepository.existsByEmail("libre@ejemplo.com")).thenReturn(false);

        assertThat(adapter.existsByEmail("libre@ejemplo.com")).isFalse();
    }
}
