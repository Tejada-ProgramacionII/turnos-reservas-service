package ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.mapper;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.AgendaProfesional;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.HorarioAtencion;
import ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.dto.HorarioCatalogoResponse;
import ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.dto.ProfesionalCatalogoResponse;

@Component
public class AgendaProfesionalClientMapper {

    public AgendaProfesional toDomainModel(ProfesionalCatalogoResponse response) {
        if (response == null) {
            return null;
        }
        return AgendaProfesional.builder()
                .profesionalId(response.getId())
                .categoriaId(response.getCategoriaId())
                .nombre(response.getNombre())
                .apellido(response.getApellido())
                .habilitado(response.isHabilitado())
                .horarios(response.getHorarios().stream().map(this::toHorario).toList())
                .versionCatalogo(response.getVersionCatalogo())
                .build();
    }

    private HorarioAtencion toHorario(HorarioCatalogoResponse response) {
        return HorarioAtencion.builder()
                .diaSemana(response.getDiaSemana())
                .horaInicio(response.getHoraInicio())
                .horaFin(response.getHoraFin())
                .duracionTurnoMinutos(response.getDuracionTurnoMinutos())
                .build();
    }
}
