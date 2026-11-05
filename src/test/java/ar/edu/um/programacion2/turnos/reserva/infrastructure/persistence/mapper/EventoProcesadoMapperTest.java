package ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EventoProcesado;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.entity.EventoProcesadoEntity;

class EventoProcesadoMapperTest {

    private static final Instant PROCESADO = Instant.parse("2026-10-27T14:00:00Z");

    private final EventoProcesadoMapper mapper = new EventoProcesadoMapper();

    @Test
    void toEntityCopiaTodosLosCampos() {
        EventoProcesado evento = EventoProcesado.builder().eventId("request-event-8a31").procesadoEn(PROCESADO).build();

        assertThat(mapper.toEntity(evento)).usingRecursiveComparison().isEqualTo(evento);
    }

    @Test
    void toDomainModelCopiaTodosLosCampos() {
        EventoProcesadoEntity entity = EventoProcesadoEntity.builder()
                .eventId("confirmation-event-7d10")
                .procesadoEn(PROCESADO)
                .build();

        assertThat(mapper.toDomainModel(entity)).usingRecursiveComparison().isEqualTo(entity);
    }

    @Test
    void devuelveNullSiRecibeNull() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomainModel(null)).isNull();
    }
}
