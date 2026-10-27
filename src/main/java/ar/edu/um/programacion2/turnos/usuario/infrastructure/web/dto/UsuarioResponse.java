package ar.edu.um.programacion2.turnos.usuario.infrastructure.web.dto;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {
    private String login;
    private String firstName;
    private String lastName;
    private String email;
    private String imageUrl;
    private String langKey;
    private boolean activated;
    private Set<String> authorities;
}
