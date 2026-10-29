package ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.web.dto;

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
public class DisponibilidadResponse {
    private Long profesionalId;
    private LocalDate fecha;
    private List<TurnoLibreResponse> turnos;
}
