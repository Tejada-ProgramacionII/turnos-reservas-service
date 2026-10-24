package ar.edu.um.programacion2.turnos.usuario.infrastructure.security.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;

class JwtTokenGeneratorAdapterTest {

    private static final long VIGENCIA = 3600;

    private final SecretKey clave = new SecretKeySpec(
            "clave-de-prueba-solo-para-este-test-de-generacion-de-tokens-jwt-hs512".getBytes(StandardCharsets.UTF_8),
            MacAlgorithm.HS512.getName());
    private final JwtTokenGeneratorAdapter adapter =
            new JwtTokenGeneratorAdapter(new NimbusJwtEncoder(new ImmutableSecret<>(clave)), VIGENCIA);
    private final JwtDecoder decoder =
            NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS512).build();

    @Test
    void elTokenLlevaElLoginLosRolesYUnaVigenciaLimitada() {
        Usuario usuario = Usuario.builder().login("jtejada").authorities(Set.of("ROLE_USER")).build();

        Jwt jwt = decoder.decode(adapter.generate(usuario));

        assertThat(jwt.getSubject()).isEqualTo("jtejada");
        assertThat(jwt.getClaimAsString("auth")).isEqualTo("ROLE_USER");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).hasSeconds(VIGENCIA);
    }
}
