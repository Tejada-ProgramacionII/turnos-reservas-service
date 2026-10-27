package ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.dto;

import java.time.LocalDate;
import java.util.List;

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
public class OcupacionesCatedraResponse {
    private Long professionalId;
    private LocalDate from;
    private LocalDate to;
    private List<OcupacionCatedraResponse> occupancies;
}
