package ar.edu.um.programacion2.turnos.reserva.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import ar.edu.um.programacion2.turnos.reserva.domain.model.Pagina;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ProcesoReservaRepository;
import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class GetMisReservasUseCaseImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProcesoReservaRepository procesoReservaRepository;

    @InjectMocks
    private GetMisReservasUseCaseImpl useCase;

    @Test
    void buscaLosProcesosDelUsuarioDelToken() {
        Pagina<ProcesoReserva> pagina = Pagina.<ProcesoReserva>builder()
                .contenido(List.of(ProcesoReserva.builder().id(1L).usuarioId(7L).build()))
                .total(1)
                .build();
        when(usuarioRepository.findByLogin("jtejada")).thenReturn(Optional.of(Usuario.builder().id(7L).build()));
        when(procesoReservaRepository.findByUsuarioId(7L, EstadoProceso.CONFIRMADO, 0, 20)).thenReturn(pagina);

        assertThat(useCase.getMisReservas("jtejada", EstadoProceso.CONFIRMADO, 0, 20)).isSameAs(pagina);
    }

    @Test
    void devuelveUnaPaginaVaciaSiElUsuarioNoExiste() {
        when(usuarioRepository.findByLogin("fantasma")).thenReturn(Optional.empty());

        Pagina<ProcesoReserva> pagina = useCase.getMisReservas("fantasma", null, 0, 20);

        assertThat(pagina.getContenido()).isEmpty();
        assertThat(pagina.getTotal()).isZero();
        verifyNoInteractions(procesoReservaRepository);
    }
}
