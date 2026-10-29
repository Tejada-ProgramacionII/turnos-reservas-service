package ar.edu.um.programacion2.turnos.reserva.infrastructure.web.mapper;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.web.dto.ProcesoReservaResponse;

@Component
public class ProcesoReservaDtoMapper {

    public ProcesoReservaResponse toResponse(ProcesoReserva proceso) {
        if (proceso == null) {
            return null;
        }
        return ProcesoReservaResponse.builder()
                .id(proceso.getId())
                .estado(proceso.getEstado() != null ? proceso.getEstado().name() : null)
                .profesionalId(proceso.getProfesionalId())
                .profesionalNombre(proceso.getProfesionalNombre())
                .profesionalApellido(proceso.getProfesionalApellido())
                .fecha(proceso.getFecha())
                .horaInicio(proceso.getHoraInicio())
                .horaFin(proceso.getHoraFin())
                .expiresAt(proceso.getExpiresAt())
                .mensajePedido(proceso.getMensajePedido())
                .motivo(proceso.getMotivo())
                .build();
    }
}
