package ar.edu.um.programacion2.turnos.reserva.application.usecases;

import java.time.Clock;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import ar.edu.um.programacion2.turnos.reserva.domain.model.EventoProcesado;
import ar.edu.um.programacion2.turnos.reserva.domain.model.EventoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.in.ProcesarEventoReservaUseCase;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.EventoProcesadoRepository;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ProcesoReservaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcesarEventoReservaUseCaseImpl implements ProcesarEventoReservaUseCase {

    private final ProcesoReservaRepository procesoReservaRepository;
    private final EventoProcesadoRepository eventoProcesadoRepository;
    private final Clock clock;

    // El efecto y el registro del eventId se guardan juntos: o quedan los dos o ninguno (D-06).
    @Override
    @Transactional
    public void procesarEvento(EventoReserva evento) {
        if (eventoProcesadoRepository.existsByEventId(evento.getEventId())) {
            log.info("Evento {} ya procesado: se ignora", evento.getEventId());
            return;
        }
        procesoReservaRepository.findByReservationProcessId(evento.getReservationProcessId()).ifPresentOrElse(
                proceso -> aplicar(proceso, evento),
                () -> log.info("Evento {} ({}) de un proceso que no es de esta aplicación: {}",
                        evento.getEventId(), evento.getTipo(), evento.getReservationProcessId()));
        eventoProcesadoRepository.save(EventoProcesado.builder()
                .eventId(evento.getEventId())
                .procesadoEn(clock.instant())
                .build());
    }

    private void aplicar(ProcesoReserva proceso, EventoReserva evento) {
        EstadoProceso destino = evento.getTipo().getDestino();
        boolean mismoEstado = proceso.getEstado() == destino;
        // Llegar a ESPERANDO_TELEFONO estando ahí solo actualiza los datos del pedido (CU-05 4b);
        // cualquier otro resultado repetido o transición no permitida se ignora (D-05, CU-06 4a y 4b).
        boolean ignorar = mismoEstado
                ? destino != EstadoProceso.ESPERANDO_TELEFONO
                : !proceso.getEstado().puedePasarA(destino);
        if (ignorar) {
            log.info("Evento {} ({}) ignorado: el proceso {} está en {}",
                    evento.getEventId(), evento.getTipo(), proceso.getId(), proceso.getEstado());
            return;
        }
        guardarDatos(proceso, evento);
        if (!mismoEstado) {
            proceso.pasarA(destino);
        }
        proceso.setActualizadoEn(clock.instant());
        procesoReservaRepository.save(proceso);
        log.info("Evento {} ({}) aplicado: el proceso {} quedó en {}",
                evento.getEventId(), evento.getTipo(), proceso.getId(), proceso.getEstado());
    }

    private static void guardarDatos(ProcesoReserva proceso, EventoReserva evento) {
        switch (evento.getTipo()) {
            case INFORMACION_SOLICITADA -> {
                proceso.setRequestEventId(evento.getEventId());
                proceso.setMensajePedido(evento.getMensaje());
                proceso.setExpiresAt(evento.getExpiresAt());
            }
            case INFORMACION_RECHAZADA -> {
                proceso.setMotivo(evento.getMotivo());
                proceso.setMensajePedido(evento.getMensaje());
                proceso.setExpiresAt(evento.getExpiresAt());
            }
            case TURNO_CONFIRMADO -> {
                proceso.setReservationId(evento.getReservationId());
                proceso.setMotivo(null);
            }
            case PROCESO_VENCIDO, PROCESO_INVALIDO, TURNO_CANCELADO -> proceso.setMotivo(evento.getMotivo());
        }
    }
}
