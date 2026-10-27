package ar.edu.um.programacion2.turnos.usuario.infrastructure.web.mapper;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.infrastructure.web.dto.UsuarioRequest;
import ar.edu.um.programacion2.turnos.usuario.infrastructure.web.dto.UsuarioResponse;

@Component
public class UsuarioDtoMapper {

    public Usuario toDomain(UsuarioRequest request) {
        if (request == null) {
            return null;
        }
        return Usuario.builder()
                .login(request.getLogin())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .imageUrl(request.getImageUrl())
                .langKey(request.getLangKey())
                .build();
    }

    public UsuarioResponse toResponse(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return UsuarioResponse.builder()
                .login(usuario.getLogin())
                .firstName(usuario.getFirstName())
                .lastName(usuario.getLastName())
                .email(usuario.getEmail())
                .imageUrl(usuario.getImageUrl())
                .langKey(usuario.getLangKey())
                .activated(usuario.isActivated())
                .authorities(usuario.getAuthorities())
                .build();
    }
}
