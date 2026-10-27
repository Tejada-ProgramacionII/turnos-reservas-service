package ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

import com.sun.net.httpserver.HttpServer;

import ar.edu.um.programacion2.turnos.config.CatalogoClientConfig;
import ar.edu.um.programacion2.turnos.config.CatalogoProperties;
import ar.edu.um.programacion2.turnos.config.JwtUsuarioInterceptor;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.CatalogoNoDisponibleException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.CatalogoRechazoException;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.AgendaProfesional;
import ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.mapper.AgendaProfesionalClientMapper;

class HttpCatalogoClientAdapterTest {

    private static final String JWT = "jwt-del-usuario";

    // Ejemplo de docs/contrato-turnos.md del catálogo, con un campo que no conocemos.
    private static final String PROFESIONAL_JSON = """
            {
              "id": 201,
              "categoriaId": 101,
              "nombre": "Ana",
              "apellido": "Torres",
              "habilitado": true,
              "campoNuevo": "se ignora",
              "horarios": [
                { "diaSemana": "MONDAY", "horaInicio": "12:00:00", "horaFin": "15:00:00", "duracionTurnoMinutos": 30 },
                { "diaSemana": "WEDNESDAY", "horaInicio": "17:00:00", "horaFin": "20:00:00", "duracionTurnoMinutos": 30 }
              ],
              "versionCatalogo": 1
            }
            """;

    private static final String NO_EXISTE_JSON = """
            {"title": "Not Found", "status": 404, "detail": "No existe el profesional 99999", "instance": "/api/profesionales/99999"}
            """;

    private static final String NO_DISPONIBLE_JSON = """
            {"title": "Service Unavailable", "status": 503,
             "detail": "El catálogo todavía no está disponible: no hay una sincronización completa aplicada",
             "instance": "/api/profesionales/201"}
            """;

    private final AtomicReference<String> pathRecibido = new AtomicReference<>();
    private final AtomicReference<String> authorizationRecibido = new AtomicReference<>();
    private final AtomicInteger statusRespuesta = new AtomicInteger(200);
    private final AtomicReference<String> cuerpoRespuesta = new AtomicReference<>(PROFESIONAL_JSON);
    private HttpServer servidor;

    @BeforeEach
    void levantarServidor() throws IOException {
        Jwt jwt = Jwt.withTokenValue(JWT).header("alg", "HS512").subject("ana").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", exchange -> {
            pathRecibido.set(exchange.getRequestURI().getPath());
            authorizationRecibido.set(exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION));
            if (cuerpoRespuesta.get() == null) {
                exchange.sendResponseHeaders(statusRespuesta.get(), -1);
                exchange.close();
                return;
            }
            byte[] cuerpo = cuerpoRespuesta.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE,
                    statusRespuesta.get() == 200 ? MediaType.APPLICATION_JSON_VALUE : MediaType.APPLICATION_PROBLEM_JSON_VALUE);
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
        SecurityContextHolder.clearContext();
    }

    @Test
    void consultarAgendaPideElProfesionalConElJwtDelUsuarioYLoTraduce() {
        Optional<AgendaProfesional> agenda = crearAdapter(servidor.getAddress().getPort(), Duration.ofSeconds(5))
                .consultarAgenda(201L);

        assertThat(pathRecibido.get()).isEqualTo("/api/profesionales/201");
        assertThat(authorizationRecibido.get()).isEqualTo("Bearer " + JWT);
        assertThat(agenda).hasValueSatisfying(profesional -> {
            assertThat(profesional.getProfesionalId()).isEqualTo(201L);
            assertThat(profesional.getNombre()).isEqualTo("Ana");
            assertThat(profesional.isHabilitado()).isTrue();
            assertThat(profesional.getVersionCatalogo()).isEqualTo(1L);
            assertThat(profesional.getHorarios()).hasSize(2);
            assertThat(profesional.getHorarios().get(1).getDiaSemana()).isEqualTo(DayOfWeek.WEDNESDAY);
            assertThat(profesional.getHorarios().get(1).getHoraInicio()).isEqualTo(LocalTime.of(17, 0));
        });
    }

    @Test
    void consultarAgendaDevuelveVacioSiElProfesionalNoExiste() {
        responder(404, NO_EXISTE_JSON);

        assertThat(crearAdapter(servidor.getAddress().getPort(), Duration.ofSeconds(5)).consultarAgenda(99999L))
                .isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403})
    void consultarAgendaLanzaRechazoSiElCatalogoRespondeOtro4xx(int status) {
        responder(status, null);
        HttpCatalogoClientAdapter adapter = crearAdapter(servidor.getAddress().getPort(), Duration.ofSeconds(5));

        assertThatThrownBy(() -> adapter.consultarAgenda(201L))
                .isInstanceOf(CatalogoRechazoException.class)
                .hasMessageContaining("HTTP " + status)
                .hasMessageNotContaining(JWT)
                .hasMessageNotContaining("localhost");
    }

    @Test
    void consultarAgendaLanzaNoDisponibleSiElCatalogoNoTieneSincronizacion() {
        responder(503, NO_DISPONIBLE_JSON);
        HttpCatalogoClientAdapter adapter = crearAdapter(servidor.getAddress().getPort(), Duration.ofSeconds(5));

        assertThatThrownBy(() -> adapter.consultarAgenda(201L))
                .isInstanceOf(CatalogoNoDisponibleException.class)
                .hasMessageContaining("HTTP 503")
                .hasMessageNotContaining(JWT)
                .hasMessageNotContaining("localhost");
    }

    @Test
    void consultarAgendaLanzaNoDisponibleSiElCatalogoFallaConUn500() {
        responder(500, null);
        HttpCatalogoClientAdapter adapter = crearAdapter(servidor.getAddress().getPort(), Duration.ofSeconds(5));

        assertThatThrownBy(() -> adapter.consultarAgenda(201L))
                .isInstanceOf(CatalogoNoDisponibleException.class)
                .hasMessageContaining("HTTP 500");
    }

    @Test
    void consultarAgendaLanzaNoDisponibleSiNoHayNadieEscuchando() throws IOException {
        int puertoLibre;
        try (ServerSocket socket = new ServerSocket(0)) {
            puertoLibre = socket.getLocalPort();
        }
        HttpCatalogoClientAdapter adapter = crearAdapter(puertoLibre, Duration.ofSeconds(5));

        assertThatThrownBy(() -> adapter.consultarAgenda(201L))
                .isInstanceOf(CatalogoNoDisponibleException.class)
                .hasMessageNotContaining(JWT)
                .hasMessageNotContaining("localhost");
    }

    @Test
    void consultarAgendaLanzaNoDisponibleSiElCatalogoNoRespondeATiempo() throws IOException {
        // Acepta la conexión pero nunca responde: la lectura corta por timeout.
        try (ServerSocket servidorMudo = new ServerSocket(0)) {
            HttpCatalogoClientAdapter adapter = crearAdapter(servidorMudo.getLocalPort(), Duration.ofMillis(200));

            assertThatThrownBy(() -> adapter.consultarAgenda(201L))
                    .isInstanceOf(CatalogoNoDisponibleException.class)
                    .hasMessageNotContaining(JWT);
        }
    }

    private void responder(int status, String cuerpo) {
        statusRespuesta.set(status);
        cuerpoRespuesta.set(cuerpo);
    }

    private HttpCatalogoClientAdapter crearAdapter(int puerto, Duration readTimeout) {
        CatalogoProperties properties = new CatalogoProperties();
        properties.setBaseUrl("http://localhost:" + puerto);
        properties.setReadTimeout(readTimeout);
        RestClient restClient = new CatalogoClientConfig()
                .catalogoRestClient(RestClient.builder(), properties, new JwtUsuarioInterceptor());
        return new HttpCatalogoClientAdapter(restClient, new AgendaProfesionalClientMapper());
    }
}
