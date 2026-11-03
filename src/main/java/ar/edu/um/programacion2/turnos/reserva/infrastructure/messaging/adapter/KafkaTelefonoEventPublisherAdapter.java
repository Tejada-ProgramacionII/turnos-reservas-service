package ar.edu.um.programacion2.turnos.reserva.infrastructure.messaging.adapter;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.config.CatedraKafkaProperties;
import ar.edu.um.programacion2.turnos.reserva.application.exception.MensajeriaNoDisponibleException;
import ar.edu.um.programacion2.turnos.reserva.domain.model.TelefonoInformado;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.TelefonoEventPublisher;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.messaging.mapper.TelefonoInformadoMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTelefonoEventPublisherAdapter implements TelefonoEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;
    private final TelefonoInformadoMessageMapper telefonoInformadoMessageMapper;
    private final CatedraKafkaProperties properties;

    // Se espera la confirmación del broker; los reintentos limitados los hace el productor (application.properties).
    // El mensaje lleva el teléfono: ni el mensaje ni la excepción van a los logs.
    @Override
    public void publicar(TelefonoInformado evento) {
        String mensaje = jsonMapper.writeValueAsString(
                telefonoInformadoMessageMapper.toMessage(evento, properties.getGroupId()));
        try {
            kafkaTemplate.send(properties.getTopicTelefono(), evento.getReservationProcessId(), mensaje)
                    .get(properties.getTimeoutEnvio().toMillis(), TimeUnit.MILLISECONDS);
            log.info("Teléfono del proceso {} publicado con el evento {}",
                    evento.getReservationProcessId(), evento.getEventId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw noDisponible(evento, e);
        } catch (ExecutionException | TimeoutException | org.apache.kafka.common.KafkaException
                | org.springframework.kafka.KafkaException e) {
            throw noDisponible(evento, e);
        }
    }

    private static MensajeriaNoDisponibleException noDisponible(TelefonoInformado evento, Exception e) {
        log.warn("Kafka no confirmó el evento {} del proceso {} ({})",
                evento.getEventId(), evento.getReservationProcessId(), e.getClass().getSimpleName());
        return new MensajeriaNoDisponibleException("Kafka no confirmó el envío del teléfono", e);
    }
}
