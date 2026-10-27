package ar.edu.um.programacion2.turnos.usuario.application.usecases;

import java.util.Optional;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.in.GetUsuarioByLoginUseCase;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GetUsuarioByLoginUseCaseImpl implements GetUsuarioByLoginUseCase {

    private final UsuarioRepository repository;

    @Override
    public Optional<Usuario> getUsuarioByLogin(String login) {
        return repository.findByLogin(login);
    }
}
