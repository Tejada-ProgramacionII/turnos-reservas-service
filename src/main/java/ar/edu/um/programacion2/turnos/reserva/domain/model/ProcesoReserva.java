package ar.edu.um.programacion2.turnos.reserva.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import ar.edu.um.programacion2.turnos.reserva.domain.exception.TransicionInvalidaException;
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
public class ProcesoReserva {
    private Long id;
    private Long usuarioId;
    private Long profesionalId;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String profesionalNombre;
    private String profesionalApellido;
    private String holdId;
    private String reservationProcessId;
    private Instant expiresAt;
    private String requestEventId;
    private String mensajePedido;
    private String telefono;
    private Long reservationId;
    private String motivo;
    private EstadoProceso estado;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public void pasarA(EstadoProceso nuevo) {
        if (!estado.puedePasarA(nuevo)) {
            throw new TransicionInvalidaException(estado, nuevo);
        }
        estado = nuevo;
    }
}
