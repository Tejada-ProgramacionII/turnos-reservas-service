package ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.CatedraNoDisponibleException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.CatedraRechazoException;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.EstadoOcupacion;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.Ocupacion;
import ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.mapper.OcupacionClientMapper;

class HttpOcupacionesCatedraClientAdapterTest {

    private static final String JWT = "jwt-de-prueba";
    private static final LocalDate FECHA = LocalDate.of(2026, 9, 14);

    // Ejemplo de la sección 8 del contrato, con un campo que no conocemos.
    private static final String OCUPACIONES_JSON = """
            {
              "professionalId": 15,
              "from": "2026-09-14",
              "to": "2026-09-14",
              "campoNuevo": "se ignora",
              "occupancies": [
                {
                  "date": "2026-09-14",
                  "startTime": "09:00:00",
                  "status": "CONFIRMED",
                  "expiresAt": null
                },
                {
                  "date": "2026-09-14",
                  "startTime": "09:30:00",
                  "status": "HELD",
                  "expiresAt": "2026-09-10T14:44:50Z"
                }
              ]
            }
            """;

    private static final String SIN_OCUPACIONES_JSON = """
            {"professionalId": 15, "from": "2026-09-14", "to": "2026-09-14", "occupancies": []}
            """;

    private final AtomicReference<String> pathRecibido = new AtomicReference<>();
    private final AtomicReference<String> queryRecibida = new AtomicReference<>();
    private final AtomicReference<String> cuerpoRespuesta = new AtomicReference<>(OCUPACIONES_JSON);
    private final AtomicInteger statusRespuesta = new AtomicInteger(200);
    private HttpServer servidor;

    @BeforeEach
    void levantarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", exchange -> {
            pathRecibido.set(exchange.getRequestURI().getPath());
            queryRecibida.set(exchange.getRequestURI().getQuery());
            byte[] cuerpo = cuerpoRespuesta.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
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
    void getOcupacionesPideElRangoDelProfesionalYLoTraduceAlDominio() {
        List<Ocupacion> ocupaciones = crearAdapter(servidor.getAddress().getPort(), Duration.ofSeconds(5))
                .getOcupaciones(15L, FECHA, FECHA);

        assertThat(pathRecibido.get()).isEqualTo("/api/appointment-occupancies");
        assertThat(queryRecibida.get()).isEqualTo("professionalId=15&from=2026-09-14&to=2026-09-14");
        assertThat(ocupaciones).hasSize(2);
        assertThat(ocupaciones.get(0)).satisfies(confirmada -> {
            assertThat(confirmada.getFecha()).isEqualTo(FECHA);
            assertThat(confirmada.getHoraInicio()).isEqualTo(LocalTime.of(9, 0));
            assertThat(confirmada.getEstado()).isEqualTo(EstadoOcupacion.CONFIRMADA);
            assertThat(confirmada.getVenceEn()).isNull();
        });
        assertThat(ocupaciones.get(1)).satisfies(retenida -> {
            assertThat(retenida.getHoraInicio()).isEqualTo(LocalTime.of(9, 30));
            assertThat(retenida.getEstado()).isEqualTo(EstadoOcupacion.RETENIDA);
            assertThat(retenida.getVenceEn()).isEqualTo(Instant.parse("2026-09-10T14:44:50Z"));
        });
    }

    @Test
    void getOcupacionesDevuelveListaVaciaSiNoHayOcupaciones() {
        cuerpoRespuesta.set(SIN_OCUPACIONES_JSON);

        List<Ocupacion> ocupaciones = crearAdapter(servidor.getAddress().getPort(), Duration.ofSeconds(5))
                .getOcupaciones(15L, FECHA, FECHA);

        assertThat(ocupaciones).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404})
    void getOcupacionesLanzaRechazoSiLaCatedraRespondeUn4xx(int status) {
        statusRespuesta.set(status);
        HttpOcupacionesCatedraClientAdapter adapter = crearAdapter(servidor.getAddress().getPort(), Duration.ofSeconds(5));

        assertThatThrownBy(() -> adapter.getOcupaciones(15L, FECHA, FECHA))
                .isInstanceOf(CatedraRechazoException.class)
                .hasMessageContaining("HTTP " + status)
                .hasMessageNotContaining(JWT)
                .hasMessageNotContaining("localhost");
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 503})
    void getOcupacionesLanzaNoDisponibleSiLaCatedraRespondeUn5xx(int status) {
        statusRespuesta.set(status);
        HttpOcupacionesCatedraClientAdapter adapter = crearAdapter(servidor.getAddress().getPort(), Duration.ofSeconds(5));

        assertThatThrownBy(() -> adapter.getOcupaciones(15L, FECHA, FECHA))
                .isInstanceOf(CatedraNoDisponibleException.class)
                .hasMessageContaining("HTTP " + status)
                .hasMessageNotContaining(JWT)
                .hasMessageNotContaining("localhost");
    }

    @Test
    void getOcupacionesLanzaNoDisponibleSiNoHayNadieEscuchando() throws IOException {
        int puertoLibre;
        try (ServerSocket socket = new ServerSocket(0)) {
            puertoLibre = socket.getLocalPort();
        }
        HttpOcupacionesCatedraClientAdapter adapter = crearAdapter(puertoLibre, Duration.ofSeconds(5));

        assertThatThrownBy(() -> adapter.getOcupaciones(15L, FECHA, FECHA))
                .isInstanceOf(CatedraNoDisponibleException.class)
                .hasMessageNotContaining(JWT)
                .hasMessageNotContaining("localhost");
    }

    @Test
    void getOcupacionesLanzaNoDisponibleSiLaCatedraNoRespondeATiempo() throws IOException {
        // Acepta la conexión pero nunca responde: la lectura corta por timeout.
        try (ServerSocket servidorMudo = new ServerSocket(0)) {
            HttpOcupacionesCatedraClientAdapter adapter = crearAdapter(servidorMudo.getLocalPort(), Duration.ofMillis(200));

            assertThatThrownBy(() -> adapter.getOcupaciones(15L, FECHA, FECHA))
                    .isInstanceOf(CatedraNoDisponibleException.class)
                    .hasMessageNotContaining(JWT)
                    .hasMessageNotContaining("localhost");
        }
    }

    private HttpOcupacionesCatedraClientAdapter crearAdapter(int puerto, Duration readTimeout) {
        CatedraProperties properties = new CatedraProperties();
        properties.setBaseUrl("http://localhost:" + puerto);
        properties.setJwt(JWT);
        properties.setReadTimeout(readTimeout);
        RestClient restClient = new CatedraClientConfig().catedraRestClient(RestClient.builder(), properties);
        return new HttpOcupacionesCatedraClientAdapter(restClient, new OcupacionClientMapper());
    }
}
