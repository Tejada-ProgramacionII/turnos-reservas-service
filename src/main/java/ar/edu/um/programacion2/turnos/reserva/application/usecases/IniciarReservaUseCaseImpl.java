package ar.edu.um.programacion2.turnos.reserva.application.usecases;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

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
import ar.edu.um.programacion2.turnos.reserva.domain.ports.in.IniciarReservaUseCase;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ProcesoReservaRepository;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ReservasCatedraClient;
import ar.edu.um.programacion2.turnos.usuario.application.exception.UsuarioNotFoundException;
import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IniciarReservaUseCaseImpl implements IniciarReservaUseCase {

    private static final Set<String> TURNO_OCUPADO = Set.of("SLOT_ALREADY_HELD", "SLOT_ALREADY_RESERVED");
    private static final Set<String> TURNO_RECHAZADO =
            Set.of("PROFESSIONAL_NOT_FOUND", "PROFESSIONAL_DISABLED", "INVALID_SLOT", "HOLD_NOT_FOUND");
    private static final String HOLD_VENCIDO = "HOLD_EXPIRED";
    private static final String YA_CONFIRMADO = "RESERVATION_PROCESS_ALREADY_CONFIRMED";
    private static final String SIN_CODIGO = "RECHAZO_SIN_CODIGO";
    private static final int LARGO_MINIMO_NOMBRE = 2;

    private final UsuarioRepository usuarioRepository;
    private final CatalogoClient catalogoClient;
    private final ProcesoReservaRepository procesoReservaRepository;
    private final ReservasCatedraClient reservasCatedraClient;
    private final Clock clock;

    @Override
    public ProcesoReserva iniciarReserva(String login, Long profesionalId, LocalDate fecha, LocalTime horaInicio) {
        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new UsuarioNotFoundException(login));
        validarPaciente(usuario);
        AgendaProfesional agenda = catalogoClient.consultarAgenda(profesionalId)
                .orElseThrow(() -> new ProfesionalNoEncontradoException(profesionalId));
        LocalTime horaFin = validarTurno(agenda, fecha, horaInicio);

        ProcesoReserva proceso = guardar(ProcesoReserva.builder()
                .usuarioId(usuario.getId())
                .profesionalId(profesionalId)
                .fecha(fecha)
                .horaInicio(horaInicio)
                .horaFin(horaFin)
                .profesionalNombre(agenda.getNombre())
                .profesionalApellido(agenda.getApellido())
                .estado(EstadoProceso.INICIADO)
                .creadoEn(clock.instant())
                .build());

        proceso = bloquear(proceso, usuario);
        if (proceso.getEstado() != EstadoProceso.BLOQUEADO) {
            return proceso;
        }
        return confirmar(proceso, usuario);
    }

    // La cátedra pide nombre y apellido de 2 a 100 caracteres (contrato 10); en el registro son opcionales.
    private void validarPaciente(Usuario usuario) {
        if (esCorto(usuario.getFirstName()) || esCorto(usuario.getLastName())) {
            throw new ReservaInvalidaException("Para reservar, tu cuenta tiene que tener nombre y apellido");
        }
    }

    private static boolean esCorto(String texto) {
        return texto == null || texto.isBlank() || texto.strip().length() < LARGO_MINIMO_NOMBRE;
    }

    private LocalTime validarTurno(AgendaProfesional agenda, LocalDate fecha, LocalTime horaInicio) {
        if (!agenda.isHabilitado()) {
            throw new ReservaInvalidaException("El profesional no está habilitado");
        }
        if (LocalDateTime.of(fecha, horaInicio).isBefore(LocalDateTime.now(clock))) {
            throw new ReservaInvalidaException("El turno ya pasó");
        }
        return agenda.getHorarios().stream()
                .filter(horario -> horario.getDiaSemana() == fecha.getDayOfWeek())
                .map(horario -> finDelTurno(horario, horaInicio))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new ReservaInvalidaException("El turno no corresponde a la agenda del profesional"));
    }

    // El turno es válido si arranca en un múltiplo de la duración desde el inicio del horario y termina dentro de él.
    private static Optional<LocalTime> finDelTurno(HorarioAtencion horario, LocalTime horaInicio) {
        long duracion = Duration.ofMinutes(horario.getDuracionTurnoMinutos()).toSeconds();
        long desdeElInicio = Duration.between(horario.getHoraInicio(), horaInicio).toSeconds();
        LocalTime fin = horaInicio.plusSeconds(duracion);
        if (duracion <= 0 || desdeElInicio < 0 || desdeElInicio % duracion != 0
                || fin.isBefore(horaInicio) || fin.isAfter(horario.getHoraFin())) {
            return Optional.empty();
        }
        return Optional.of(fin);
    }

    private ProcesoReserva bloquear(ProcesoReserva proceso, Usuario usuario) {
        try {
            HoldCreado hold = reservasCatedraClient.crearHold(proceso.getProfesionalId(), proceso.getFecha(),
                    proceso.getHoraInicio(), usuario.getExternalPatientId().toString());
            proceso.setHoldId(hold.getHoldId());
            proceso.setReservationProcessId(hold.getReservationProcessId());
            proceso.setExpiresAt(hold.getVenceEn());
            proceso.pasarA(EstadoProceso.BLOQUEADO);
        } catch (CatedraNoDisponibleException e) {
            // D-17: no se sabe si el hold se creó; no se repite y lo cierra CU-09.
            proceso.setMotivo("Sin respuesta de la cátedra al crear el hold");
        } catch (CatedraRechazoException e) {
            rechazar(proceso, e);
        }
        return guardar(proceso);
    }

    private ProcesoReserva confirmar(ProcesoReserva proceso, Usuario usuario) {
        try {
            HoldConfirmado confirmado = reservasCatedraClient.confirmarHold(proceso.getHoldId(),
                    usuario.getExternalPatientId().toString(), usuario.getFirstName(), usuario.getLastName());
            return pasarAEsperandoTelefono(proceso.getId(), confirmado.getVenceEn());
        } catch (CatedraNoDisponibleException e) {
            // 6d: queda BLOQUEADO; la reconciliación es de CU-09.
            return releer(proceso.getId());
        } catch (CatedraRechazoException e) {
            if (YA_CONFIRMADO.equals(e.getCodigo())) {
                return pasarAEsperandoTelefono(proceso.getId(), null);
            }
            ProcesoReserva actual = releer(proceso.getId());
            if (actual.getEstado() == EstadoProceso.BLOQUEADO) {
                rechazar(actual, e);
            }
            return actual;
        }
    }

    // Si el pedido de teléfono llegó antes por Kafka, el proceso ya avanzó y no se retrocede (CU-05 4b).
    private ProcesoReserva pasarAEsperandoTelefono(Long id, Instant venceEn) {
        ProcesoReserva actual = releer(id);
        if (actual.getEstado() != EstadoProceso.BLOQUEADO) {
            return actual;
        }
        actual.pasarA(EstadoProceso.ESPERANDO_TELEFONO);
        if (venceEn != null) {
            actual.setExpiresAt(venceEn);
        }
        return guardar(actual);
    }

    // Deja el proceso en su estado final, lo guarda y avisa por qué. Un code desconocido es un error de integración.
    private void rechazar(ProcesoReserva proceso, CatedraRechazoException e) {
        String codigo = e.getCodigo() != null ? e.getCodigo() : SIN_CODIGO;
        proceso.setMotivo(codigo);
        proceso.pasarA(HOLD_VENCIDO.equals(codigo) ? EstadoProceso.VENCIDO : EstadoProceso.FALLIDO);
        ProcesoReserva guardado = guardar(proceso);
        if (TURNO_OCUPADO.contains(codigo)) {
            throw new ReservaRechazadaException("El turno ya no está libre", guardado);
        }
        if (HOLD_VENCIDO.equals(codigo)) {
            throw new ReservaRechazadaException("El turno venció antes de confirmarse", guardado);
        }
        if (TURNO_RECHAZADO.contains(codigo)) {
            throw new ReservaRechazadaException("La cátedra rechazó el turno", guardado);
        }
        throw e;
    }

    private ProcesoReserva releer(Long id) {
        return procesoReservaRepository.findById(id).orElseThrow();
    }

    private ProcesoReserva guardar(ProcesoReserva proceso) {
        proceso.setActualizadoEn(clock.instant());
        return procesoReservaRepository.save(proceso);
    }
}
