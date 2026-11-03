package ar.edu.um.programacion2.turnos.reserva.domain.ports.in;

import java.util.Optional;

import ar.edu.um.programacion2.turnos.reserva.domain.model.ResultadoReconciliacion;

public interface ReconciliarProcesosUseCase {
    // Vacío si ya había una reconciliación en curso.
    Optional<ResultadoReconciliacion> reconciliar();
}
