package ar.edu.um.programacion2.turnos.reserva.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// Eventos del topic de acciones de turnos (contrato 15.4 y 15.6 a 15.10) y el estado al que llevan al proceso.
@Getter
@RequiredArgsConstructor
public enum TipoEventoReserva {
    INFORMACION_SOLICITADA("AdditionalInformationRequested", EstadoProceso.ESPERANDO_TELEFONO),
    TURNO_CONFIRMADO("AppointmentConfirmed", EstadoProceso.CONFIRMADO),
    INFORMACION_RECHAZADA("AdditionalInformationRejected", EstadoProceso.ESPERANDO_TELEFONO),
    PROCESO_VENCIDO("AppointmentProcessExpired", EstadoProceso.VENCIDO),
    PROCESO_INVALIDO("AppointmentProcessInvalid", EstadoProceso.INVALIDO),
    TURNO_CANCELADO("AppointmentCancelled", EstadoProceso.CANCELADO);

    private final String eventType;
    private final EstadoProceso destino;
}
