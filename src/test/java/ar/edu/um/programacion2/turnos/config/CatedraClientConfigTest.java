package ar.edu.um.programacion2.turnos.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import com.sun.net.httpserver.HttpServer;

class CatedraClientConfigTest {

    private final AtomicReference<String> pathRecibido = new AtomicReference<>();
    private final AtomicReference<String> authorizationRecibido = new AtomicReference<>();
    private HttpServer servidor;

    @BeforeEach
    void levantarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", exchange -> {
            pathRecibido.set(exchange.getRequestURI().getPath());
            authorizationRecibido.set(exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        servidor.start();
    }

    @AfterEach
    void detenerServidor() {
        servidor.stop(0);
    }

    @Test
    void usaLaUrlBaseYMandaElJwtTecnico() {
        CatedraProperties properties = new CatedraProperties();
        properties.setBaseUrl("http://localhost:" + servidor.getAddress().getPort());
        properties.setJwt("jwt-de-prueba");

        RestClient restClient = new CatedraClientConfig().catedraRestClient(RestClient.builder(), properties);
        restClient.get().uri("/api/appointment-occupancies").retrieve().toBodilessEntity();

        assertThat(pathRecibido.get()).isEqualTo("/api/appointment-occupancies");
        assertThat(authorizationRecibido.get()).isEqualTo("Bearer jwt-de-prueba");
    }
}
