package ar.edu.um.programacion2.turnos.usuario.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class GetUsuarioByLoginUseCaseImplTest {

    @Mock
    private UsuarioRepository repository;

    @InjectMocks
    private GetUsuarioByLoginUseCaseImpl useCase;

    @Test
    void devuelveElUsuarioCuandoExiste() {
        Usuario usuario = Usuario.builder().login("jtejada").build();
        when(repository.findByLogin("jtejada")).thenReturn(Optional.of(usuario));

        assertThat(useCase.getUsuarioByLogin("jtejada")).contains(usuario);
    }

    @Test
    void devuelveVacioCuandoNoExiste() {
        when(repository.findByLogin("nadie")).thenReturn(Optional.empty());

        assertThat(useCase.getUsuarioByLogin("nadie")).isEmpty();
    }
}
