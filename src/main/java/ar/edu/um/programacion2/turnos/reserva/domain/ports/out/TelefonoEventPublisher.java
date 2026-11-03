package ar.edu.um.programacion2.turnos.reserva.domain.ports.out;

import ar.edu.um.programacion2.turnos.reserva.domain.model.TelefonoInformado;

public interface TelefonoEventPublisher {
    void publicar(TelefonoInformado evento);
}
