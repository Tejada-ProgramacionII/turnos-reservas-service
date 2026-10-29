package ar.edu.um.programacion2.turnos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "catedra.kafka")
public class CatedraKafkaProperties {

    @NotBlank
    private String bootstrapServers;

    @NotBlank
    private String topicAcciones;

    @NotBlank
    private String grupo;
}
