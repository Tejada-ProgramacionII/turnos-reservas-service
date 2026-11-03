package ar.edu.um.programacion2.turnos.reserva.domain.ports.in;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EventoReserva;

public interface ProcesarEventoReservaUseCase {
    void procesarEvento(EventoReserva evento);
}
