package ar.edu.um.programacion2.turnos.reserva.infrastructure.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
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
public class IniciarReservaRequest {
    @NotNull
    private Long profesionalId;

    @NotNull
    private LocalDate fecha;

    @NotNull
    private LocalTime horaInicio;
}
