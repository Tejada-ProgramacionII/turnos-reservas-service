package ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EventoProcesado;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.entity.EventoProcesadoEntity;

@Component
public class EventoProcesadoMapper {

    public EventoProcesadoEntity toEntity(EventoProcesado evento) {
        if (evento == null) {
            return null;
        }
        return EventoProcesadoEntity.builder()
                .eventId(evento.getEventId())
                .procesadoEn(evento.getProcesadoEn())
                .build();
    }

    public EventoProcesado toDomainModel(EventoProcesadoEntity entity) {
        if (entity == null) {
            return null;
        }
        return EventoProcesado.builder()
                .eventId(entity.getEventId())
                .procesadoEn(entity.getProcesadoEn())
                .build();
    }
}
