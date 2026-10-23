package ar.edu.um.programacion2.turnos.usuario.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.UsuarioRepository;
import ar.edu.um.programacion2.turnos.usuario.infrastructure.persistence.entity.UsuarioEntity;
import ar.edu.um.programacion2.turnos.usuario.infrastructure.persistence.mapper.UsuarioMapper;
import ar.edu.um.programacion2.turnos.usuario.infrastructure.persistence.repository.JpaUsuarioRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaUsuarioRepositoryAdapter implements UsuarioRepository {

    private final JpaUsuarioRepository jpaUsuarioRepository;
    private final UsuarioMapper usuarioMapper;

    @Override
    public Usuario create(Usuario usuario) {
        UsuarioEntity guardado = jpaUsuarioRepository.save(usuarioMapper.toEntity(usuario));
        return usuarioMapper.toDomainModel(guardado);
    }

    @Override
    public Optional<Usuario> findByLogin(String login) {
        return jpaUsuarioRepository.findByLogin(login).map(usuarioMapper::toDomainModel);
    }

    @Override
    public boolean existsByLogin(String login) {
        return jpaUsuarioRepository.existsByLogin(login);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaUsuarioRepository.existsByEmail(email);
    }
}
