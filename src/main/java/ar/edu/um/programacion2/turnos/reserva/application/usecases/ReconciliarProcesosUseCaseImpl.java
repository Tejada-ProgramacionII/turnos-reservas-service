package ar.edu.um.programacion2.turnos.reserva.application.usecases;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.reserva.application.exception.CatedraNoDisponibleException;
import ar.edu.um.programacion2.turnos.reserva.application.exception.CatedraRechazoException;
import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoReservaCatedra;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ReservaCatedra;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ResultadoReconciliacion;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.in.ReconciliarProcesosUseCase;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ProcesoReservaRepository;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ReservasCatedraClient;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliarProcesosUseCaseImpl implements ReconciliarProcesosUseCase {

    static final Duration VIGENCIA_MAXIMA_HOLD = Duration.ofMinutes(10);
    static final String MOTIVO_INCIERTO = "Resultado incierto al crear el hold";
    static final String MOTIVO_CONFIRMADA = "Reconciliación: confirmada en la cátedra";
    static final String MOTIVO_CANCELADA = "Reconciliación: cancelada en la cátedra";
    static final String MOTIVO_FALLIDA = "Reconciliación: la cátedra informa FAILED";
    static final String MOTIVO_VENCIDA = "Reconciliación: venció sin confirmarse en la cátedra";
    static final String MOTIVO_TELEFONO = "Reconciliación: la cátedra espera el teléfono";

    private static final Set<EstadoProceso> PENDIENTES = EnumSet.of(EstadoProceso.INICIADO, EstadoProceso.BLOQUEADO,
            EstadoProceso.ESPERANDO_TELEFONO, EstadoProceso.TELEFONO_ENVIADO);

    private final UsuarioRepository usuarioRepository;
    private final ProcesoReservaRepository procesoReservaRepository;
    private final ReservasCatedraClient reservasCatedraClient;
    private final Clock clock;
    // El arranque y el scheduler pueden coincidir: una vuelta a la vez (supuesto: una sola instancia).
    private final ReentrantLock enCurso = new ReentrantLock();

    @Override
    public Optional<ResultadoReconciliacion> reconciliar() {
        if (!enCurso.tryLock()) {
            log.info("Ya hay una reconciliación en curso: se saltea esta vuelta");
            return Optional.empty();
        }
        try {
            return Optional.of(reconciliarTodo());
        } finally {
            enCurso.unlock();
        }
    }

    // Los CONFIRMADO de hoy en adelante se revisan por si una cancelación (CU-08) dio timeout
    // o se perdió el AppointmentCancelled.
    private ResultadoReconciliacion reconciliarTodo() {
        Instant ahora = clock.instant();
        List<ProcesoReserva> procesos = new ArrayList<>(procesoReservaRepository.findByEstados(PENDIENTES));
        procesos.addAll(procesoReservaRepository.findConfirmadosDesde(LocalDate.now(clock)));

        Map<Boolean, List<ProcesoReserva>> iniciados = procesos.stream()
                .collect(Collectors.partitioningBy(proceso -> proceso.getEstado() == EstadoProceso.INICIADO));
        int cambiados = 0;
        for (ProcesoReserva proceso : iniciados.get(true)) {
            if (reconciliarIniciado(proceso, ahora)) {
                cambiados++;
            }
        }
        Map<Long, List<ProcesoReserva>> porUsuario = iniciados.get(false).stream()
                .collect(Collectors.groupingBy(ProcesoReserva::getUsuarioId));
        for (Map.Entry<Long, List<ProcesoReserva>> usuario : porUsuario.entrySet()) {
            cambiados += reconciliarUsuario(usuario.getKey(), usuario.getValue(), ahora);
        }

        log.info("Reconciliación terminada: {} procesos revisados, {} cambiados", procesos.size(), cambiados);
        return ResultadoReconciliacion.builder().revisados(procesos.size()).cambiados(cambiados).build();
    }

    // D-17: sin holdId no hay con qué buscarlo, y un hold HELD de la cuenta no prueba que sea de este proceso.
    // Pasada la vigencia máxima de un hold, si se llegó a crear ya venció solo.
    private boolean reconciliarIniciado(ProcesoReserva proceso, Instant ahora) {
        if (proceso.getCreadoEn().plus(VIGENCIA_MAXIMA_HOLD).isAfter(ahora)) {
            return false;
        }
        return releer(proceso).map(actual -> cambiar(actual, EstadoProceso.FALLIDO, MOTIVO_INCIERTO, null))
                .orElse(false);
    }

    private int reconciliarUsuario(Long usuarioId, List<ProcesoReserva> procesos, Instant ahora) {
        Optional<Map<String, ReservaCatedra>> reservas = consultarReservas(usuarioId);
        if (reservas.isEmpty()) {
            return 0;
        }
        int cambiados = 0;
        for (ProcesoReserva proceso : procesos) {
            ReservaCatedra reserva = reservas.get().get(proceso.getReservationProcessId());
            if (releer(proceso).map(actual -> aplicar(actual, reserva, ahora)).orElse(false)) {
                cambiados++;
            }
        }
        return cambiados;
    }

    // Si la cátedra no responde, los procesos del usuario no se tocan y se reintenta en la próxima vuelta.
    private Optional<Map<String, ReservaCatedra>> consultarReservas(Long usuarioId) {
        Optional<String> paciente = usuarioRepository.findById(usuarioId)
                .map(usuario -> usuario.getExternalPatientId().toString());
        if (paciente.isEmpty()) {
            log.warn("Reconciliación: no existe el usuario {} de los procesos pendientes", usuarioId);
            return Optional.empty();
        }
        try {
            return Optional.of(reservasCatedraClient.listarReservas(paciente.get()).stream()
                    .filter(reserva -> reserva.getReservationProcessId() != null)
                    .collect(Collectors.toMap(ReservaCatedra::getReservationProcessId, Function.identity(),
                            (primera, repetida) -> primera)));
        } catch (CatedraNoDisponibleException | CatedraRechazoException e) {
            log.warn("Reconciliación: no se pudieron consultar las reservas del usuario {}: {}",
                    usuarioId, e.getMessage());
            return Optional.empty();
        }
    }

    // Un evento de Kafka pudo mover el proceso mientras se consultaba a la cátedra: se decide con lo guardado.
    private Optional<ProcesoReserva> releer(ProcesoReserva leido) {
        return procesoReservaRepository.findById(leido.getId())
                .filter(actual -> actual.getEstado() == leido.getEstado());
    }

    private boolean aplicar(ProcesoReserva proceso, ReservaCatedra reserva, Instant ahora) {
        boolean vencido = proceso.getExpiresAt() != null && !proceso.getExpiresAt().isAfter(ahora);
        if (proceso.getEstado() == EstadoProceso.CONFIRMADO) {
            return reserva != null && reserva.getEstado() == EstadoReservaCatedra.CANCELADA
                    && cambiar(proceso, EstadoProceso.CANCELADO, MOTIVO_CANCELADA, null);
        }
        if (reserva == null) {
            return vencido && cambiar(proceso, EstadoProceso.VENCIDO, MOTIVO_VENCIDA, null);
        }
        if (reserva.getEstado() == null) {
            return false;
        }
        return switch (reserva.getEstado()) {
            case CONFIRMADA -> cambiar(proceso, EstadoProceso.CONFIRMADO, MOTIVO_CONFIRMADA, reserva.getReservationId());
            case CANCELADA -> cambiar(proceso, EstadoProceso.CANCELADO, MOTIVO_CANCELADA, null);
            case FALLIDA -> vencido
                    ? cambiar(proceso, EstadoProceso.VENCIDO, MOTIVO_VENCIDA, null)
                    : cambiar(proceso, EstadoProceso.FALLIDO, MOTIVO_FALLIDA, null);
            // Sin requestEventId: el teléfono no se puede informar hasta que llegue el pedido por Kafka (CU-05 8b).
            case TELEFONO_PENDIENTE -> vencido
                    ? cambiar(proceso, EstadoProceso.VENCIDO, MOTIVO_VENCIDA, null)
                    : proceso.getEstado() == EstadoProceso.BLOQUEADO
                            && cambiar(proceso, EstadoProceso.ESPERANDO_TELEFONO, MOTIVO_TELEFONO, null);
        };
    }

    // Siempre por la máquina de estados (D-05): una transición no permitida se registra y no se aplica.
    private boolean cambiar(ProcesoReserva proceso, EstadoProceso destino, String motivo, Long reservationId) {
        EstadoProceso anterior = proceso.getEstado();
        if (!anterior.puedePasarA(destino)) {
            log.info("Reconciliación: el proceso {} no puede pasar de {} a {}", proceso.getId(), anterior, destino);
            return false;
        }
        proceso.setMotivo(motivo);
        if (reservationId != null) {
            proceso.setReservationId(reservationId);
        }
        proceso.pasarA(destino);
        proceso.setActualizadoEn(clock.instant());
        procesoReservaRepository.save(proceso);
        log.info("Reconciliación: el proceso {} pasó de {} a {}", proceso.getId(), anterior, destino);
        return true;
    }
}
