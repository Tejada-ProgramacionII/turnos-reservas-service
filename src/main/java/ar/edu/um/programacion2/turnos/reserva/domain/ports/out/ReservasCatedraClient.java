package ar.edu.um.programacion2.turnos.reserva.domain.ports.out;

import java.time.LocalDate;
import java.time.LocalTime;

import ar.edu.um.programacion2.turnos.reserva.domain.model.HoldCreado;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ReservaCancelada;

public interface ReservasCatedraClient {
    HoldCreado crearHold(Long profesionalId, LocalDate fecha, LocalTime horaInicio, String externalPatientId);

    ReservaCancelada cancelar(String reservationProcessId, String motivo);
}
