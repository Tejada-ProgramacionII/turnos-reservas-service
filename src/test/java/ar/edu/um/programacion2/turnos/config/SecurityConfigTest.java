package ar.edu.um.programacion2.turnos.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import jakarta.servlet.Filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.TokenGenerator;

@SpringBootTest
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    @Autowired
    private TokenGenerator tokenGenerator;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
    }

    @Test
    void unEndpointProtegidoSinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/account")).andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenInvalidoResponde401() throws Exception {
        mockMvc.perform(get("/api/account").header(HttpHeaders.AUTHORIZATION, "Bearer no-es-un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenValidoPasaLaSeguridad() throws Exception {
        String token = tokenGenerator.generate(
                Usuario.builder().login("fantasma").authorities(Set.of("ROLE_USER")).build());

        // El token es válido: la seguridad lo deja pasar y el controller responde 404
        // porque ese usuario no está en la base.
        mockMvc.perform(get("/api/account").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void elRegistroEsPublico() throws Exception {
        // Sin token y con un cuerpo vacío: responde 400 por validación, no 401.
        mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
