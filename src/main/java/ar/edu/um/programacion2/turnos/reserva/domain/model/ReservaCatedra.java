package ar.edu.um.programacion2.turnos.reserva.domain.model;

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
public class ReservaCatedra {
    private String reservationProcessId;
    private Long reservationId;
    private EstadoReservaCatedra estado;
}
