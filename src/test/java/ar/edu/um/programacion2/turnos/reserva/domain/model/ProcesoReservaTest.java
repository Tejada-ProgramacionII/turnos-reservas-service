package ar.edu.um.programacion2.turnos.reserva.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import ar.edu.um.programacion2.turnos.reserva.domain.exception.TransicionInvalidaException;

class ProcesoReservaTest {

    @Test
    void pasarAUnEstadoPermitidoCambiaElEstado() {
        ProcesoReserva proceso = ProcesoReserva.builder().estado(EstadoProceso.INICIADO).build();

        proceso.pasarA(EstadoProceso.BLOQUEADO);

        assertThat(proceso.getEstado()).isEqualTo(EstadoProceso.BLOQUEADO);
    }

    @Test
    void pasarAUnEstadoNoPermitidoTiraExcepcionYNoCambiaElEstado() {
        ProcesoReserva proceso = ProcesoReserva.builder().estado(EstadoProceso.VENCIDO).build();

        assertThatThrownBy(() -> proceso.pasarA(EstadoProceso.CONFIRMADO))
                .isInstanceOf(TransicionInvalidaException.class)
                .hasMessage("El proceso no puede pasar de VENCIDO a CONFIRMADO");
        assertThat(proceso.getEstado()).isEqualTo(EstadoProceso.VENCIDO);
    }
}
