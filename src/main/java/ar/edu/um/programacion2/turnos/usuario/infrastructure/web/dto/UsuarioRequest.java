package ar.edu.um.programacion2.turnos.usuario.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class UsuarioRequest {

    // Misma expresión que usa JHipster para el login (Constants.LOGIN_REGEX)
    public static final String LOGIN_REGEX =
            "^(?>[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*)|(?>[_.@A-Za-z0-9-]+)$";

    @NotBlank
    @Size(min = 1, max = 50)
    @Pattern(regexp = LOGIN_REGEX)
    private String login;

    @NotNull
    @Size(min = 4, max = 100)
    private String password;

    @Size(max = 50)
    private String firstName;

    @Size(max = 50)
    private String lastName;

    @NotNull
    @Email
    @Size(min = 5, max = 254)
    private String email;

    @Size(max = 256)
    private String imageUrl;

    @NotNull
    @Size(min = 2, max = 10)
    private String langKey;
}
