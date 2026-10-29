package ar.edu.um.programacion2.turnos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    private static final long ESPERA_ENTRE_REINTENTOS_MS = 5000;

    // Si guardar el efecto falla, el offset no se confirma y el mismo mensaje se vuelve a procesar
    // (D-06). Por defecto Spring lo saltea después de 10 intentos; acá se reintenta sin límite.
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(ESPERA_ENTRE_REINTENTOS_MS, FixedBackOff.UNLIMITED_ATTEMPTS));
    }
}
