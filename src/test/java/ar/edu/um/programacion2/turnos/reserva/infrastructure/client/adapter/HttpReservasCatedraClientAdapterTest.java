package ar.edu.um.programacion2.turnos.reserva.infrastructure.client.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.sun.net.httpserver.HttpServer;

import ar.edu.um.programacion2.turnos.config.CatedraClientConfig;
import ar.edu.um.programacion2.turnos.config.CatedraProperties;
import ar.edu.um.programacion2.turnos.reserva.application.exception.CatedraNoDisponibleException;
import ar.edu.um.programacion2.turnos.reserva.application.exception.CatedraRechazoException;
import ar.edu.um.programacion2.turnos.reserva.domain.model.HoldCreado;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ReservaCancelada;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.mapper.ReservaClientMapper;

class HttpReservasCatedraClientAdapterTest {

    private static final String JWT = "jwt-de-prueba";
    private static final LocalDate FECHA = LocalDate.of(2026, 9, 14);
    private static final LocalTime HORA = LocalTime.of(9, 30);

    // Ejemplos de las secciones 9, 12 y 13 del contrato.
    private static final String HOLD_JSON = """
            {
              "holdId": "hold-6c0c2f41",
              "reservationProcessId": "process-a92b47d0",
              "status": "HELD",
              "expiresAt": "2026-09-10T14:44:50Z"
            }
            """;

    private static final String CANCELACION_JSON = """
            {
              "reservationId": 8251,
              "reservationProcessId": "process-a92b47d0",
              "status": "CANCELLED",
              "cancelledAt": "2026-09-11T15:10:00Z"
            }
            """;

    private static final String SLOT_ALREADY_HELD_JSON = """
            {
              "type": "https://www.jhipster.tech/problem/problem-with-message",
              "title": "Conflict",
              "status": 409,
              "detail": "Slot already held",
              "message": "error.http.409",
              "path": "/api/appointment-holds",
              "code": "SLOT_ALREADY_HELD"
            }
            """;

    private final AtomicReference<String> metodoRecibido = new AtomicReference<>();
    private final AtomicReference<String> pathRecibido = new AtomicReference<>();
    private final AtomicReference<String> cuerpoRecibido = new AtomicReference<>();
    private final AtomicInteger statusRespuesta = new AtomicInteger();
    private final AtomicReference<String> tipoRespuesta = new AtomicReference<>(MediaType.APPLICATION_JSON_VALUE);
    private final AtomicReference<String> cuerpoRespuesta = new AtomicReference<>();
    private HttpServer servidor;

    @BeforeEach
    void levantarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", exchange -> {
            metodoRecibido.set(exchange.getRequestMethod());
            pathRecibido.set(exchange.getRequestURI().getPath());
            cuerpoRecibido.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (cuerpoRespuesta.get() == null) {
                exchange.sendResponseHeaders(statusRespuesta.get(), -1);
                exchange.close();
                return;
            }
            byte[] cuerpo = cuerpoRespuesta.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, tipoRespuesta.get());
            exchange.sendResponseHeaders(statusRespuesta.get(), cuerpo.length);
            try (OutputStream salida = exchange.getResponseBody()) {
                salida.write(cuerpo);
            }
        });
        servidor.start();
    }

    @AfterEach
    void detenerServidor() {
        servidor.stop(0);
    }

    @Test
    void crearHoldMandaElSlotYTraduceLaRespuesta() {
        responder(201, HOLD_JSON);

        HoldCreado hold = crearAdapter(servidor.getAddress().getPort()).crearHold(15L, FECHA, HORA, "user-42");

        assertThat(metodoRecibido.get()).isEqualTo("POST");
        assertThat(pathRecibido.get()).isEqualTo("/api/appointment-holds");
        assertThat(cuerpoRecibido.get())
                .contains("\"professionalId\":15")
                .contains("\"date\":\"2026-09-14\"")
                .contains("\"startTime\":\"09:30")
                .contains("\"externalPatientId\":\"user-42\"");
        assertThat(hold.getHoldId()).isEqualTo("hold-6c0c2f41");
        assertThat(hold.getReservationProcessId()).isEqualTo("process-a92b47d0");
        assertThat(hold.getVenceEn()).isEqualTo(Instant.parse("2026-09-10T14:44:50Z"));
    }

    @Test
    void cancelarMandaElMotivoYTraduceLaRespuesta() {
        responder(200, CANCELACION_JSON);

        ReservaCancelada cancelada = crearAdapter(servidor.getAddress().getPort())
                .cancelar("process-a92b47d0", "Abandonado por el usuario");

        assertThat(metodoRecibido.get()).isEqualTo("POST");
        assertThat(pathRecibido.get()).isEqualTo("/api/appointments/process-a92b47d0/cancel");
        assertThat(cuerpoRecibido.get()).contains("\"reason\":\"Abandonado por el usuario\"");
        assertThat(cancelada.getReservationProcessId()).isEqualTo("process-a92b47d0");
        assertThat(cancelada.getCanceladaEn()).isEqualTo(Instant.parse("2026-09-11T15:10:00Z"));
    }

    @Test
    void crearHoldLanzaRechazoConElCodeDelCuerpoDeError() {
        responder(409, SLOT_ALREADY_HELD_JSON);
        tipoRespuesta.set(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        HttpReservasCatedraClientAdapter adapter = crearAdapter(servidor.getAddress().getPort());

        assertThatThrownBy(() -> adapter.crearHold(15L, FECHA, HORA, "user-42"))
                .isInstanceOfSatisfying(CatedraRechazoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("SLOT_ALREADY_HELD"))
                .hasMessageContaining("HTTP 409")
                .hasMessageNotContaining(JWT)
                .hasMessageNotContaining("localhost");
    }

    @Test
    void cancelarLanzaRechazoSinCodeSiElErrorNoTraeCuerpo() {
        responder(401, null);
        HttpReservasCatedraClientAdapter adapter = crearAdapter(servidor.getAddress().getPort());

        assertThatThrownBy(() -> adapter.cancelar("process-a92b47d0", null))
                .isInstanceOfSatisfying(CatedraRechazoException.class, e -> assertThat(e.getCodigo()).isNull())
                .hasMessageContaining("HTTP 401")
                .hasMessageNotContaining(JWT)
                .hasMessageNotContaining("localhost");
    }

    @Test
    void crearHoldLanzaRechazoSinCodeSiElCuerpoDeErrorNoEsJson() {
        responder(403, "<html>Forbidden</html>");
        tipoRespuesta.set(MediaType.TEXT_HTML_VALUE);
        HttpReservasCatedraClientAdapter adapter = crearAdapter(servidor.getAddress().getPort());

        assertThatThrownBy(() -> adapter.crearHold(15L, FECHA, HORA, "user-42"))
                .isInstanceOfSatisfying(CatedraRechazoException.class, e -> assertThat(e.getCodigo()).isNull())
                .hasMessageContaining("HTTP 403");
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 503})
    void crearHoldLanzaNoDisponibleSiLaCatedraRespondeUn5xx(int status) {
        responder(status, null);
        HttpReservasCatedraClientAdapter adapter = crearAdapter(servidor.getAddress().getPort());

        assertThatThrownBy(() -> adapter.crearHold(15L, FECHA, HORA, "user-42"))
                .isInstanceOf(CatedraNoDisponibleException.class)
                .hasMessageContaining("HTTP " + status)
                .hasMessageNotContaining(JWT)
                .hasMessageNotContaining("localhost");
    }

    @Test
    void cancelarLanzaNoDisponibleSiNoHayNadieEscuchando() throws IOException {
        int puertoLibre;
        try (ServerSocket socket = new ServerSocket(0)) {
            puertoLibre = socket.getLocalPort();
        }
        HttpReservasCatedraClientAdapter adapter = crearAdapter(puertoLibre);

        assertThatThrownBy(() -> adapter.cancelar("process-a92b47d0", null))
                .isInstanceOf(CatedraNoDisponibleException.class)
                .hasMessageNotContaining(JWT)
                .hasMessageNotContaining("localhost");
    }

    private void responder(int status, String cuerpo) {
        statusRespuesta.set(status);
        cuerpoRespuesta.set(cuerpo);
    }

    private HttpReservasCatedraClientAdapter crearAdapter(int puerto) {
        CatedraProperties properties = new CatedraProperties();
        properties.setBaseUrl("http://localhost:" + puerto);
        properties.setJwt(JWT);
        RestClient restClient = new CatedraClientConfig().catedraRestClient(RestClient.builder(), properties);
        return new HttpReservasCatedraClientAdapter(restClient, new ReservaClientMapper());
    }
}
