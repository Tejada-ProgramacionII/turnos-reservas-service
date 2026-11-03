package ar.edu.um.programacion2.turnos.reserva.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ProcesoReservaRepository;
import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class GetProcesoReservaUseCaseImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProcesoReservaRepository procesoReservaRepository;

    @InjectMocks
    private GetProcesoReservaUseCaseImpl useCase;

    @Test
    void devuelveElProcesoSiEsDelUsuario() {
        ProcesoReserva proceso = ProcesoReserva.builder().id(1L).usuarioId(7L).build();
        when(usuarioRepository.findByLogin("jtejada")).thenReturn(Optional.of(Usuario.builder().id(7L).build()));
        when(procesoReservaRepository.findById(1L)).thenReturn(Optional.of(proceso));

        assertThat(useCase.getProcesoReserva("jtejada", 1L)).contains(proceso);
    }

    @Test
    void devuelveVacioSiElProcesoEsDeOtroUsuario() {
        when(usuarioRepository.findByLogin("jtejada")).thenReturn(Optional.of(Usuario.builder().id(7L).build()));
        when(procesoReservaRepository.findById(1L))
                .thenReturn(Optional.of(ProcesoReserva.builder().id(1L).usuarioId(8L).build()));

        assertThat(useCase.getProcesoReserva("jtejada", 1L)).isEmpty();
    }

    @Test
    void devuelveVacioSiElProcesoNoExiste() {
        when(usuarioRepository.findByLogin("jtejada")).thenReturn(Optional.of(Usuario.builder().id(7L).build()));
        when(procesoReservaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(useCase.getProcesoReserva("jtejada", 99L)).isEmpty();
    }

    @Test
    void devuelveVacioSiElUsuarioNoExiste() {
        when(usuarioRepository.findByLogin("fantasma")).thenReturn(Optional.empty());

        assertThat(useCase.getProcesoReserva("fantasma", 1L)).isEmpty();
        verifyNoInteractions(procesoReservaRepository);
    }
}
