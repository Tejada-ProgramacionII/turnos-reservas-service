package ar.edu.um.programacion2.turnos.reserva.application.service;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.stereotype.Service;

import ar.edu.um.programacion2.turnos.reserva.application.exception.ProcesoReservaNoEncontradoException;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.in.AbandonarProcesoUseCase;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.in.GetProcesoReservaUseCase;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.in.InformarTelefonoUseCase;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.in.IniciarReservaUseCase;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private final IniciarReservaUseCase iniciarReservaUseCase;
    private final GetProcesoReservaUseCase getProcesoReservaUseCase;
    private final InformarTelefonoUseCase informarTelefonoUseCase;
    private final AbandonarProcesoUseCase abandonarProcesoUseCase;

    public ProcesoReserva iniciarReserva(String login, Long profesionalId, LocalDate fecha, LocalTime horaInicio) {
        return iniciarReservaUseCase.iniciarReserva(login, profesionalId, fecha, horaInicio);
    }

    public ProcesoReserva getProcesoReserva(String login, Long id) {
        return getProcesoReservaUseCase.getProcesoReserva(login, id)
                .orElseThrow(() -> new ProcesoReservaNoEncontradoException(id));
    }

    public ProcesoReserva informarTelefono(String login, Long id, String telefono) {
        return informarTelefonoUseCase.informarTelefono(login, id, telefono)
                .orElseThrow(() -> new ProcesoReservaNoEncontradoException(id));
    }

    public ProcesoReserva abandonarProceso(String login, Long id) {
        return abandonarProcesoUseCase.abandonarProceso(login, id)
                .orElseThrow(() -> new ProcesoReservaNoEncontradoException(id));
    }
}
