package ar.edu.um.programacion2.turnos.reserva.application.usecases;

import java.util.Optional;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.in.GetProcesoReservaUseCase;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ProcesoReservaRepository;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GetProcesoReservaUseCaseImpl implements GetProcesoReservaUseCase {

    private final UsuarioRepository usuarioRepository;
    private final ProcesoReservaRepository procesoReservaRepository;

    // D-13: un proceso ajeno se trata igual que uno inexistente.
    @Override
    public Optional<ProcesoReserva> getProcesoReserva(String login, Long id) {
        return usuarioRepository.findByLogin(login)
                .flatMap(usuario -> procesoReservaRepository.findById(id)
                        .filter(proceso -> usuario.getId().equals(proceso.getUsuarioId())));
    }
}
