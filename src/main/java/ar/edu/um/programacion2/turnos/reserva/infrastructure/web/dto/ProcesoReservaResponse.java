package ar.edu.um.programacion2.turnos.reserva.infrastructure.web.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

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
public class ProcesoReservaResponse {
    private Long id;
    private String estado;
    private Long profesionalId;
    private String profesionalNombre;
    private String profesionalApellido;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Instant expiresAt;
    private String mensajePedido;
    private String motivo;
}
