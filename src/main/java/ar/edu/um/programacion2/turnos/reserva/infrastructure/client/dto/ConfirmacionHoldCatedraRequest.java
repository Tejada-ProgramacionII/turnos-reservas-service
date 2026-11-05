package ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmacionHoldCatedraRequest {
    private String externalPatientId;
    private String patientFirstName;
    private String patientLastName;
}
