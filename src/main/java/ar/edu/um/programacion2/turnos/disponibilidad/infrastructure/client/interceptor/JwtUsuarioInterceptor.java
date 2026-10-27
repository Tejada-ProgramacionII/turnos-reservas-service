package ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.interceptor;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

// Propaga al catálogo el JWT del usuario que hizo el pedido (D-12 del catálogo).
@Component
public class JwtUsuarioInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken autenticacion) {
            request.getHeaders().setBearerAuth(autenticacion.getToken().getTokenValue());
        }
        return execution.execute(request, body);
    }
}
