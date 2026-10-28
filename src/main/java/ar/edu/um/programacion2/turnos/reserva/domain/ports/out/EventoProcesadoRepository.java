package ar.edu.um.programacion2.turnos.reserva.domain.ports.out;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EventoProcesado;

public interface EventoProcesadoRepository {
    boolean existsByEventId(String eventId);

    EventoProcesado save(EventoProcesado evento);
}
