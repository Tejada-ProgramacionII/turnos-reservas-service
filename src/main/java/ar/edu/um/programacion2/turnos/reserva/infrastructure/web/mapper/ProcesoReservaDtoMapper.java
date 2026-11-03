package ar.edu.um.programacion2.turnos.reserva.infrastructure.web.mapper;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.web.dto.ProcesoReservaResponse;

@Component
public class ProcesoReservaDtoMapper {

    public ProcesoReservaResponse toResponse(ProcesoReserva proceso) {
        if (proceso == null) {
            return null;
        }
        EstadoProceso estado = proceso.getEstado();
        return ProcesoReservaResponse.builder()
                .id(proceso.getId())
                .estado(estado != null ? estado.name() : null)
                .profesionalId(proceso.getProfesionalId())
                .profesionalNombre(proceso.getProfesionalNombre())
                .profesionalApellido(proceso.getProfesionalApellido())
                .fecha(proceso.getFecha())
                .horaInicio(proceso.getHoraInicio())
                .horaFin(proceso.getHoraFin())
                .expiresAt(proceso.getExpiresAt())
                .mensajePedido(proceso.getMensajePedido())
                .motivo(proceso.getMotivo())
                .puedeAbandonar(estado != null && estado.esPendiente())
                .puedeCancelar(estado != null && estado.esCancelable())
                .build();
    }
}
