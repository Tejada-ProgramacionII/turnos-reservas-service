package ar.edu.um.programacion2.turnos.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

import com.sun.net.httpserver.HttpServer;

import ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.interceptor.JwtUsuarioInterceptor;

class CatalogoClientConfigTest {

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
        SecurityContextHolder.clearContext();
    }

    @Test
    void usaLaUrlBaseYMandaElJwtDelUsuarioAutenticado() {
        Jwt jwt = Jwt.withTokenValue("jwt-del-usuario").header("alg", "HS512").subject("ana").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        CatalogoProperties properties = new CatalogoProperties();
        properties.setBaseUrl("http://localhost:" + servidor.getAddress().getPort());

        RestClient restClient = new CatalogoClientConfig()
                .catalogoRestClient(RestClient.builder(), properties, new JwtUsuarioInterceptor());
        restClient.get().uri("/api/profesionales/201").retrieve().toBodilessEntity();

        assertThat(pathRecibido.get()).isEqualTo("/api/profesionales/201");
        assertThat(authorizationRecibido.get()).isEqualTo("Bearer jwt-del-usuario");
    }
}
