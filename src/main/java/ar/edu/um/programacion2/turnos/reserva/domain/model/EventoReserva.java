package ar.edu.um.programacion2.turnos.reserva.domain.model;

import java.time.Instant;

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
public class EventoReserva {
    private String eventId;
    private TipoEventoReserva tipo;
    private String reservationProcessId;
    private Long reservationId;
    private String motivo;
    private String mensaje;
    private Instant expiresAt;
}
