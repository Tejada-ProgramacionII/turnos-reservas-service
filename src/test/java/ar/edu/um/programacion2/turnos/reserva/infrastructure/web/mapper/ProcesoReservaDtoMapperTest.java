package ar.edu.um.programacion2.turnos.reserva.infrastructure.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.web.dto.ProcesoReservaResponse;

class ProcesoReservaDtoMapperTest {

    private final ProcesoReservaDtoMapper mapper = new ProcesoReservaDtoMapper();

    @Test
    void toResponseExponeSoloLoQueVeElUsuario() {
        ProcesoReserva proceso = ProcesoReserva.builder()
                .id(1L)
                .usuarioId(7L)
                .profesionalId(201L)
                .profesionalNombre("Ana")
                .profesionalApellido("Gomez")
                .fecha(LocalDate.of(2026, 11, 2))
                .horaInicio(LocalTime.of(9, 30))
                .horaFin(LocalTime.of(10, 0))
                .holdId("hold-1")
                .reservationProcessId("process-1")
                .expiresAt(Instant.parse("2026-10-27T18:10:00Z"))
                .mensajePedido("Ingrese un numero de telefono para continuar.")
                .telefono("+5492615551234")
                .motivo("motivo")
                .estado(EstadoProceso.ESPERANDO_TELEFONO)
                .build();

        ProcesoReservaResponse response = mapper.toResponse(proceso);

        assertThat(response).usingRecursiveComparison()
                .ignoringFields("estado")
                .isEqualTo(proceso);
        assertThat(response.getEstado()).isEqualTo("ESPERANDO_TELEFONO");
    }

    @Test
    void toResponseSinEstadoDejaElEstadoEnNull() {
        assertThat(mapper.toResponse(ProcesoReserva.builder().id(1L).build()).getEstado()).isNull();
    }

    @Test
    void toResponseDevuelveNullSiRecibeNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
