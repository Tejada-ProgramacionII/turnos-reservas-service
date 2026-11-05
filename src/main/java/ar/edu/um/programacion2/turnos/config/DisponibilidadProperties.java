package ar.edu.um.programacion2.turnos.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "disponibilidad")
public class DisponibilidadProperties {

    @Min(1)
    private int maxIntentos = 3;

    private Duration esperaEntreIntentos = Duration.ofMillis(500);
}
