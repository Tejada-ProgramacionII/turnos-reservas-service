package ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.EstadoOcupacion;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.Ocupacion;
import ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.dto.OcupacionCatedraResponse;

class OcupacionClientMapperTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 9, 14);
    private static final Instant VENCE_EN = Instant.parse("2026-09-10T14:44:50Z");

    private final OcupacionClientMapper mapper = new OcupacionClientMapper();

    @Test
    void toDomainModelTraduceUnaOcupacionConfirmada() {
        OcupacionCatedraResponse response = OcupacionCatedraResponse.builder()
                .date(FECHA)
                .startTime(LocalTime.of(9, 0))
                .status("CONFIRMED")
                .build();

        Ocupacion ocupacion = mapper.toDomainModel(response);

        assertThat(ocupacion.getFecha()).isEqualTo(FECHA);
        assertThat(ocupacion.getHoraInicio()).isEqualTo(LocalTime.of(9, 0));
        assertThat(ocupacion.getEstado()).isEqualTo(EstadoOcupacion.CONFIRMADA);
        assertThat(ocupacion.getVenceEn()).isNull();
    }

    @Test
    void toDomainModelTraduceUnaOcupacionRetenidaConSuVencimiento() {
        OcupacionCatedraResponse response = OcupacionCatedraResponse.builder()
                .date(FECHA)
                .startTime(LocalTime.of(9, 30))
                .status("HELD")
                .expiresAt(VENCE_EN)
                .build();

        Ocupacion ocupacion = mapper.toDomainModel(response);

        assertThat(ocupacion.getEstado()).isEqualTo(EstadoOcupacion.RETENIDA);
        assertThat(ocupacion.getVenceEn()).isEqualTo(VENCE_EN);
    }

    @Test
    void toDomainModelLanzaExcepcionSiElEstadoEsDesconocido() {
        OcupacionCatedraResponse response = OcupacionCatedraResponse.builder().status("CANCELLED").build();

        assertThatThrownBy(() -> mapper.toDomainModel(response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void toDomainModelDevuelveNullSiRecibeNull() {
        assertThat(mapper.toDomainModel(null)).isNull();
    }
}
