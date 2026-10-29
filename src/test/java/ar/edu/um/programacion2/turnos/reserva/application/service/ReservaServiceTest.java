package ar.edu.um.programacion2.turnos.reserva.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.programacion2.turnos.reserva.application.exception.ProcesoReservaNoEncontradoException;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.in.GetProcesoReservaUseCase;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.in.IniciarReservaUseCase;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private IniciarReservaUseCase iniciarReservaUseCase;

    @Mock
    private GetProcesoReservaUseCase getProcesoReservaUseCase;

    @InjectMocks
    private ReservaService service;

    @Test
    void iniciarReservaDelegaEnElCasoDeUso() {
        ProcesoReserva proceso = ProcesoReserva.builder().id(1L).build();
        LocalDate fecha = LocalDate.of(2026, 11, 2);
        LocalTime hora = LocalTime.of(9, 30);
        when(iniciarReservaUseCase.iniciarReserva("jtejada", 201L, fecha, hora)).thenReturn(proceso);

        assertThat(service.iniciarReserva("jtejada", 201L, fecha, hora)).isSameAs(proceso);
    }

    @Test
    void getProcesoReservaDevuelveElProcesoCuandoExiste() {
        ProcesoReserva proceso = ProcesoReserva.builder().id(1L).build();
        when(getProcesoReservaUseCase.getProcesoReserva("jtejada", 1L)).thenReturn(Optional.of(proceso));

        assertThat(service.getProcesoReserva("jtejada", 1L)).isSameAs(proceso);
    }

    @Test
    void getProcesoReservaTiraExcepcionCuandoNoLoEncuentra() {
        when(getProcesoReservaUseCase.getProcesoReserva("jtejada", 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProcesoReserva("jtejada", 99L))
                .isInstanceOf(ProcesoReservaNoEncontradoException.class)
                .hasMessage("No se encontró el proceso de reserva con id: 99");
    }
}
