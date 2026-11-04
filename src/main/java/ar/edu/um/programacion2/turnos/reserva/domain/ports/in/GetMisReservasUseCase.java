package ar.edu.um.programacion2.turnos.reserva.domain.ports.in;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import ar.edu.um.programacion2.turnos.reserva.domain.model.Pagina;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;

public interface GetMisReservasUseCase {
    Pagina<ProcesoReserva> getMisReservas(String login, EstadoProceso estado, int pagina, int tamanio);
}
