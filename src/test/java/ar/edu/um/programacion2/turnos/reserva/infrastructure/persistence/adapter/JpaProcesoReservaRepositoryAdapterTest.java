package ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.entity.ProcesoReservaEntity;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.mapper.ProcesoReservaMapper;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.repository.JpaProcesoReservaRepository;

@ExtendWith(MockitoExtension.class)
class JpaProcesoReservaRepositoryAdapterTest {

    private static final Instant CREADO = Instant.parse("2026-10-27T14:00:00Z");

    @Mock
    private JpaProcesoReservaRepository jpaProcesoReservaRepository;

    private JpaProcesoReservaRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaProcesoReservaRepositoryAdapter(jpaProcesoReservaRepository, new ProcesoReservaMapper());
    }

    @Test
    void saveGuardaElProcesoYDevuelveElIdGenerado() {
        ProcesoReserva nuevo = ProcesoReserva.builder()
                .usuarioId(7L)
                .profesionalId(201L)
                .fecha(LocalDate.of(2026, 11, 2))
                .horaInicio(LocalTime.of(12, 0))
                .horaFin(LocalTime.of(12, 30))
                .estado(EstadoProceso.INICIADO)
                .creadoEn(CREADO)
                .actualizadoEn(CREADO)
                .build();
        when(jpaProcesoReservaRepository.save(any(ProcesoReservaEntity.class))).thenAnswer(invocacion -> {
            ProcesoReservaEntity entity = invocacion.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        ProcesoReserva guardado = adapter.save(nuevo);

        assertThat(guardado.getId()).isEqualTo(1L);
        assertThat(guardado).usingRecursiveComparison().ignoringFields("id").isEqualTo(nuevo);
    }

    @Test
    void findByIdDevuelveElProcesoCuandoExiste() {
        when(jpaProcesoReservaRepository.findById(1L)).thenReturn(Optional.of(
                ProcesoReservaEntity.builder().id(1L).estado(EstadoProceso.BLOQUEADO).build()));

        Optional<ProcesoReserva> encontrado = adapter.findById(1L);

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getEstado()).isEqualTo(EstadoProceso.BLOQUEADO);
    }

    @Test
    void findByIdDevuelveVacioCuandoNoExiste() {
        when(jpaProcesoReservaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(adapter.findById(99L)).isEmpty();
    }

    @Test
    void findByReservationProcessIdDevuelveElProcesoCuandoExiste() {
        when(jpaProcesoReservaRepository.findByReservationProcessId("process-a92b47d0")).thenReturn(Optional.of(
                ProcesoReservaEntity.builder().id(1L).reservationProcessId("process-a92b47d0").build()));

        assertThat(adapter.findByReservationProcessId("process-a92b47d0"))
                .map(ProcesoReserva::getId)
                .contains(1L);
    }

    @Test
    void findByReservationProcessIdDevuelveVacioCuandoNoExiste() {
        when(jpaProcesoReservaRepository.findByReservationProcessId("process-x")).thenReturn(Optional.empty());

        assertThat(adapter.findByReservationProcessId("process-x")).isEmpty();
    }
}
