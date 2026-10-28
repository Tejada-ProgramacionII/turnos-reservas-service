package ar.edu.um.programacion2.turnos.usuario.infrastructure.security.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import ar.edu.um.programacion2.turnos.config.SecurityConfig;

class Argon2PasswordHasherAdapterTest {

    // El mismo encoder que usa la aplicación, no uno armado en el test.
    private final PasswordEncoder passwordEncoder = new SecurityConfig().passwordEncoder();
    private final Argon2PasswordHasherAdapter adapter = new Argon2PasswordHasherAdapter(passwordEncoder);

    @Test
    void hashGuardaUnHashArgon2idYNoLaContrasena() {
        String hash = adapter.hash("secreta123");

        assertThat(hash)
                .doesNotContain("secreta123")
                .startsWith("$argon2id$v=19$m=16384,t=2,p=1$")
                .hasSizeLessThanOrEqualTo(255);
    }

    @Test
    void matchesAceptaLaContrasenaCorrectaYRechazaOtra() {
        String hash = adapter.hash("secreta123");

        assertThat(adapter.matches("secreta123", hash)).isTrue();
        assertThat(adapter.matches("otra", hash)).isFalse();
    }

    @Test
    void dosHashesDeLaMismaContrasenaSonDistintos() {
        // Cada hash lleva su propia sal: dos usuarios con la misma clave no comparten hash.
        assertThat(adapter.hash("secreta123")).isNotEqualTo(adapter.hash("secreta123"));
    }

    @Test
    void matchesRechazaUnHashBCryptViejo() {
        String hashBCrypt = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

        assertThat(adapter.matches("secreta123", hashBCrypt)).isFalse();
    }
}
