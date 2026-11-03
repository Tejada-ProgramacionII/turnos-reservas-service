package ar.edu.um.programacion2.turnos.reserva.domain.ports.out;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import ar.edu.um.programacion2.turnos.reserva.domain.model.HoldConfirmado;
import ar.edu.um.programacion2.turnos.reserva.domain.model.HoldCreado;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ReservaCancelada;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ReservaCatedra;

public interface ReservasCatedraClient {
    HoldCreado crearHold(Long profesionalId, LocalDate fecha, LocalTime horaInicio, String externalPatientId);

    HoldConfirmado confirmarHold(String holdId, String externalPatientId, String nombre, String apellido);

    ReservaCancelada cancelar(String reservationProcessId, String motivo);

    List<ReservaCatedra> listarReservas(String externalPatientId);
}
