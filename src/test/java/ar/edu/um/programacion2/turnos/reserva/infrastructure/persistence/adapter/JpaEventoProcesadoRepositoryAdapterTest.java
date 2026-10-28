package ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EventoProcesado;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.entity.EventoProcesadoEntity;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.mapper.EventoProcesadoMapper;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.repository.JpaEventoProcesadoRepository;

@ExtendWith(MockitoExtension.class)
class JpaEventoProcesadoRepositoryAdapterTest {

    @Mock
    private JpaEventoProcesadoRepository jpaEventoProcesadoRepository;

    private JpaEventoProcesadoRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaEventoProcesadoRepositoryAdapter(jpaEventoProcesadoRepository, new EventoProcesadoMapper());
    }

    @Test
    void existsByEventIdDelegaEnElRepositorio() {
        when(jpaEventoProcesadoRepository.existsById("request-event-8a31")).thenReturn(true);

        assertThat(adapter.existsByEventId("request-event-8a31")).isTrue();
    }

    @Test
    void saveGuardaElEvento() {
        EventoProcesado evento = EventoProcesado.builder()
                .eventId("request-event-8a31")
                .procesadoEn(Instant.parse("2026-10-27T14:00:00Z"))
                .build();
        when(jpaEventoProcesadoRepository.save(any(EventoProcesadoEntity.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        assertThat(adapter.save(evento)).usingRecursiveComparison().isEqualTo(evento);
    }
}
