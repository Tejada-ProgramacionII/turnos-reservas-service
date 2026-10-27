package ar.edu.um.programacion2.turnos.usuario.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.um.programacion2.turnos.usuario.infrastructure.persistence.entity.UsuarioEntity;

@Repository
public interface JpaUsuarioRepository extends JpaRepository<UsuarioEntity, Long> {
    Optional<UsuarioEntity> findByLogin(String login);

    boolean existsByLogin(String login);

    boolean existsByEmail(String email);
}
