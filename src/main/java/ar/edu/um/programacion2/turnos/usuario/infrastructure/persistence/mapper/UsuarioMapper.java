package ar.edu.um.programacion2.turnos.usuario.infrastructure.persistence.mapper;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.infrastructure.persistence.entity.UsuarioEntity;

@Component
public class UsuarioMapper {

    public UsuarioEntity toEntity(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return UsuarioEntity.builder()
                .id(usuario.getId())
                .login(usuario.getLogin())
                .passwordHash(usuario.getPasswordHash())
                .firstName(usuario.getFirstName())
                .lastName(usuario.getLastName())
                .email(usuario.getEmail())
                .imageUrl(usuario.getImageUrl())
                .activated(usuario.isActivated())
                .langKey(usuario.getLangKey())
                .externalPatientId(usuario.getExternalPatientId())
                .authorities(copiar(usuario.getAuthorities()))
                .createdBy(usuario.getCreatedBy())
                .createdDate(usuario.getCreatedDate())
                .lastModifiedBy(usuario.getLastModifiedBy())
                .lastModifiedDate(usuario.getLastModifiedDate())
                .build();
    }

    public Usuario toDomainModel(UsuarioEntity entity) {
        if (entity == null) {
            return null;
        }
        return Usuario.builder()
                .id(entity.getId())
                .login(entity.getLogin())
                .passwordHash(entity.getPasswordHash())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .imageUrl(entity.getImageUrl())
                .activated(entity.isActivated())
                .langKey(entity.getLangKey())
                .externalPatientId(entity.getExternalPatientId())
                .authorities(copiar(entity.getAuthorities()))
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    private Set<String> copiar(Set<String> authorities) {
        if (authorities == null) {
            return new HashSet<>();
        }
        return new HashSet<>(authorities);
    }
}
