package ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.adapter;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.CatedraNoDisponibleException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.CatedraRechazoException;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.Ocupacion;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.ports.out.OcupacionesCatedraClient;
import ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.dto.OcupacionesCatedraResponse;
import ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.mapper.OcupacionClientMapper;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HttpOcupacionesCatedraClientAdapter implements OcupacionesCatedraClient {

    private static final String OCUPACIONES_PATH = "/api/appointment-occupancies";

    private final RestClient catedraRestClient;
    private final OcupacionClientMapper ocupacionClientMapper;

    @Override
    public List<Ocupacion> getOcupaciones(Long profesionalId, LocalDate desde, LocalDate hasta) {
        try {
            OcupacionesCatedraResponse response = catedraRestClient.get()
                    .uri(uriBuilder -> uriBuilder.path(OCUPACIONES_PATH)
                            .queryParam("professionalId", profesionalId)
                            .queryParam("from", desde)
                            .queryParam("to", hasta)
                            .build())
                    .retrieve()
                    .body(OcupacionesCatedraResponse.class);
            return response.getOccupancies().stream()
                    .map(ocupacionClientMapper::toDomainModel)
                    .toList();
        } catch (HttpClientErrorException e) {
            throw new CatedraRechazoException(
                    "La cátedra rechazó el pedido de ocupaciones (HTTP " + e.getStatusCode().value() + ")", e);
        } catch (HttpServerErrorException e) {
            throw new CatedraNoDisponibleException(
                    "La cátedra respondió con un error al pedir las ocupaciones (HTTP " + e.getStatusCode().value() + ")", e);
        } catch (ResourceAccessException e) {
            throw new CatedraNoDisponibleException("No se pudo conectar con la cátedra para pedir las ocupaciones", e);
        }
    }
}
