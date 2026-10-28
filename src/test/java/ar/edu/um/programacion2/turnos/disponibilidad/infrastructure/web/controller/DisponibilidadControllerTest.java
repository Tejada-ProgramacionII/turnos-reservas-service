package ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.web.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.CatalogoNoDisponibleException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.CatedraNoDisponibleException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.CatedraRechazoException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.FechaPasadaException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.ProfesionalDeshabilitadoException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.ProfesionalNoEncontradoException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.service.DisponibilidadService;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.Disponibilidad;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.TurnoLibre;
import ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.web.mapper.DisponibilidadDtoMapper;

@ExtendWith(MockitoExtension.class)
class DisponibilidadControllerTest {

    private static final String URL = "/api/disponibilidad";
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 9);

    @Mock
    private DisponibilidadService disponibilidadService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DisponibilidadController(disponibilidadService, new DisponibilidadDtoMapper()))
                .build();
    }

    @Test
    void devuelveLosTurnosLibresConLaFechaUnaSolaVez() throws Exception {
        when(disponibilidadService.consultarDisponibilidad(201L, FECHA)).thenReturn(Disponibilidad.builder()
                .profesionalId(201L)
                .fecha(FECHA)
                .turnos(List.of(
                        TurnoLibre.builder().horaInicio(LocalTime.of(12, 0)).horaFin(LocalTime.of(12, 30)).build(),
                        TurnoLibre.builder().horaInicio(LocalTime.of(12, 30)).horaFin(LocalTime.of(13, 0)).build()))
                .build());

        mockMvc.perform(get(URL).param("profesionalId", "201").param("fecha", "2026-11-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profesionalId").value(201))
                .andExpect(jsonPath("$.fecha").value("2026-11-09"))
                .andExpect(jsonPath("$.turnos.length()").value(2))
                .andExpect(jsonPath("$.turnos[0].horaInicio").value("12:00:00"))
                .andExpect(jsonPath("$.turnos[0].horaFin").value("12:30:00"))
                .andExpect(jsonPath("$.turnos[1].horaInicio").value("12:30:00"));
    }

    @Test
    void sinTurnosLibresDevuelveListaVacia() throws Exception {
        when(disponibilidadService.consultarDisponibilidad(201L, FECHA)).thenReturn(Disponibilidad.builder()
                .profesionalId(201L).fecha(FECHA).turnos(List.of()).build());

        mockMvc.perform(get(URL).param("profesionalId", "201").param("fecha", "2026-11-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnos").isEmpty());
    }

    @Test
    void unaFechaPasadaResponde400ConElMotivo() throws Exception {
        LocalDate ayer = LocalDate.of(2026, 11, 1);
        when(disponibilidadService.consultarDisponibilidad(201L, ayer)).thenThrow(new FechaPasadaException(ayer));

        mockMvc.perform(get(URL).param("profesionalId", "201").param("fecha", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La fecha 2026-11-01 ya pasó"));
    }

    @Test
    void unaFechaConFormatoInvalidoResponde400SinConsultarNada() throws Exception {
        mockMvc.perform(get(URL).param("profesionalId", "201").param("fecha", "09/11/2026"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(disponibilidadService);
    }

    @Test
    void sinParametrosResponde400() throws Exception {
        mockMvc.perform(get(URL).param("fecha", "2026-11-09"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(disponibilidadService);
    }

    @Test
    void unProfesionalInexistenteResponde404() throws Exception {
        when(disponibilidadService.consultarDisponibilidad(99999L, FECHA))
                .thenThrow(new ProfesionalNoEncontradoException(99999L));

        mockMvc.perform(get(URL).param("profesionalId", "99999").param("fecha", "2026-11-09"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe el profesional 99999"));
    }

    @Test
    void unProfesionalDeshabilitadoResponde409() throws Exception {
        when(disponibilidadService.consultarDisponibilidad(201L, FECHA))
                .thenThrow(new ProfesionalDeshabilitadoException(201L));

        mockMvc.perform(get(URL).param("profesionalId", "201").param("fecha", "2026-11-09"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El profesional 201 no admite reservas nuevas"));
    }

    @Test
    void sinCatalogoResponde503() throws Exception {
        when(disponibilidadService.consultarDisponibilidad(201L, FECHA))
                .thenThrow(new CatalogoNoDisponibleException("El catálogo respondió con un error (HTTP 503)", null));

        mockMvc.perform(get(URL).param("profesionalId", "201").param("fecha", "2026-11-09"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("La disponibilidad no se puede consultar ahora"));
    }

    @Test
    void sinCatedraResponde503() throws Exception {
        when(disponibilidadService.consultarDisponibilidad(201L, FECHA))
                .thenThrow(new CatedraNoDisponibleException("No se pudo conectar con la cátedra", null));

        mockMvc.perform(get(URL).param("profesionalId", "201").param("fecha", "2026-11-09"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void unRechazoDeLaIntegracionResponde502() throws Exception {
        when(disponibilidadService.consultarDisponibilidad(201L, FECHA))
                .thenThrow(new CatedraRechazoException("La cátedra rechazó el pedido de ocupaciones (HTTP 403)", null));

        mockMvc.perform(get(URL).param("profesionalId", "201").param("fecha", "2026-11-09"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("Error de integración al consultar la disponibilidad"));
    }
}
