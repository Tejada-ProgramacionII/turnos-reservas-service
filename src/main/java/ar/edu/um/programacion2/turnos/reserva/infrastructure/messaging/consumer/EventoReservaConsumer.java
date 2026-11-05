package ar.edu.um.programacion2.turnos.reserva.infrastructure.messaging.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EventoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.in.ProcesarEventoReservaUseCase;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.messaging.dto.EventoReservaMessage;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.messaging.mapper.EventoReservaMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventoReservaConsumer {

    private final JsonMapper jsonMapper;
    private final EventoReservaMessageMapper eventoReservaMessageMapper;
    private final ProcesarEventoReservaUseCase procesarEventoReservaUseCase;

    // Un mensaje ilegible o desconocido se descarta: reintentarlo no lo arreglaría.
    // Si procesarlo falla, la excepción llega al contenedor, que no confirma el offset y lo reintenta.
    @KafkaListener(topics = "${catedra.kafka.topic-acciones}")
    public void recibir(String mensaje) {
        EventoReservaMessage message;
        try {
            message = jsonMapper.readValue(mensaje, EventoReservaMessage.class);
        } catch (JacksonException e) {
            log.warn("Mensaje del topic de acciones que no es JSON válido: se descarta");
            return;
        }
        if (message.getEventId() == null || message.getReservationProcessId() == null) {
            log.warn("Evento {} sin eventId o reservationProcessId: se descarta", message.getEventType());
            return;
        }
        EventoReserva evento = eventoReservaMessageMapper.toDomain(message);
        if (evento == null) {
            log.info("Evento {} de tipo desconocido {}: se descarta", message.getEventId(), message.getEventType());
            return;
        }
        procesarEventoReservaUseCase.procesarEvento(evento);
    }
}
