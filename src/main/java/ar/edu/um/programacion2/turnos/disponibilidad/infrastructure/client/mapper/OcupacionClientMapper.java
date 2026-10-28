package ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.mapper;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.EstadoOcupacion;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.Ocupacion;
import ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.client.dto.OcupacionCatedraResponse;

@Component
public class OcupacionClientMapper {

    public Ocupacion toDomainModel(OcupacionCatedraResponse response) {
        if (response == null) {
            return null;
        }
        return Ocupacion.builder()
                .fecha(response.getDate())
                .horaInicio(response.getStartTime())
                .estado(toEstado(response.getStatus()))
                .venceEn(response.getExpiresAt())
                .build();
    }

    private EstadoOcupacion toEstado(String status) {
        return switch (status) {
            case "CONFIRMED" -> EstadoOcupacion.CONFIRMADA;
            case "HELD" -> EstadoOcupacion.RETENIDA;
            default -> throw new IllegalArgumentException("Estado de ocupación desconocido: " + status);
        };
    }
}
