package ar.edu.um.programacion2.turnos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.interceptor.JwtUsuarioInterceptor;

@Configuration
public class CatalogoClientConfig {

    @Bean
    public RestClient catalogoRestClient(RestClient.Builder builder, CatalogoProperties properties,
            JwtUsuarioInterceptor jwtUsuarioInterceptor) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());

        return builder
                .baseUrl(properties.getBaseUrl())
                .requestInterceptor(jwtUsuarioInterceptor)
                .requestFactory(requestFactory)
                .build();
    }
}
