package ar.edu.um.programacion2.turnos.reserva.infrastructure.client.mapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoReservaCatedra;
import ar.edu.um.programacion2.turnos.reserva.domain.model.HoldConfirmado;
import ar.edu.um.programacion2.turnos.reserva.domain.model.HoldCreado;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ReservaCancelada;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ReservaCatedra;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto.CancelacionCatedraRequest;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto.CancelacionCatedraResponse;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto.ConfirmacionHoldCatedraRequest;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto.ConfirmacionHoldCatedraResponse;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto.HoldCatedraRequest;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto.HoldCatedraResponse;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto.ReservaCatedraResponse;

@Component
public class ReservaClientMapper {

    private static final Map<String, EstadoReservaCatedra> ESTADOS = Map.of(
            "PHONE_PENDING", EstadoReservaCatedra.TELEFONO_PENDIENTE,
            "CONFIRMED", EstadoReservaCatedra.CONFIRMADA,
            "CANCELLED", EstadoReservaCatedra.CANCELADA,
            "FAILED", EstadoReservaCatedra.FALLIDA);

    public HoldCatedraRequest toHoldRequest(Long profesionalId, LocalDate fecha, LocalTime horaInicio,
            String externalPatientId) {
        return HoldCatedraRequest.builder()
                .professionalId(profesionalId)
                .date(fecha)
                .startTime(horaInicio)
                .externalPatientId(externalPatientId)
                .build();
    }

    public ConfirmacionHoldCatedraRequest toConfirmacionRequest(String externalPatientId, String nombre,
            String apellido) {
        return ConfirmacionHoldCatedraRequest.builder()
                .externalPatientId(externalPatientId)
                .patientFirstName(nombre)
                .patientLastName(apellido)
                .build();
    }

    public CancelacionCatedraRequest toCancelacionRequest(String motivo) {
        return CancelacionCatedraRequest.builder()
                .reason(motivo)
                .build();
    }

    public HoldCreado toHoldCreado(HoldCatedraResponse response) {
        if (response == null) {
            return null;
        }
        return HoldCreado.builder()
                .holdId(response.getHoldId())
                .reservationProcessId(response.getReservationProcessId())
                .venceEn(response.getExpiresAt())
                .build();
    }

    public HoldConfirmado toHoldConfirmado(ConfirmacionHoldCatedraResponse response) {
        if (response == null) {
            return null;
        }
        return HoldConfirmado.builder()
                .reservationProcessId(response.getReservationProcessId())
                .venceEn(response.getExpiresAt())
                .build();
    }

    public ReservaCancelada toReservaCancelada(CancelacionCatedraResponse response) {
        if (response == null) {
            return null;
        }
        return ReservaCancelada.builder()
                .reservationProcessId(response.getReservationProcessId())
                .canceladaEn(response.getCancelledAt())
                .build();
    }

    // Un status desconocido queda en null: la reconciliación no decide nada con él.
    public ReservaCatedra toReservaCatedra(ReservaCatedraResponse response) {
        if (response == null) {
            return null;
        }
        return ReservaCatedra.builder()
                .reservationProcessId(response.getReservationProcessId())
                .reservationId(response.getReservationId())
                .estado(response.getStatus() != null ? ESTADOS.get(response.getStatus()) : null)
                .build();
    }
}
