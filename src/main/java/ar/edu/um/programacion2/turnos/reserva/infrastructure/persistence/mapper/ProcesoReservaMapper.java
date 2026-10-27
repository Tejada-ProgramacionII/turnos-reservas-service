package ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.entity.ProcesoReservaEntity;

@Component
public class ProcesoReservaMapper {

    public ProcesoReservaEntity toEntity(ProcesoReserva proceso) {
        if (proceso == null) {
            return null;
        }
        return ProcesoReservaEntity.builder()
                .id(proceso.getId())
                .usuarioId(proceso.getUsuarioId())
                .profesionalId(proceso.getProfesionalId())
                .fecha(proceso.getFecha())
                .horaInicio(proceso.getHoraInicio())
                .horaFin(proceso.getHoraFin())
                .profesionalNombre(proceso.getProfesionalNombre())
                .profesionalApellido(proceso.getProfesionalApellido())
                .holdId(proceso.getHoldId())
                .reservationProcessId(proceso.getReservationProcessId())
                .expiresAt(proceso.getExpiresAt())
                .requestEventId(proceso.getRequestEventId())
                .mensajePedido(proceso.getMensajePedido())
                .telefono(proceso.getTelefono())
                .reservationId(proceso.getReservationId())
                .motivo(proceso.getMotivo())
                .estado(proceso.getEstado())
                .creadoEn(proceso.getCreadoEn())
                .actualizadoEn(proceso.getActualizadoEn())
                .build();
    }

    public ProcesoReserva toDomainModel(ProcesoReservaEntity entity) {
        if (entity == null) {
            return null;
        }
        return ProcesoReserva.builder()
                .id(entity.getId())
                .usuarioId(entity.getUsuarioId())
                .profesionalId(entity.getProfesionalId())
                .fecha(entity.getFecha())
                .horaInicio(entity.getHoraInicio())
                .horaFin(entity.getHoraFin())
                .profesionalNombre(entity.getProfesionalNombre())
                .profesionalApellido(entity.getProfesionalApellido())
                .holdId(entity.getHoldId())
                .reservationProcessId(entity.getReservationProcessId())
                .expiresAt(entity.getExpiresAt())
                .requestEventId(entity.getRequestEventId())
                .mensajePedido(entity.getMensajePedido())
                .telefono(entity.getTelefono())
                .reservationId(entity.getReservationId())
                .motivo(entity.getMotivo())
                .estado(entity.getEstado())
                .creadoEn(entity.getCreadoEn())
                .actualizadoEn(entity.getActualizadoEn())
                .build();
    }
}
