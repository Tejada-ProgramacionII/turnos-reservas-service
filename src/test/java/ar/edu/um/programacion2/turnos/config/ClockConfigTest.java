package ar.edu.um.programacion2.turnos.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class ClockConfigTest {

    @Test
    void elRelojUsaLaZonaHorariaDeMendoza() {
        assertThat(new ClockConfig().clock().getZone()).isEqualTo(ZoneId.of("America/Argentina/Mendoza"));
    }
}
