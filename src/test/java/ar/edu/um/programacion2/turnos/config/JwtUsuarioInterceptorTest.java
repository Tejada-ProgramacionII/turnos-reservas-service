package ar.edu.um.programacion2.turnos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class JwtUsuarioInterceptorTest {

    private static final byte[] SIN_CUERPO = new byte[0];

    private final JwtUsuarioInterceptor interceptor = new JwtUsuarioInterceptor();
    private final MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("/api/profesionales/201"));
    private final ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
    private final ClientHttpResponse response = mock(ClientHttpResponse.class);

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void agregaElJwtDelUsuarioAutenticado() throws IOException {
        Jwt jwt = Jwt.withTokenValue("jwt-del-usuario").header("alg", "HS512").subject("ana").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        when(execution.execute(request, SIN_CUERPO)).thenReturn(response);

        assertThat(interceptor.intercept(request, SIN_CUERPO, execution)).isSameAs(response);
        assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer jwt-del-usuario");
    }

    @Test
    void noAgregaAuthorizationSiNoHayUsuarioAutenticado() throws IOException {
        interceptor.intercept(request, SIN_CUERPO, execution);

        assertThat(request.getHeaders().containsHeader(HttpHeaders.AUTHORIZATION)).isFalse();
        verify(execution).execute(request, SIN_CUERPO);
    }

    @Test
    void noAgregaAuthorizationSiLaAutenticacionNoEsUnJwt() throws IOException {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("ana", "clave"));

        interceptor.intercept(request, SIN_CUERPO, execution);

        assertThat(request.getHeaders().containsHeader(HttpHeaders.AUTHORIZATION)).isFalse();
    }
}
