package ar.edu.um.programacion2.turnos.reserva.infrastructure.web.dto;

import jakarta.validation.constraints.Size;
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
public class CancelarReservaRequest {
    // La cátedra acepta hasta 500 caracteres (contrato 12).
    @Size(max = 500)
    private String motivo;
}
