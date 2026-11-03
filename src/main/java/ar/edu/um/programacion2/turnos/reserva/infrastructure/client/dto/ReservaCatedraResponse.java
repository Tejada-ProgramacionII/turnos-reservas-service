package ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Solo los campos que usa la reconciliación: el resto (entre ellos el teléfono del paciente) no se lee.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReservaCatedraResponse {
    private Long reservationId;
    private String reservationProcessId;
    private String status;
}
