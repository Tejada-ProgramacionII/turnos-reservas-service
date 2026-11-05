package ar.edu.um.programacion2.turnos.reserva.domain.ports.in;

import java.time.LocalDate;
import java.time.LocalTime;

import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;

public interface IniciarReservaUseCase {
    ProcesoReserva iniciarReserva(String login, Long profesionalId, LocalDate fecha, LocalTime horaInicio);
}
