package ar.edu.um.programacion2.turnos.reserva.application.usecases;

import java.time.Clock;
import java.util.Optional;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.reserva.application.exception.OperacionNoPermitidaException;
import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.in.AbandonarProcesoUseCase;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ProcesoReservaRepository;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AbandonarProcesoUseCaseImpl implements AbandonarProcesoUseCase {

    static final String MOTIVO = "Abandonado por el usuario";

    private final UsuarioRepository usuarioRepository;
    private final ProcesoReservaRepository procesoReservaRepository;
    private final Clock clock;

    // D-13: un proceso ajeno se trata igual que uno inexistente.
    @Override
    public Optional<ProcesoReserva> abandonarProceso(String login, Long id) {
        return usuarioRepository.findByLogin(login)
                .flatMap(usuario -> procesoReservaRepository.findById(id)
                        .filter(proceso -> usuario.getId().equals(proceso.getUsuarioId())))
                .map(this::abandonar);
    }

    // D-16: no se llama a la cátedra, que no cancela un hold sin confirmar; el hold vence solo.
    private ProcesoReserva abandonar(ProcesoReserva proceso) {
        EstadoProceso estado = proceso.getEstado();
        if (estado == EstadoProceso.CONFIRMADO) {
            throw new OperacionNoPermitidaException(
                    "El turno ya está confirmado: usá la cancelación de la reserva (CU-08)", estado);
        }
        if (estado.esFinal()) {
            throw new OperacionNoPermitidaException("El proceso ya terminó", estado);
        }
        proceso.setMotivo(MOTIVO);
        proceso.pasarA(EstadoProceso.CANCELADO);
        proceso.setActualizadoEn(clock.instant());
        return procesoReservaRepository.save(proceso);
    }
}
