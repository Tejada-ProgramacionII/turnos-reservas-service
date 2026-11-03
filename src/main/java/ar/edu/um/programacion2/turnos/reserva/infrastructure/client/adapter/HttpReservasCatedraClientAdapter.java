package ar.edu.um.programacion2.turnos.reserva.infrastructure.client.adapter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.function.Supplier;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import ar.edu.um.programacion2.turnos.reserva.application.exception.CatedraNoDisponibleException;
import ar.edu.um.programacion2.turnos.reserva.application.exception.CatedraRechazoException;
import ar.edu.um.programacion2.turnos.reserva.domain.model.HoldConfirmado;
import ar.edu.um.programacion2.turnos.reserva.domain.model.HoldCreado;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ReservaCancelada;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ReservasCatedraClient;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto.CancelacionCatedraResponse;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto.ConfirmacionHoldCatedraResponse;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto.ErrorCatedraResponse;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto.HoldCatedraResponse;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.mapper.ReservaClientMapper;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HttpReservasCatedraClientAdapter implements ReservasCatedraClient {

    private static final String HOLDS_PATH = "/api/appointment-holds";
    private static final String CONFIRMAR_PATH = "/api/appointment-holds/{holdId}/confirm";
    private static final String CANCELAR_PATH = "/api/appointments/{reservationProcessId}/cancel";

    private final RestClient catedraRestClient;
    private final ReservaClientMapper reservaClientMapper;

    @Override
    public HoldCreado crearHold(Long profesionalId, LocalDate fecha, LocalTime horaInicio, String externalPatientId) {
        return llamarALaCatedra("crear el hold", () -> {
            HoldCatedraResponse response = catedraRestClient.post()
                    .uri(HOLDS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(reservaClientMapper.toHoldRequest(profesionalId, fecha, horaInicio, externalPatientId))
                    .retrieve()
                    .body(HoldCatedraResponse.class);
            return reservaClientMapper.toHoldCreado(response);
        });
    }

    @Override
    public HoldConfirmado confirmarHold(String holdId, String externalPatientId, String nombre, String apellido) {
        return llamarALaCatedra("confirmar el hold", () -> {
            ConfirmacionHoldCatedraResponse response = catedraRestClient.post()
                    .uri(CONFIRMAR_PATH, holdId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(reservaClientMapper.toConfirmacionRequest(externalPatientId, nombre, apellido))
                    .retrieve()
                    .body(ConfirmacionHoldCatedraResponse.class);
            return reservaClientMapper.toHoldConfirmado(response);
        });
    }

    @Override
    public ReservaCancelada cancelar(String reservationProcessId, String motivo) {
        return llamarALaCatedra("cancelar la reserva", () -> {
            CancelacionCatedraResponse response = catedraRestClient.post()
                    .uri(CANCELAR_PATH, reservationProcessId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(reservaClientMapper.toCancelacionRequest(motivo))
                    .retrieve()
                    .body(CancelacionCatedraResponse.class);
            return reservaClientMapper.toReservaCancelada(response);
        });
    }

    // La sección 13 del contrato pide decidir por status y code, nunca por el detail.
    private <T> T llamarALaCatedra(String operacion, Supplier<T> pedido) {
        try {
            return pedido.get();
        } catch (HttpClientErrorException e) {
            String codigo = leerCodigo(e);
            throw new CatedraRechazoException(
                    "La cátedra rechazó el pedido para " + operacion
                            + " (HTTP " + e.getStatusCode().value() + ", code " + codigo + ")",
                    codigo, e);
        } catch (HttpServerErrorException e) {
            throw new CatedraNoDisponibleException(
                    "La cátedra respondió con un error al " + operacion + " (HTTP " + e.getStatusCode().value() + ")", e);
        } catch (ResourceAccessException e) {
            throw new CatedraNoDisponibleException("No se pudo conectar con la cátedra para " + operacion, e);
        }
    }

    // Los 401 y 403 de Spring Security pueden venir sin cuerpo o sin code.
    private String leerCodigo(HttpClientErrorException e) {
        try {
            ErrorCatedraResponse error = e.getResponseBodyAs(ErrorCatedraResponse.class);
            return error != null ? error.getCode() : null;
        } catch (RestClientException cuerpoIlegible) {
            return null;
        }
    }
}
