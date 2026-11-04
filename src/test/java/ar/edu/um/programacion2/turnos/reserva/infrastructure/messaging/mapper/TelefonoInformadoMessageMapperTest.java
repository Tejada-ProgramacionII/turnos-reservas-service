package ar.edu.um.programacion2.turnos.reserva.infrastructure.messaging.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import ar.edu.um.programacion2.turnos.reserva.domain.model.TelefonoInformado;
import tools.jackson.databind.json.JsonMapper;

class TelefonoInformadoMessageMapperTest {

    // Ejemplo de AdditionalInformationSubmitted del contrato (15.5), tal cual.
    private static final String CONTRATO = """
            {
              "eventId": "phone-submitted-b91e",
              "eventType": "AdditionalInformationSubmitted",
              "groupId": "proyecto-juan-perez",
              "reservationProcessId": "process-a92b47d0",
              "holdId": "hold-6c0c2f41",
              "requestEventId": "request-event-8a31",
              "information": {
                "phoneNumber": "+5492615551234"
              },
              "occurredAt": "2026-09-10T14:36:00Z",
              "schemaVersion": 1
            }
            """;

    private final TelefonoInformadoMessageMapper mapper = new TelefonoInformadoMessageMapper();

    @Test
    void serializaIgualQueElEjemploDelContrato() throws Exception {
        TelefonoInformado evento = TelefonoInformado.builder()
                .eventId("phone-submitted-b91e")
                .reservationProcessId("process-a92b47d0")
                .holdId("hold-6c0c2f41")
                .requestEventId("request-event-8a31")
                .telefono("+5492615551234")
                .occurredAt(Instant.parse("2026-09-10T14:36:00Z"))
                .build();

        String json = JsonMapper.builder().build().writeValueAsString(mapper.toMessage(evento, "proyecto-juan-perez"));

        JSONAssert.assertEquals(CONTRATO, json, JSONCompareMode.STRICT);
    }

    @Test
    void devuelveNullSiElEventoEsNull() {
        assertThat(mapper.toMessage(null, "proyecto-juan-perez")).isNull();
    }
}
