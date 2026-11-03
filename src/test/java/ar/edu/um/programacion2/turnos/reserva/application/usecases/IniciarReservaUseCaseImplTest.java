package ar.edu.um.programacion2.turnos.reserva.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.CatalogoNoDisponibleException;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.AgendaProfesional;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.HorarioAtencion;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.ports.out.CatalogoClient;
import ar.edu.um.programacion2.turnos.reserva.application.exception.CatedraNoDisponibleException;
import ar.edu.um.programacion2.turnos.reserva.application.exception.CatedraRechazoException;
import ar.edu.um.programacion2.turnos.reserva.application.exception.ProfesionalNoEncontradoException;
import ar.edu.um.programacion2.turnos.reserva.application.exception.ReservaInvalidaException;
import ar.edu.um.programacion2.turnos.reserva.application.exception.ReservaRechazadaException;
import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import ar.edu.um.programacion2.turnos.reserva.domain.model.HoldConfirmado;
import ar.edu.um.programacion2.turnos.reserva.domain.model.HoldCreado;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ProcesoReservaRepository;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ReservasCatedraClient;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.mapper.ProcesoReservaMapper;
import ar.edu.um.programacion2.turnos.usuario.application.exception.UsuarioNotFoundException;
import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class IniciarReservaUseCaseImplTest {

    private static final String LOGIN = "jtejada";
    private static final Long USUARIO_ID = 7L;
    private static final UUID EXTERNAL_PATIENT_ID = UUID.fromString("3f2b8c1e-5d4a-4e7b-9a10-2c6d8e4f1b23");
    private static final Long PROFESIONAL_ID = 201L;
    // Lunes 2/11/2026; "ahora" es el martes 27/10/2026 a las 15:00 en Mendoza.
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 2);
    private static final LocalTime HORA = LocalTime.of(9, 30);
    private static final Instant AHORA = Instant.parse("2026-10-27T18:00:00Z");
    private static final Instant VENCE_HOLD = Instant.parse("2026-10-27T18:10:00Z");
    private static final Instant VENCE_CONFIRMACION = Instant.parse("2026-10-27T18:11:00Z");

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CatalogoClient catalogoClient;

    @Mock
    private ReservasCatedraClient reservasCatedraClient;

    private RepositorioEnMemoria repositorio;
    private IniciarReservaUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        repositorio = new RepositorioEnMemoria();
        Clock clock = Clock.fixed(AHORA, ZoneId.of("America/Argentina/Mendoza"));
        useCase = new IniciarReservaUseCaseImpl(usuarioRepository, catalogoClient, repositorio,
                reservasCatedraClient, clock);
    }

    @Test
    void bloqueaYConfirmaElTurnoGuardandoAntesDeCadaLlamada() {
        prepararUsuarioYAgenda();
        when(reservasCatedraClient.crearHold(PROFESIONAL_ID, FECHA, HORA, EXTERNAL_PATIENT_ID.toString()))
                .thenAnswer(invocacion -> {
                    assertThat(repositorio.estadosGuardados).containsExactly(EstadoProceso.INICIADO);
                    return hold();
                });
        when(reservasCatedraClient.confirmarHold("hold-1", EXTERNAL_PATIENT_ID.toString(), "Joaquin", "Tejada"))
                .thenAnswer(invocacion -> {
                    assertThat(repositorio.estadosGuardados).endsWith(EstadoProceso.BLOQUEADO);
                    return confirmacion();
                });

        ProcesoReserva proceso = useCase.iniciarReserva(LOGIN, PROFESIONAL_ID, FECHA, HORA);

        assertThat(repositorio.estadosGuardados).containsExactly(
                EstadoProceso.INICIADO, EstadoProceso.BLOQUEADO, EstadoProceso.ESPERANDO_TELEFONO);
        assertThat(proceso.getEstado()).isEqualTo(EstadoProceso.ESPERANDO_TELEFONO);
        assertThat(proceso.getUsuarioId()).isEqualTo(USUARIO_ID);
        assertThat(proceso.getHoraFin()).isEqualTo(LocalTime.of(10, 0));
        assertThat(proceso.getProfesionalNombre()).isEqualTo("Ana");
        assertThat(proceso.getProfesionalApellido()).isEqualTo("Gomez");
        assertThat(proceso.getHoldId()).isEqualTo("hold-1");
        assertThat(proceso.getReservationProcessId()).isEqualTo("process-1");
        assertThat(proceso.getExpiresAt()).isEqualTo(VENCE_CONFIRMACION);
        assertThat(proceso.getCreadoEn()).isEqualTo(AHORA);
        assertThat(proceso.getActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    void fallaSiElUsuarioAutenticadoNoExiste() {
        when(usuarioRepository.findByLogin(LOGIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.iniciarReserva(LOGIN, PROFESIONAL_ID, FECHA, HORA))
                .isInstanceOf(UsuarioNotFoundException.class);
        verifyNoInteractions(catalogoClient, reservasCatedraClient);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {"null, Tejada", "Joaquin, null", "' ', Tejada", "J, Tejada"})
    void rechazaSinNombreYApellidoValidosAntesDeConsultarElCatalogo(String nombre, String apellido) {
        when(usuarioRepository.findByLogin(LOGIN)).thenReturn(Optional.of(usuario(nombre, apellido)));

        assertThatThrownBy(() -> useCase.iniciarReserva(LOGIN, PROFESIONAL_ID, FECHA, HORA))
                .isInstanceOf(ReservaInvalidaException.class)
                .hasMessage("Para reservar, tu cuenta tiene que tener nombre y apellido");
        verifyNoInteractions(catalogoClient, reservasCatedraClient);
    }

    @Test
    void rechazaSinCrearElProcesoSiElProfesionalNoExiste() {
        when(usuarioRepository.findByLogin(LOGIN)).thenReturn(Optional.of(usuario("Joaquin", "Tejada")));
        when(catalogoClient.consultarAgenda(PROFESIONAL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.iniciarReserva(LOGIN, PROFESIONAL_ID, FECHA, HORA))
                .isInstanceOf(ProfesionalNoEncontradoException.class);
        assertThat(repositorio.estadosGuardados).isEmpty();
        verifyNoInteractions(reservasCatedraClient);
    }

    @Test
    void rechazaSinCrearElProcesoSiElProfesionalEstaDeshabilitado() {
        when(usuarioRepository.findByLogin(LOGIN)).thenReturn(Optional.of(usuario("Joaquin", "Tejada")));
        AgendaProfesional agenda = agenda();
        agenda.setHabilitado(false);
        when(catalogoClient.consultarAgenda(PROFESIONAL_ID)).thenReturn(Optional.of(agenda));

        assertThatThrownBy(() -> useCase.iniciarReserva(LOGIN, PROFESIONAL_ID, FECHA, HORA))
                .isInstanceOf(ReservaInvalidaException.class)
                .hasMessage("El profesional no está habilitado");
        assertThat(repositorio.estadosGuardados).isEmpty();
    }

    // Horario de los lunes de 9:00 a 12:00 con turnos de 30 minutos.
    @ParameterizedTest(name = "{0} {1}")
    @CsvSource({
            "2026-11-03, 09:30",  // martes: no atiende
            "2026-11-02, 08:30",  // antes del horario
            "2026-11-02, 09:15",  // no arranca en un múltiplo de 30 minutos
            "2026-11-02, 11:45",  // terminaría después de las 12:00
            "2026-11-02, 12:00",  // es la hora de cierre
            "2026-11-02, 09:30:15"})
    void rechazaSinCrearElProcesoSiElTurnoNoSaleDeLaAgenda(LocalDate fecha, LocalTime hora) {
        prepararUsuarioYAgenda();

        assertThatThrownBy(() -> useCase.iniciarReserva(LOGIN, PROFESIONAL_ID, fecha, hora))
                .isInstanceOf(ReservaInvalidaException.class)
                .hasMessage("El turno no corresponde a la agenda del profesional");
        assertThat(repositorio.estadosGuardados).isEmpty();
    }

    @Test
    void ignoraLosHorariosConDuracionInvalida() {
        when(usuarioRepository.findByLogin(LOGIN)).thenReturn(Optional.of(usuario("Joaquin", "Tejada")));
        AgendaProfesional agenda = agenda();
        agenda.getHorarios().getFirst().setDuracionTurnoMinutos(0);
        when(catalogoClient.consultarAgenda(PROFESIONAL_ID)).thenReturn(Optional.of(agenda));

        assertThatThrownBy(() -> useCase.iniciarReserva(LOGIN, PROFESIONAL_ID, FECHA, HORA))
                .isInstanceOf(ReservaInvalidaException.class);
    }

    @Test
    void rechazaUnTurnoQueYaPaso() {
        prepararUsuarioYAgenda();

        assertThatThrownBy(() -> useCase.iniciarReserva(LOGIN, PROFESIONAL_ID, LocalDate.of(2026, 10, 26), HORA))
                .isInstanceOf(ReservaInvalidaException.class)
                .hasMessage("El turno ya pasó");
        assertThat(repositorio.estadosGuardados).isEmpty();
    }

    @Test
    void noIniciaLaReservaSiElCatalogoNoResponde() {
        when(usuarioRepository.findByLogin(LOGIN)).thenReturn(Optional.of(usuario("Joaquin", "Tejada")));
        when(catalogoClient.consultarAgenda(PROFESIONAL_ID))
                .thenThrow(new CatalogoNoDisponibleException("sin respuesta", null));

        assertThatThrownBy(() -> useCase.iniciarReserva(LOGIN, PROFESIONAL_ID, FECHA, HORA))
                .isInstanceOf(CatalogoNoDisponibleException.class);
        assertThat(repositorio.estadosGuardados).isEmpty();
        verifyNoInteractions(reservasCatedraClient);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SLOT_ALREADY_HELD", "SLOT_ALREADY_RESERVED"})
    void fallaSiElTurnoYaNoEstaLibre(String codigo) {
        prepararUsuarioYAgenda();
        when(reservasCatedraClient.crearHold(any(), any(), any(), anyString())).thenThrow(rechazo(codigo));

        assertThatThrownBy(() -> useCase.iniciarReserva(LOGIN, PROFESIONAL_ID, FECHA, HORA))
                .isInstanceOfSatisfying(ReservaRechazadaException.class, e -> {
                    assertThat(e.getProceso().getEstado()).isEqualTo(EstadoProceso.FALLIDO);
                    assertThat(e.getProceso().getMotivo()).isEqualTo(codigo);
                })
                .hasMessage("El turno ya no está libre");
        assertThat(repositorio.estadosGuardados).containsExactly(EstadoProceso.INICIADO, EstadoProceso.FALLIDO);
        verify(reservasCatedraClient, never()).confirmarHold(any(), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PROFESSIONAL_NOT_FOUND", "PROFESSIONAL_DISABLED", "INVALID_SLOT"})
    void fallaSiLaCatedraRechazaElTurno(String codigo) {
        prepararUsuarioYAgenda();
        when(reservasCatedraClient.crearHold(any(), any(), any(), anyString())).thenThrow(rechazo(codigo));

        assertThatThrownBy(() -> useCase.iniciarReserva(LOGIN, PROFESIONAL_ID, FECHA, HORA))
                .isInstanceOfSatisfying(ReservaRechazadaException.class,
                        e -> assertThat(e.getProceso().getMotivo()).isEqualTo(codigo))
                .hasMessage("La cátedra rechazó el turno");
        assertThat(repositorio.estadosGuardados).containsExactly(EstadoProceso.INICIADO, EstadoProceso.FALLIDO);
    }

    @Test
    void unRechazoConCodeDesconocidoDejaElProcesoFallidoYSePropaga() {
        prepararUsuarioYAgenda();
        CatedraRechazoException rechazo = rechazo(null);
        when(reservasCatedraClient.crearHold(any(), any(), any(), anyString())).thenThrow(rechazo);

        assertThatThrownBy(() -> useCase.iniciarReserva(LOGIN, PROFESIONAL_ID, FECHA, HORA)).isSameAs(rechazo);
        assertThat(repositorio.estadosGuardados).containsExactly(EstadoProceso.INICIADO, EstadoProceso.FALLIDO);
        assertThat(repositorio.unico().getMotivo()).isEqualTo("RECHAZO_SIN_CODIGO");
    }

    @Test
    void siCrearElHoldNoTieneRespuestaQuedaIniciadoYNoSeRepite() {
        prepararUsuarioYAgenda();
        when(reservasCatedraClient.crearHold(any(), any(), any(), anyString()))
                .thenThrow(new CatedraNoDisponibleException("timeout", null));

        ProcesoReserva proceso = useCase.iniciarReserva(LOGIN, PROFESIONAL_ID, FECHA, HORA);

        assertThat(proceso.getEstado()).isEqualTo(EstadoProceso.INICIADO);
        assertThat(proceso.getMotivo()).isEqualTo("Sin respuesta de la cátedra al crear el hold");
        assertThat(proceso.getReservationProcessId()).isNull();
        verify(reservasCatedraClient).crearHold(any(), any(), any(), anyString());
        verify(reservasCatedraClient, never()).confirmarHold(any(), any(), any(), any());
    }

    @Test
    void siElHoldVencioAlConfirmarQuedaVencido() {
        prepararHastaElHold();
        when(reservasCatedraClient.confirmarHold(any(), any(), any(), any())).thenThrow(rechazo("HOLD_EXPIRED"));

        assertThatThrownBy(() -> useCase.iniciarReserva(LOGIN, PROFESIONAL_ID, FECHA, HORA))
                .isInstanceOfSatisfying(ReservaRechazadaException.class,
                        e -> assertThat(e.getProceso().getEstado()).isEqualTo(EstadoProceso.VENCIDO))
                .hasMessage("El turno venció antes de confirmarse");
        assertThat(repositorio.estadosGuardados).containsExactly(
                EstadoProceso.INICIADO, EstadoProceso.BLOQUEADO, EstadoProceso.VENCIDO);
    }

    @Test
    void siLaCatedraRechazaLaConfirmacionQuedaFallido() {
        prepararHastaElHold();
        when(reservasCatedraClient.confirmarHold(any(), any(), any(), any())).thenThrow(rechazo("HOLD_NOT_FOUND"));

        assertThatThrownBy(() -> useCase.iniciarReserva(LOGIN, PROFESIONAL_ID, FECHA, HORA))
                .isInstanceOfSatisfying(ReservaRechazadaException.class,
                        e -> assertThat(e.getProceso().getMotivo()).isEqualTo("HOLD_NOT_FOUND"));
        assertThat(repositorio.estadosGuardados).endsWith(EstadoProceso.FALLIDO);
    }

    @Test
    void unaConfirmacionYaHechaSeTomaComoExito() {
        prepararHastaElHold();
        when(reservasCatedraClient.confirmarHold(any(), any(), any(), any()))
                .thenThrow(rechazo("RESERVATION_PROCESS_ALREADY_CONFIRMED"));

        ProcesoReserva proceso = useCase.iniciarReserva(LOGIN, PROFESIONAL_ID, FECHA, HORA);

        assertThat(proceso.getEstado()).isEqualTo(EstadoProceso.ESPERANDO_TELEFONO);
        assertThat(proceso.getExpiresAt()).isEqualTo(VENCE_HOLD);
    }

    @Test
    void siConfirmarNoTieneRespuestaQuedaBloqueado() {
        prepararHastaElHold();
        when(reservasCatedraClient.confirmarHold(any(), any(), any(), any()))
                .thenThrow(new CatedraNoDisponibleException("timeout", null));

        ProcesoReserva proceso = useCase.iniciarReserva(LOGIN, PROFESIONAL_ID, FECHA, HORA);

        assertThat(proceso.getEstado()).isEqualTo(EstadoProceso.BLOQUEADO);
        assertThat(repositorio.estadosGuardados).containsExactly(EstadoProceso.INICIADO, EstadoProceso.BLOQUEADO);
    }

    @Test
    void siElPedidoDeTelefonoLlegoAntesQueLaRespuestaNoRetrocede() {
        prepararHastaElHold();
        when(reservasCatedraClient.confirmarHold(any(), any(), any(), any())).thenAnswer(invocacion -> {
            repositorio.avanzarPorKafka(EstadoProceso.TELEFONO_ENVIADO);
            return confirmacion();
        });

        ProcesoReserva proceso = useCase.iniciarReserva(LOGIN, PROFESIONAL_ID, FECHA, HORA);

        assertThat(proceso.getEstado()).isEqualTo(EstadoProceso.TELEFONO_ENVIADO);
        assertThat(proceso.getRequestEventId()).isEqualTo("request-event-1");
        assertThat(repositorio.estadosGuardados).containsExactly(EstadoProceso.INICIADO, EstadoProceso.BLOQUEADO);
    }

    @Test
    void unRechazoDeLaConfirmacionNoMueveUnProcesoQueYaAvanzoPorKafka() {
        prepararHastaElHold();
        when(reservasCatedraClient.confirmarHold(any(), any(), any(), any())).thenAnswer(invocacion -> {
            repositorio.avanzarPorKafka(EstadoProceso.ESPERANDO_TELEFONO);
            throw rechazo("HOLD_NOT_FOUND");
        });

        ProcesoReserva proceso = useCase.iniciarReserva(LOGIN, PROFESIONAL_ID, FECHA, HORA);

        assertThat(proceso.getEstado()).isEqualTo(EstadoProceso.ESPERANDO_TELEFONO);
        assertThat(repositorio.estadosGuardados).containsExactly(EstadoProceso.INICIADO, EstadoProceso.BLOQUEADO);
    }

    private void prepararUsuarioYAgenda() {
        when(usuarioRepository.findByLogin(LOGIN)).thenReturn(Optional.of(usuario("Joaquin", "Tejada")));
        when(catalogoClient.consultarAgenda(PROFESIONAL_ID)).thenReturn(Optional.of(agenda()));
    }

    private void prepararHastaElHold() {
        prepararUsuarioYAgenda();
        when(reservasCatedraClient.crearHold(any(), any(), any(), anyString())).thenReturn(hold());
    }

    private static Usuario usuario(String nombre, String apellido) {
        return Usuario.builder()
                .id(USUARIO_ID)
                .login(LOGIN)
                .firstName(nombre)
                .lastName(apellido)
                .externalPatientId(EXTERNAL_PATIENT_ID)
                .build();
    }

    private static AgendaProfesional agenda() {
        return AgendaProfesional.builder()
                .profesionalId(PROFESIONAL_ID)
                .nombre("Ana")
                .apellido("Gomez")
                .habilitado(true)
                .horarios(new ArrayList<>(List.of(HorarioAtencion.builder()
                        .diaSemana(DayOfWeek.MONDAY)
                        .horaInicio(LocalTime.of(9, 0))
                        .horaFin(LocalTime.of(12, 0))
                        .duracionTurnoMinutos(30)
                        .build())))
                .build();
    }

    private static HoldCreado hold() {
        return HoldCreado.builder().holdId("hold-1").reservationProcessId("process-1").venceEn(VENCE_HOLD).build();
    }

    private static HoldConfirmado confirmacion() {
        return HoldConfirmado.builder().reservationProcessId("process-1").venceEn(VENCE_CONFIRMACION).build();
    }

    private static CatedraRechazoException rechazo(String codigo) {
        return new CatedraRechazoException("rechazo", codigo, null);
    }

    // Guarda copias, como la base: el objeto que tiene el caso de uso no cambia si otro lo modifica.
    private static class RepositorioEnMemoria implements ProcesoReservaRepository {

        private static final ProcesoReservaMapper MAPPER = new ProcesoReservaMapper();

        private final Map<Long, ProcesoReserva> procesos = new HashMap<>();
        private final List<EstadoProceso> estadosGuardados = new ArrayList<>();

        @Override
        public ProcesoReserva save(ProcesoReserva proceso) {
            if (proceso.getId() == null) {
                proceso.setId((long) procesos.size() + 1);
            }
            estadosGuardados.add(proceso.getEstado());
            procesos.put(proceso.getId(), copiar(proceso));
            return copiar(proceso);
        }

        @Override
        public Optional<ProcesoReserva> findById(Long id) {
            return Optional.ofNullable(procesos.get(id)).map(RepositorioEnMemoria::copiar);
        }

        @Override
        public Optional<ProcesoReserva> findByReservationProcessId(String reservationProcessId) {
            return procesos.values().stream()
                    .filter(proceso -> reservationProcessId.equals(proceso.getReservationProcessId()))
                    .findFirst()
                    .map(RepositorioEnMemoria::copiar);
        }

        // Simula al consumidor de Kafka guardando el pedido de teléfono por su cuenta.
        void avanzarPorKafka(EstadoProceso estado) {
            ProcesoReserva proceso = unico();
            proceso.setEstado(estado);
            proceso.setRequestEventId("request-event-1");
        }

        ProcesoReserva unico() {
            return procesos.values().iterator().next();
        }

        private static ProcesoReserva copiar(ProcesoReserva proceso) {
            return MAPPER.toDomainModel(MAPPER.toEntity(proceso));
        }
    }
}
