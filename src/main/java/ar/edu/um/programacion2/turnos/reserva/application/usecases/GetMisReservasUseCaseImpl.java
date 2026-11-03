package ar.edu.um.programacion2.turnos.reserva.application.usecases;

import java.util.List;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import ar.edu.um.programacion2.turnos.reserva.domain.model.Pagina;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.in.GetMisReservasUseCase;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ProcesoReservaRepository;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GetMisReservasUseCaseImpl implements GetMisReservasUseCase {

    private final UsuarioRepository usuarioRepository;
    private final ProcesoReservaRepository procesoReservaRepository;

    // D-12 y D-13: el usuario sale del token, así que solo se listan sus procesos.
    @Override
    public Pagina<ProcesoReserva> getMisReservas(String login, EstadoProceso estado, int pagina, int tamanio) {
        return usuarioRepository.findByLogin(login)
                .map(usuario -> procesoReservaRepository.findByUsuarioId(usuario.getId(), estado, pagina, tamanio))
                .orElseGet(() -> Pagina.<ProcesoReserva>builder().contenido(List.of()).total(0).build());
    }
}
