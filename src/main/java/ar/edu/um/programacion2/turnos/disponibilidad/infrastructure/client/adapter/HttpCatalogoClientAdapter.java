package ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.CatalogoNoDisponibleException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.CatalogoRechazoException;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.AgendaProfesional;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.ports.out.CatalogoClient;
import ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.dto.ProfesionalCatalogoResponse;
import ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.mapper.AgendaProfesionalClientMapper;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HttpCatalogoClientAdapter implements CatalogoClient {

    private static final String PROFESIONAL_PATH = "/api/profesionales/{id}";

    private final RestClient catalogoRestClient;
    private final AgendaProfesionalClientMapper agendaProfesionalClientMapper;

    @Override
    public Optional<AgendaProfesional> consultarAgenda(Long profesionalId) {
        try {
            ProfesionalCatalogoResponse response = catalogoRestClient.get()
                    .uri(PROFESIONAL_PATH, profesionalId)
                    .retrieve()
                    .body(ProfesionalCatalogoResponse.class);
            return Optional.ofNullable(agendaProfesionalClientMapper.toDomainModel(response));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (HttpClientErrorException e) {
            throw new CatalogoRechazoException(
                    "El catálogo rechazó la consulta de la agenda (HTTP " + e.getStatusCode().value() + ")", e);
        } catch (HttpServerErrorException e) {
            throw new CatalogoNoDisponibleException(
                    "El catálogo respondió con un error al consultar la agenda (HTTP " + e.getStatusCode().value() + ")", e);
        } catch (ResourceAccessException e) {
            throw new CatalogoNoDisponibleException("No se pudo conectar con el catálogo para consultar la agenda", e);
        }
    }
}
