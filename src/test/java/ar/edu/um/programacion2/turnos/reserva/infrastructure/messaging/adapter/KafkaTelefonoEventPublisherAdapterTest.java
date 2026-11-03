package ar.edu.um.programacion2.turnos.reserva.infrastructure.messaging.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import ar.edu.um.programacion2.turnos.config.CatedraKafkaProperties;
import ar.edu.um.programacion2.turnos.reserva.application.exception.MensajeriaNoDisponibleException;
import ar.edu.um.programacion2.turnos.reserva.domain.model.TelefonoInformado;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.messaging.mapper.TelefonoInformadoMessageMapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class KafkaTelefonoEventPublisherAdapterTest {

    private static final String TOPIC = "catedra.turnos.telefono.proyecto-test";

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private KafkaTelefonoEventPublisherAdapter adapter;

    @BeforeEach
    void setUp() {
        CatedraKafkaProperties properties = new CatedraKafkaProperties();
        properties.setTopicTelefono(TOPIC);
        properties.setGroupId("proyecto-test");
        properties.setTimeoutEnvio(Duration.ofMillis(50));
        adapter = new KafkaTelefonoEventPublisherAdapter(kafkaTemplate, jsonMapper,
                new TelefonoInformadoMessageMapper(), properties);
    }

    @Test
    void publicaElEventoConElReservationProcessIdComoClave() {
        when(kafkaTemplate.send(eq(TOPIC), eq("process-1"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, null)));

        assertThatCode(() -> adapter.publicar(evento())).doesNotThrowAnyException();

        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq("process-1"), mensaje.capture());
        JsonNode json = jsonMapper.readTree(mensaje.getValue());
        assertThat(json.get("eventType").asString()).isEqualTo("AdditionalInformationSubmitted");
        assertThat(json.get("groupId").asString()).isEqualTo("proyecto-test");
        assertThat(json.get("reservationProcessId").asString()).isEqualTo("process-1");
        assertThat(json.get("information").get("phoneNumber").asString()).isEqualTo("+5492615551234");
    }

    @Test
    void siElBrokerRechazaElEnvioTiraMensajeriaNoDisponible() {
        when(kafkaTemplate.send(eq(TOPIC), eq("process-1"), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker caído")));

        assertThatThrownBy(() -> adapter.publicar(evento()))
                .isInstanceOf(MensajeriaNoDisponibleException.class)
                .hasCauseInstanceOf(ExecutionException.class);
    }

    @Test
    void siElBrokerNoConfirmaATiempoTiraMensajeriaNoDisponible() {
        when(kafkaTemplate.send(eq(TOPIC), eq("process-1"), anyString())).thenReturn(new CompletableFuture<>());

        assertThatThrownBy(() -> adapter.publicar(evento()))
                .isInstanceOf(MensajeriaNoDisponibleException.class)
                .hasCauseInstanceOf(TimeoutException.class);
    }

    @Test
    void siElProductorFallaAlEnviarTiraMensajeriaNoDisponible() {
        when(kafkaTemplate.send(eq(TOPIC), eq("process-1"), anyString()))
                .thenThrow(new org.apache.kafka.common.errors.TimeoutException("sin metadata"));

        assertThatThrownBy(() -> adapter.publicar(evento()))
                .isInstanceOf(MensajeriaNoDisponibleException.class);
    }

    @Test
    void siSeInterrumpeLaEsperaTiraMensajeriaNoDisponibleYConservaLaInterrupcion() {
        when(kafkaTemplate.send(eq(TOPIC), eq("process-1"), anyString())).thenReturn(new CompletableFuture<>());
        Thread.currentThread().interrupt();

        try {
            assertThatThrownBy(() -> adapter.publicar(evento()))
                    .isInstanceOf(MensajeriaNoDisponibleException.class)
                    .hasCauseInstanceOf(InterruptedException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    private static TelefonoInformado evento() {
        return TelefonoInformado.builder()
                .eventId("phone-submitted-1")
                .reservationProcessId("process-1")
                .holdId("hold-1")
                .requestEventId("request-event-1")
                .telefono("+5492615551234")
                .occurredAt(Instant.parse("2026-10-27T18:00:00Z"))
                .build();
    }
}
