package ar.edu.um.programacion2.turnos.reserva.infrastructure.client.mapper;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.reserva.domain.model.HoldConfirmado;
import ar.edu.um.programacion2.turnos.reserva.domain.model.HoldCreado;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ReservaCancelada;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto.CancelacionCatedraRequest;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto.CancelacionCatedraResponse;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto.ConfirmacionHoldCatedraRequest;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto.ConfirmacionHoldCatedraResponse;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto.HoldCatedraRequest;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto.HoldCatedraResponse;

@Component
public class ReservaClientMapper {

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
}
