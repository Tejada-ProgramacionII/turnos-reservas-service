package ar.edu.um.programacion2.turnos.usuario.infrastructure.security.adapter;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.TokenGenerator;

@Component
public class JwtTokenGeneratorAdapter implements TokenGenerator {

    private final JwtEncoder jwtEncoder;
    private final long tokenValiditySeconds;

    public JwtTokenGeneratorAdapter(JwtEncoder jwtEncoder,
            @Value("${jwt.token-validity-seconds}") long tokenValiditySeconds) {
        this.jwtEncoder = jwtEncoder;
        this.tokenValiditySeconds = tokenValiditySeconds;
    }

    @Override
    public String generate(Usuario usuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(usuario.getLogin())
                .claim("auth", String.join(" ", usuario.getAuthorities()))
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(tokenValiditySeconds))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS512).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
