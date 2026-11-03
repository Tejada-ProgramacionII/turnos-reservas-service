package ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.adapter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import ar.edu.um.programacion2.turnos.reserva.domain.model.Pagina;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ProcesoReservaRepository;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.entity.ProcesoReservaEntity;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.mapper.ProcesoReservaMapper;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.repository.JpaProcesoReservaRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaProcesoReservaRepositoryAdapter implements ProcesoReservaRepository {

    // El id desempata dos procesos creados en el mismo instante.
    private static final Sort MAS_RECIENTES_PRIMERO = Sort.by(Sort.Order.desc("creadoEn"), Sort.Order.desc("id"));

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

    @Override
    public Pagina<ProcesoReserva> findByUsuarioId(Long usuarioId, EstadoProceso estado, int pagina, int tamanio) {
        PageRequest pedido = PageRequest.of(pagina, tamanio, MAS_RECIENTES_PRIMERO);
        Page<ProcesoReservaEntity> resultado = estado == null
                ? jpaProcesoReservaRepository.findByUsuarioId(usuarioId, pedido)
                : jpaProcesoReservaRepository.findByUsuarioIdAndEstado(usuarioId, estado, pedido);
        return Pagina.<ProcesoReserva>builder()
                .contenido(resultado.map(procesoReservaMapper::toDomainModel).getContent())
                .total(resultado.getTotalElements())
                .build();
    }

    @Override
    public List<ProcesoReserva> findByEstados(Set<EstadoProceso> estados) {
        return jpaProcesoReservaRepository.findByEstadoIn(estados).stream()
                .map(procesoReservaMapper::toDomainModel)
                .toList();
    }

    @Override
    public List<ProcesoReserva> findConfirmadosDesde(LocalDate fecha) {
        return jpaProcesoReservaRepository.findByEstadoAndFechaGreaterThanEqual(EstadoProceso.CONFIRMADO, fecha).stream()
                .map(procesoReservaMapper::toDomainModel)
                .toList();
    }
}
