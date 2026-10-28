package ar.edu.um.programacion2.turnos.disponibilidad.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.programacion2.turnos.config.DisponibilidadProperties;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.CatalogoNoDisponibleException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.CatalogoRechazoException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.CatedraNoDisponibleException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.ProfesionalNoEncontradoException;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.Disponibilidad;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.ports.in.ConsultarDisponibilidadUseCase;

@ExtendWith(MockitoExtension.class)
class DisponibilidadServiceTest {

    private static final Long PROFESIONAL_ID = 201L;
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 9);
    private static final CatalogoNoDisponibleException CATALOGO_NO_DISPONIBLE =
            new CatalogoNoDisponibleException("El catálogo respondió con un error (HTTP 503)", null);
    private static final CatedraNoDisponibleException CATEDRA_NO_DISPONIBLE =
            new CatedraNoDisponibleException("No se pudo conectar con la cátedra", null);

    @Mock
    private ConsultarDisponibilidadUseCase consultarDisponibilidadUseCase;

    private DisponibilidadProperties disponibilidadProperties;

    private DisponibilidadService service;

    @BeforeEach
    void setUp() {
        disponibilidadProperties = new DisponibilidadProperties();
        disponibilidadProperties.setMaxIntentos(3);
        disponibilidadProperties.setEsperaEntreIntentos(Duration.ZERO);
        service = new DisponibilidadService(consultarDisponibilidadUseCase, disponibilidadProperties);
    }

    @Test
    void devuelveLaDisponibilidadDelCasoDeUso() {
        Disponibilidad disponibilidad = Disponibilidad.builder()
                .profesionalId(PROFESIONAL_ID).fecha(FECHA).turnos(List.of()).build();
        when(consultarDisponibilidadUseCase.consultarDisponibilidad(PROFESIONAL_ID, FECHA))
                .thenReturn(Optional.of(disponibilidad));

        assertThat(service.consultarDisponibilidad(PROFESIONAL_ID, FECHA)).isSameAs(disponibilidad);
    }

    @Test
    void siElProfesionalNoExisteLanzaExcepcion() {
        when(consultarDisponibilidadUseCase.consultarDisponibilidad(PROFESIONAL_ID, FECHA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consultarDisponibilidad(PROFESIONAL_ID, FECHA))
                .isInstanceOf(ProfesionalNoEncontradoException.class)
                .hasMessage("No existe el profesional 201");
    }

    @Test
    void reintentaHastaQueElCatalogoYLaCatedraResponden() {
        Disponibilidad disponibilidad = Disponibilidad.builder().profesionalId(PROFESIONAL_ID).build();
        when(consultarDisponibilidadUseCase.consultarDisponibilidad(PROFESIONAL_ID, FECHA))
                .thenThrow(CATALOGO_NO_DISPONIBLE)
                .thenThrow(CATEDRA_NO_DISPONIBLE)
                .thenReturn(Optional.of(disponibilidad));

        assertThat(service.consultarDisponibilidad(PROFESIONAL_ID, FECHA)).isSameAs(disponibilidad);
        verify(consultarDisponibilidadUseCase, times(3)).consultarDisponibilidad(PROFESIONAL_ID, FECHA);
    }

    @Test
    void siSigueSinRespuestaCortaEnElMaximoDeIntentosYPropaga() {
        when(consultarDisponibilidadUseCase.consultarDisponibilidad(PROFESIONAL_ID, FECHA))
                .thenThrow(CATEDRA_NO_DISPONIBLE);

        assertThatThrownBy(() -> service.consultarDisponibilidad(PROFESIONAL_ID, FECHA))
                .isSameAs(CATEDRA_NO_DISPONIBLE);
        verify(consultarDisponibilidadUseCase, times(3)).consultarDisponibilidad(PROFESIONAL_ID, FECHA);
    }

    @Test
    void unRechazoNoSeReintenta() {
        CatalogoRechazoException rechazo = new CatalogoRechazoException("El catálogo rechazó la consulta (HTTP 401)", null);
        when(consultarDisponibilidadUseCase.consultarDisponibilidad(PROFESIONAL_ID, FECHA)).thenThrow(rechazo);

        assertThatThrownBy(() -> service.consultarDisponibilidad(PROFESIONAL_ID, FECHA)).isSameAs(rechazo);
        verify(consultarDisponibilidadUseCase).consultarDisponibilidad(PROFESIONAL_ID, FECHA);
    }

    @Test
    void siSeInterrumpeLaEsperaCortaYConservaLaMarca() {
        disponibilidadProperties.setEsperaEntreIntentos(Duration.ofSeconds(1));
        when(consultarDisponibilidadUseCase.consultarDisponibilidad(PROFESIONAL_ID, FECHA))
                .thenThrow(CATALOGO_NO_DISPONIBLE);

        boolean marcaPuesta;
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> service.consultarDisponibilidad(PROFESIONAL_ID, FECHA))
                    .isSameAs(CATALOGO_NO_DISPONIBLE);
        } finally {
            marcaPuesta = Thread.interrupted();
        }

        assertThat(marcaPuesta).isTrue();
        verify(consultarDisponibilidadUseCase).consultarDisponibilidad(PROFESIONAL_ID, FECHA);
    }
}
