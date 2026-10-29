package ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EventoProcesado;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.EventoProcesadoRepository;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.mapper.EventoProcesadoMapper;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.repository.JpaEventoProcesadoRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaEventoProcesadoRepositoryAdapter implements EventoProcesadoRepository {

    private final JpaEventoProcesadoRepository jpaEventoProcesadoRepository;
    private final EventoProcesadoMapper eventoProcesadoMapper;

    @Override
    public boolean existsByEventId(String eventId) {
        return jpaEventoProcesadoRepository.existsById(eventId);
    }

    @Override
    public EventoProcesado save(EventoProcesado evento) {
        return eventoProcesadoMapper.toDomainModel(
                jpaEventoProcesadoRepository.save(eventoProcesadoMapper.toEntity(evento)));
    }
}
