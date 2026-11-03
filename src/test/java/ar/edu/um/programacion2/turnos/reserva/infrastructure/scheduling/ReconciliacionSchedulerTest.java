package ar.edu.um.programacion2.turnos.reserva.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import ar.edu.um.programacion2.turnos.config.SchedulingConfig;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.in.ReconciliarProcesosUseCase;

class ReconciliacionSchedulerTest {

    private final ReconciliarProcesosUseCase reconciliarProcesosUseCase = mock(ReconciliarProcesosUseCase.class);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SchedulingConfig.class, ReconciliacionScheduler.class)
            .withBean(ReconciliarProcesosUseCase.class, () -> reconciliarProcesosUseCase);

    @Test
    void reconciliaCadaIntervaloConfigurado() {
        contextRunner.withPropertyValues("reconciliacion.intervalo=50ms")
                .run(context -> verify(reconciliarProcesosUseCase, timeout(2000).atLeast(2)).reconciliar());
    }

    @Test
    void reconciliaAlArrancar() {
        contextRunner.withPropertyValues("reconciliacion.intervalo=1h")
                .run(context -> {
                    context.publishEvent(new ApplicationReadyEvent(new SpringApplication(), new String[0],
                            context.getSourceApplicationContext(), Duration.ZERO));
                    verify(reconciliarProcesosUseCase, times(1)).reconciliar();
                });
    }

    @Test
    void conLaReconciliacionDesactivadaNoHayScheduler() {
        contextRunner.withPropertyValues("reconciliacion.habilitada=false")
                .run(context -> assertThat(context).doesNotHaveBean(ReconciliacionScheduler.class));
    }

    @Test
    void unErrorNoCortaLasVueltasSiguientes() {
        when(reconciliarProcesosUseCase.reconciliar()).thenThrow(new IllegalStateException("base caída"));

        assertThatCode(new ReconciliacionScheduler(reconciliarProcesosUseCase)::reconciliar)
                .doesNotThrowAnyException();
    }
}
