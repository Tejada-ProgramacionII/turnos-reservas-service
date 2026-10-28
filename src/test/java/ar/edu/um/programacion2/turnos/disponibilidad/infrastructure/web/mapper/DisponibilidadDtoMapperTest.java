package ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.Disponibilidad;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.TurnoLibre;
import ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.web.dto.DisponibilidadResponse;

class DisponibilidadDtoMapperTest {

    private final DisponibilidadDtoMapper mapper = new DisponibilidadDtoMapper();

    @Test
    void toResponseTraduceLaDisponibilidadYSusTurnos() {
        Disponibilidad disponibilidad = Disponibilidad.builder()
                .profesionalId(201L)
                .fecha(LocalDate.of(2026, 11, 9))
                .turnos(List.of(TurnoLibre.builder().horaInicio(LocalTime.of(12, 0)).horaFin(LocalTime.of(12, 30)).build()))
                .build();

        DisponibilidadResponse response = mapper.toResponse(disponibilidad);

        assertThat(response.getProfesionalId()).isEqualTo(201L);
        assertThat(response.getFecha()).isEqualTo(LocalDate.of(2026, 11, 9));
        assertThat(response.getTurnos()).singleElement().satisfies(turno -> {
            assertThat(turno.getHoraInicio()).isEqualTo(LocalTime.of(12, 0));
            assertThat(turno.getHoraFin()).isEqualTo(LocalTime.of(12, 30));
        });
    }

    @Test
    void toResponseDevuelveNullSiRecibeNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
