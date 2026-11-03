package ar.edu.um.programacion2.turnos.reserva.application.usecases;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.reserva.application.exception.MensajeriaNoDisponibleException;
import ar.edu.um.programacion2.turnos.reserva.application.exception.OperacionNoPermitidaException;
import ar.edu.um.programacion2.turnos.reserva.application.exception.TelefonoInvalidoException;
import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.model.Telefono;
import ar.edu.um.programacion2.turnos.reserva.domain.model.TelefonoInformado;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.in.InformarTelefonoUseCase;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ProcesoReservaRepository;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.TelefonoEventPublisher;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InformarTelefonoUseCaseImpl implements InformarTelefonoUseCase {

    private final UsuarioRepository usuarioRepository;
    private final ProcesoReservaRepository procesoReservaRepository;
    private final TelefonoEventPublisher telefonoEventPublisher;
    private final Clock clock;

    // D-13: un proceso ajeno se trata igual que uno inexistente.
    @Override
    public Optional<ProcesoReserva> informarTelefono(String login, Long id, String telefono) {
        return usuarioRepository.findByLogin(login)
                .flatMap(usuario -> procesoReservaRepository.findById(id)
                        .filter(proceso -> usuario.getId().equals(proceso.getUsuarioId())))
                .map(proceso -> enviar(proceso, telefono));
    }

    private ProcesoReserva enviar(ProcesoReserva proceso, String telefono) {
        validarEstado(proceso);
        String normalizado = Telefono.normalizar(telefono)
                .orElseThrow(() -> new TelefonoInvalidoException("El teléfono tiene que tener " + Telefono.FORMATO));

        // Se guarda TELEFONO_ENVIADO antes de publicar: si la respuesta de la cátedra llega por Kafka
        // antes de que esto termine, el consumidor ya encuentra el proceso en el estado que la espera.
        String telefonoAnterior = proceso.getTelefono();
        String motivoAnterior = proceso.getMotivo();
        proceso.setTelefono(normalizado);
        proceso.setMotivo(null);
        proceso.pasarA(EstadoProceso.TELEFONO_ENVIADO);
        ProcesoReserva enviado = guardar(proceso);
        try {
            telefonoEventPublisher.publicar(TelefonoInformado.builder()
                    .eventId(UUID.randomUUID().toString())
                    .reservationProcessId(enviado.getReservationProcessId())
                    .holdId(enviado.getHoldId())
                    .requestEventId(enviado.getRequestEventId())
                    .telefono(normalizado)
                    .occurredAt(clock.instant())
                    .build());
            return enviado;
        } catch (MensajeriaNoDisponibleException e) {
            revertir(enviado.getId(), telefonoAnterior, motivoAnterior);
            throw e;
        }
    }

    private void validarEstado(ProcesoReserva proceso) {
        EstadoProceso estado = proceso.getEstado();
        boolean pedidoPendiente = estado == EstadoProceso.INICIADO || estado == EstadoProceso.BLOQUEADO
                || (estado == EstadoProceso.ESPERANDO_TELEFONO && proceso.getRequestEventId() == null);
        if (pedidoPendiente) {
            throw new OperacionNoPermitidaException(
                    "Todavía no se puede informar el teléfono: la cátedra no lo pidió", estado);
        }
        if (estado != EstadoProceso.ESPERANDO_TELEFONO) {
            throw new OperacionNoPermitidaException("El proceso no está esperando el teléfono", estado);
        }
        Instant venceEn = proceso.getExpiresAt();
        if (venceEn != null && !venceEn.isAfter(clock.instant())) {
            throw new OperacionNoPermitidaException("Venció el plazo para informar el teléfono", estado);
        }
    }

    // Si mientras tanto llegó otro resultado por Kafka, el proceso ya no está en TELEFONO_ENVIADO y no se toca.
    private void revertir(Long id, String telefonoAnterior, String motivoAnterior) {
        procesoReservaRepository.findById(id)
                .filter(actual -> actual.getEstado() == EstadoProceso.TELEFONO_ENVIADO)
                .ifPresent(actual -> {
                    actual.setTelefono(telefonoAnterior);
                    actual.setMotivo(motivoAnterior);
                    actual.pasarA(EstadoProceso.ESPERANDO_TELEFONO);
                    guardar(actual);
                });
    }

    private ProcesoReserva guardar(ProcesoReserva proceso) {
        proceso.setActualizadoEn(clock.instant());
        return procesoReservaRepository.save(proceso);
    }
}
