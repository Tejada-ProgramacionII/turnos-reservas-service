package ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ProcesoReservaRepository;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.entity.ProcesoReservaEntity;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.mapper.ProcesoReservaMapper;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.repository.JpaProcesoReservaRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaProcesoReservaRepositoryAdapter implements ProcesoReservaRepository {

    private final JpaProcesoReservaRepository jpaProcesoReservaRepository;
    private final ProcesoReservaMapper procesoReservaMapper;

    @Override
    public ProcesoReserva save(ProcesoReserva proceso) {
        ProcesoReservaEntity guardado = jpaProcesoReservaRepository.save(procesoReservaMapper.toEntity(proceso));
        return procesoReservaMapper.toDomainModel(guardado);
    }

    @Override
    public Optional<ProcesoReserva> findById(Long id) {
        return jpaProcesoReservaRepository.findById(id).map(procesoReservaMapper::toDomainModel);
    }

    @Override
    public Optional<ProcesoReserva> findByReservationProcessId(String reservationProcessId) {
        return jpaProcesoReservaRepository.findByReservationProcessId(reservationProcessId)
                .map(procesoReservaMapper::toDomainModel);
    }
}
