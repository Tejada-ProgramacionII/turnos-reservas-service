package ar.edu.um.programacion2.turnos.reserva.application.usecases;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.reserva.application.exception.CatedraRechazoException;
import ar.edu.um.programacion2.turnos.reserva.application.exception.OperacionNoPermitidaException;
import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ReservaCancelada;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.in.CancelarReservaUseCase;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ProcesoReservaRepository;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ReservasCatedraClient;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CancelarReservaUseCaseImpl implements CancelarReservaUseCase {

    static final String MOTIVO_POR_DEFECTO = "Cancelada por el usuario";

    private final UsuarioRepository usuarioRepository;
    private final ProcesoReservaRepository procesoReservaRepository;
    private final ReservasCatedraClient reservasCatedraClient;
    private final Clock clock;

    // D-13: un proceso ajeno se trata igual que uno inexistente.
    @Override
    public Optional<ProcesoReserva> cancelarReserva(String login, Long id, String motivo) {
        return usuarioRepository.findByLogin(login)
                .flatMap(usuario -> procesoReservaRepository.findById(id)
                        .filter(proceso -> usuario.getId().equals(proceso.getUsuarioId())))
                .map(proceso -> cancelar(proceso, motivo));
    }

    // Si la cátedra no responde, la excepción sube y el proceso queda CONFIRMADO con el pedido guardado (CU-09).
    private ProcesoReserva cancelar(ProcesoReserva proceso, String motivo) {
        validar(proceso.getEstado());
        String motivoAnterior = proceso.getMotivo();
        String motivoPedido = motivo == null || motivo.isBlank() ? MOTIVO_POR_DEFECTO : motivo.strip();
        // D-04: queda registrado qué se pidió antes de llamar a la cátedra.
        proceso.setMotivo(motivoPedido);
        guardar(proceso);

        ReservaCancelada cancelada;
        try {
            cancelada = reservasCatedraClient.cancelar(proceso.getReservationProcessId(), motivoPedido);
        } catch (CatedraRechazoException e) {
            restaurarMotivo(proceso.getId(), motivoAnterior);
            throw e;
        }
        return marcarCancelado(proceso.getId(), cancelada != null ? cancelada.getCanceladaEn() : null);
    }

    private static void validar(EstadoProceso estado) {
        if (estado.esCancelable()) {
            return;
        }
        if (estado.esAbandonable()) {
            throw new OperacionNoPermitidaException(
                    "La reserva todavía no está confirmada: usá abandonar el proceso (CU-10)", estado);
        }
        if (!estado.esFinal()) {
            throw new OperacionNoPermitidaException(
                    "La cátedra todavía está confirmando la reserva: esperá el resultado", estado);
        }
        throw new OperacionNoPermitidaException("Solo se puede cancelar una reserva confirmada", estado);
    }

    // La cátedra rechazó: el proceso no cambia.
    private void restaurarMotivo(Long id, String motivoAnterior) {
        ProcesoReserva actual = releer(id);
        if (actual.getEstado() == EstadoProceso.CONFIRMADO) {
            actual.setMotivo(motivoAnterior);
            guardar(actual);
        }
    }

    // Se relee porque AppointmentCancelled pudo llegar por Kafka mientras tanto; un CANCELADO no se toca (D-05).
    private ProcesoReserva marcarCancelado(Long id, Instant canceladaEn) {
        ProcesoReserva actual = releer(id);
        if (actual.getEstado() == EstadoProceso.CONFIRMADO) {
            actual.pasarA(EstadoProceso.CANCELADO);
        }
        if (actual.getCanceladaEn() == null) {
            actual.setCanceladaEn(canceladaEn != null ? canceladaEn : clock.instant());
        }
        return guardar(actual);
    }

    private ProcesoReserva releer(Long id) {
        return procesoReservaRepository.findById(id).orElseThrow();
    }

    private ProcesoReserva guardar(ProcesoReserva proceso) {
        proceso.setActualizadoEn(clock.instant());
        return procesoReservaRepository.save(proceso);
    }
}
