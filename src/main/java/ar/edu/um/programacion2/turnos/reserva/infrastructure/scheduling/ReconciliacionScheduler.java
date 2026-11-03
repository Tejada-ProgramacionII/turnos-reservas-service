package ar.edu.um.programacion2.turnos.reserva.infrastructure.scheduling;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.reserva.domain.ports.in.ReconciliarProcesosUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// CU-09 / D-14: reconcilia los procesos pendientes al arrancar y cada intervalo.
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "reconciliacion.habilitada", havingValue = "true", matchIfMissing = true)
public class ReconciliacionScheduler {

    private final ReconciliarProcesosUseCase reconciliarProcesosUseCase;

    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        reconciliar();
    }

    // La primera vuelta periódica espera un intervalo: al arrancar ya reconcilia alArrancar.
    @Scheduled(fixedDelayString = "${reconciliacion.intervalo:1m}",
            initialDelayString = "${reconciliacion.intervalo:1m}")
    public void reconciliar() {
        try {
            reconciliarProcesosUseCase.reconciliar();
        } catch (RuntimeException e) {
            log.error("Reconciliación fallida ({}): {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
