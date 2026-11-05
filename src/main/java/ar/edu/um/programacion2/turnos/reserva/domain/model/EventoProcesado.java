package ar.edu.um.programacion2.turnos.reserva.domain.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoProcesado {
    private String eventId;
    private Instant procesadoEn;
}
