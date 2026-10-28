package ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.entity.ProcesoReservaEntity;

class ProcesoReservaMapperTest {

    private static final Instant CREADO = Instant.parse("2026-10-27T14:00:00Z");

    private final ProcesoReservaMapper mapper = new ProcesoReservaMapper();

    @Test
    void toEntityCopiaTodosLosCampos() {
        ProcesoReserva proceso = ProcesoReserva.builder()
                .id(1L)
                .usuarioId(7L)
                .profesionalId(201L)
                .fecha(LocalDate.of(2026, 11, 2))
                .horaInicio(LocalTime.of(12, 0))
                .horaFin(LocalTime.of(12, 30))
                .profesionalNombre("Ana")
                .profesionalApellido("Gomez")
                .holdId("hold-6c0c2f41")
                .reservationProcessId("process-a92b47d0")
                .expiresAt(CREADO.plusSeconds(600))
                .requestEventId("request-event-8a31")
                .mensajePedido("Ingrese un numero de telefono para continuar.")
                .telefono("+5492615551234")
                .reservationId(8251L)
                .motivo("abandonado por el usuario")
                .estado(EstadoProceso.TELEFONO_ENVIADO)
                .creadoEn(CREADO)
                .actualizadoEn(CREADO.plusSeconds(60))
                .build();

        ProcesoReservaEntity entity = mapper.toEntity(proceso);

        assertThat(entity).usingRecursiveComparison().isEqualTo(proceso);
    }

    @Test
    void toDomainModelCopiaTodosLosCampos() {
        ProcesoReservaEntity entity = ProcesoReservaEntity.builder()
                .id(2L)
                .usuarioId(7L)
                .profesionalId(201L)
                .fecha(LocalDate.of(2026, 11, 2))
                .horaInicio(LocalTime.of(9, 0))
                .horaFin(LocalTime.of(9, 30))
                .holdId("hold-1")
                .reservationProcessId("process-1")
                .expiresAt(CREADO.plusSeconds(600))
                .reservationId(10L)
                .estado(EstadoProceso.CONFIRMADO)
                .creadoEn(CREADO)
                .actualizadoEn(CREADO)
                .build();

        ProcesoReserva proceso = mapper.toDomainModel(entity);

        assertThat(proceso).usingRecursiveComparison().isEqualTo(entity);
    }

    @Test
    void toEntityDevuelveNullSiRecibeNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toDomainModelDevuelveNullSiRecibeNull() {
        assertThat(mapper.toDomainModel(null)).isNull();
    }
}
