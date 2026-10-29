package ar.edu.um.programacion2.turnos.reserva.infrastructure.web.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.CatalogoNoDisponibleException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.CatalogoRechazoException;
import ar.edu.um.programacion2.turnos.reserva.application.exception.CatedraRechazoException;
import ar.edu.um.programacion2.turnos.reserva.application.exception.ProcesoReservaNoEncontradoException;
import ar.edu.um.programacion2.turnos.reserva.application.exception.ProfesionalNoEncontradoException;
import ar.edu.um.programacion2.turnos.reserva.application.exception.ReservaInvalidaException;
import ar.edu.um.programacion2.turnos.reserva.application.exception.ReservaRechazadaException;
import ar.edu.um.programacion2.turnos.reserva.application.service.ReservaService;
import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.web.mapper.ProcesoReservaDtoMapper;

@ExtendWith(MockitoExtension.class)
class ReservaControllerTest {

    private static final String URL = "/api/reservas";
    private static final String LOGIN = "jtejada";
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 2);
    private static final LocalTime HORA = LocalTime.of(9, 30);
    private static final String CUERPO = """
            {"profesionalId": 201, "fecha": "2026-11-02", "horaInicio": "09:30"}
            """;

    @Mock
    private ReservaService reservaService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS512").subject(LOGIN).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ReservaController(reservaService, new ProcesoReservaDtoMapper()))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void iniciarReservaDevuelve201ConElProcesoEsperandoElTelefono() throws Exception {
        when(reservaService.iniciarReserva(LOGIN, 201L, FECHA, HORA))
                .thenReturn(proceso(EstadoProceso.ESPERANDO_TELEFONO));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/reservas/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("ESPERANDO_TELEFONO"))
                .andExpect(jsonPath("$.horaFin").value("10:00:00"))
                .andExpect(jsonPath("$.expiresAt").value("2026-10-27T18:10:00Z"))
                .andExpect(jsonPath("$.holdId").doesNotExist())
                .andExpect(jsonPath("$.usuarioId").doesNotExist());
    }

    @ParameterizedTest
    @EnumSource(names = {"INICIADO", "BLOQUEADO"})
    void iniciarReservaDevuelve202SiElResultadoQuedaPendiente(EstadoProceso estado) throws Exception {
        when(reservaService.iniciarReserva(LOGIN, 201L, FECHA, HORA)).thenReturn(proceso(estado));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.estado").value(estado.name()));
    }

    @Test
    void iniciarReservaSinCamposObligatoriosDevuelve400() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"profesionalId\": 201}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reservaService);
    }

    @Test
    void iniciarReservaConUnTurnoInvalidoDevuelve400() throws Exception {
        when(reservaService.iniciarReserva(LOGIN, 201L, FECHA, HORA))
                .thenThrow(new ReservaInvalidaException("El turno no corresponde a la agenda del profesional"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El turno no corresponde a la agenda del profesional"));
    }

    @Test
    void iniciarReservaConUnProfesionalInexistenteDevuelve404() throws Exception {
        when(reservaService.iniciarReserva(LOGIN, 201L, FECHA, HORA))
                .thenThrow(new ProfesionalNoEncontradoException(201L));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isNotFound());
    }

    @Test
    void iniciarReservaRechazadaPorLaCatedraDevuelve409ConElProceso() throws Exception {
        when(reservaService.iniciarReserva(LOGIN, 201L, FECHA, HORA))
                .thenThrow(new ReservaRechazadaException("El turno ya no está libre", proceso(EstadoProceso.FALLIDO)));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El turno ya no está libre"))
                .andExpect(jsonPath("$.procesoId").value(1))
                .andExpect(jsonPath("$.estado").value("FALLIDO"));
    }

    @Test
    void iniciarReservaSinCatalogoDevuelve503() throws Exception {
        when(reservaService.iniciarReserva(LOGIN, 201L, FECHA, HORA))
                .thenThrow(new CatalogoNoDisponibleException("sin respuesta", null));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("La reserva no se puede iniciar ahora"));
    }

    @Test
    void iniciarReservaConUnErrorDeIntegracionDelCatalogoDevuelve502() throws Exception {
        when(reservaService.iniciarReserva(LOGIN, 201L, FECHA, HORA))
                .thenThrow(new CatalogoRechazoException("HTTP 401", null));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("Error de integración al iniciar la reserva"));
    }

    @Test
    void iniciarReservaConUnErrorDeIntegracionDeLaCatedraDevuelve502() throws Exception {
        when(reservaService.iniciarReserva(LOGIN, 201L, FECHA, HORA))
                .thenThrow(new CatedraRechazoException("HTTP 400", "VALIDATION_ERROR", null));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isBadGateway());
    }

    @Test
    void getProcesoReservaDevuelveElProcesoDelUsuario() throws Exception {
        when(reservaService.getProcesoReserva(LOGIN, 1L)).thenReturn(proceso(EstadoProceso.ESPERANDO_TELEFONO));

        mockMvc.perform(get(URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("ESPERANDO_TELEFONO"));
    }

    @Test
    void getProcesoReservaAjenoOInexistenteDevuelve404() throws Exception {
        when(reservaService.getProcesoReserva(LOGIN, 2L)).thenThrow(new ProcesoReservaNoEncontradoException(2L));

        mockMvc.perform(get(URL + "/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No se encontró el proceso de reserva con id: 2"));
    }

    private static ProcesoReserva proceso(EstadoProceso estado) {
        return ProcesoReserva.builder()
                .id(1L)
                .usuarioId(7L)
                .profesionalId(201L)
                .fecha(FECHA)
                .horaInicio(HORA)
                .horaFin(LocalTime.of(10, 0))
                .holdId("hold-1")
                .expiresAt(Instant.parse("2026-10-27T18:10:00Z"))
                .estado(estado)
                .build();
    }
}
