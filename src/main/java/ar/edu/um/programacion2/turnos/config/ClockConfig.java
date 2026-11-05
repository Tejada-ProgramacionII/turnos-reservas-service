package ar.edu.um.programacion2.turnos.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// "Hoy" y "ahora" se calculan en la hora local de los turnos, sin depender de la zona del servidor.
@Configuration
public class ClockConfig {

    private static final ZoneId ZONA_TURNOS = ZoneId.of("America/Argentina/Mendoza");

    @Bean
    public Clock clock() {
        return Clock.system(ZONA_TURNOS);
    }
}
