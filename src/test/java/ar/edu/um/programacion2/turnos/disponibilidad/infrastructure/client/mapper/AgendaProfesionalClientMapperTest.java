package ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.AgendaProfesional;
import ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.dto.HorarioCatalogoResponse;
import ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.dto.ProfesionalCatalogoResponse;

class AgendaProfesionalClientMapperTest {

    private final AgendaProfesionalClientMapper mapper = new AgendaProfesionalClientMapper();

    @Test
    void toDomainModelTraduceElProfesionalYSusHorarios() {
        ProfesionalCatalogoResponse response = ProfesionalCatalogoResponse.builder()
                .id(201L)
                .categoriaId(101L)
                .nombre("Ana")
                .apellido("Torres")
                .habilitado(true)
                .horarios(List.of(HorarioCatalogoResponse.builder()
                        .diaSemana(DayOfWeek.MONDAY)
                        .horaInicio(LocalTime.of(12, 0))
                        .horaFin(LocalTime.of(15, 0))
                        .duracionTurnoMinutos(30)
                        .build()))
                .versionCatalogo(1L)
                .build();

        AgendaProfesional agenda = mapper.toDomainModel(response);

        assertThat(agenda.getProfesionalId()).isEqualTo(201L);
        assertThat(agenda.getCategoriaId()).isEqualTo(101L);
        assertThat(agenda.getNombre()).isEqualTo("Ana");
        assertThat(agenda.getApellido()).isEqualTo("Torres");
        assertThat(agenda.isHabilitado()).isTrue();
        assertThat(agenda.getVersionCatalogo()).isEqualTo(1L);
        assertThat(agenda.getHorarios()).singleElement().satisfies(horario -> {
            assertThat(horario.getDiaSemana()).isEqualTo(DayOfWeek.MONDAY);
            assertThat(horario.getHoraInicio()).isEqualTo(LocalTime.of(12, 0));
            assertThat(horario.getHoraFin()).isEqualTo(LocalTime.of(15, 0));
            assertThat(horario.getDuracionTurnoMinutos()).isEqualTo(30);
        });
    }

    @Test
    void toDomainModelConHorariosVaciosDevuelveAgendaSinHorarios() {
        ProfesionalCatalogoResponse response = ProfesionalCatalogoResponse.builder()
                .id(201L)
                .habilitado(false)
                .horarios(List.of())
                .build();

        AgendaProfesional agenda = mapper.toDomainModel(response);

        assertThat(agenda.isHabilitado()).isFalse();
        assertThat(agenda.getHorarios()).isEmpty();
    }

    @Test
    void toDomainModelDevuelveNullSiRecibeNull() {
        assertThat(mapper.toDomainModel(null)).isNull();
    }
}
