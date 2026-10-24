package ar.edu.um.programacion2.turnos.usuario.infrastructure.web.mapper;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.infrastructure.web.dto.UsuarioRequest;

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
}
