package ar.edu.um.programacion2.turnos.usuario.infrastructure.security.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class BCryptPasswordHasherAdapterTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final BCryptPasswordHasherAdapter adapter = new BCryptPasswordHasherAdapter(passwordEncoder);

    @Test
    void hashNoDevuelveLaContrasenaEnTextoPlano() {
        String hash = adapter.hash("secreta123");

        assertThat(hash).isNotEqualTo("secreta123").hasSize(60).startsWith("$2a$");
    }

    @Test
    void elHashCorrespondeALaContrasenaOriginal() {
        String hash = adapter.hash("secreta123");

        assertThat(passwordEncoder.matches("secreta123", hash)).isTrue();
        assertThat(passwordEncoder.matches("otra", hash)).isFalse();
    }

    @Test
    void matchesAceptaLaContrasenaCorrectaYRechazaOtra() {
        String hash = adapter.hash("secreta123");

        assertThat(adapter.matches("secreta123", hash)).isTrue();
        assertThat(adapter.matches("otra", hash)).isFalse();
    }

    @Test
    void dosHashesDeLaMismaContrasenaSonDistintos() {
        assertThat(adapter.hash("secreta123")).isNotEqualTo(adapter.hash("secreta123"));
    }
}
