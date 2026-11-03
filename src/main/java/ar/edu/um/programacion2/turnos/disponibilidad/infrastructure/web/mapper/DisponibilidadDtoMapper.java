package ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.web.mapper;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.Disponibilidad;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.TurnoLibre;
import ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.web.dto.DisponibilidadResponse;
import ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.web.dto.TurnoLibreResponse;

@Component
public class DisponibilidadDtoMapper {

    public DisponibilidadResponse toResponse(Disponibilidad disponibilidad) {
        if (disponibilidad == null) {
            return null;
        }
        return DisponibilidadResponse.builder()
                .profesionalId(disponibilidad.getProfesionalId())
                .fecha(disponibilidad.getFecha())
                .turnos(disponibilidad.getTurnos().stream().map(this::toTurnoResponse).toList())
                .build();
    }

    private TurnoLibreResponse toTurnoResponse(TurnoLibre turno) {
        return TurnoLibreResponse.builder()
                .horaInicio(turno.getHoraInicio())
                .horaFin(turno.getHoraFin())
                .build();
    }
}
