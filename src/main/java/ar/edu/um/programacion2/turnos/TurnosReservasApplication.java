package ar.edu.um.programacion2.turnos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TurnosReservasApplication {

    public static void main(String[] args) {
        SpringApplication.run(TurnosReservasApplication.class, args);
    }

}
