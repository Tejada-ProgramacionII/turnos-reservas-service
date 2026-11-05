package ar.edu.um.programacion2.turnos.disponibilidad.application.usecases;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.FechaPasadaException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.ProfesionalDeshabilitadoException;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.AgendaProfesional;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.Disponibilidad;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.HorarioAtencion;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.Ocupacion;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.TurnoLibre;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.ports.in.ConsultarDisponibilidadUseCase;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.ports.out.CatalogoClient;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.ports.out.OcupacionesCatedraClient;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ConsultarDisponibilidadUseCaseImpl implements ConsultarDisponibilidadUseCase {

    private static final int SEGUNDOS_POR_MINUTO = 60;

    private final CatalogoClient catalogoClient;
    private final OcupacionesCatedraClient ocupacionesCatedraClient;
    private final Clock clock;

    @Override
    public Optional<Disponibilidad> consultarDisponibilidad(Long profesionalId, LocalDate fecha) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        if (fecha.isBefore(ahora.toLocalDate())) {
            throw new FechaPasadaException(fecha);
        }
        return catalogoClient.consultarAgenda(profesionalId)
                .map(agenda -> armarDisponibilidad(agenda, fecha, ahora));
    }

    private Disponibilidad armarDisponibilidad(AgendaProfesional agenda, LocalDate fecha, LocalDateTime ahora) {
        if (!agenda.isHabilitado()) {
            throw new ProfesionalDeshabilitadoException(agenda.getProfesionalId());
        }
        boolean esHoy = fecha.equals(ahora.toLocalDate());
        List<TurnoLibre> turnos = agenda.getHorarios().stream()
                .filter(horario -> horario.getDiaSemana() == fecha.getDayOfWeek())
                .flatMap(this::partirEnTurnos)
                .filter(turno -> !esHoy || !turno.getHoraInicio().isBefore(ahora.toLocalTime()))
                .sorted(Comparator.comparing(TurnoLibre::getHoraInicio))
                .toList();
        // Sin turnos posibles no hace falta preguntarle a la cátedra.
        if (!turnos.isEmpty()) {
            turnos = descartarOcupados(turnos, agenda.getProfesionalId(), fecha);
        }
        return Disponibilidad.builder()
                .profesionalId(agenda.getProfesionalId())
                .fecha(fecha)
                .turnos(turnos)
                .build();
    }

    // Un turno entra solo si termina antes o justo en la hora de fin del horario.
    private Stream<TurnoLibre> partirEnTurnos(HorarioAtencion horario) {
        int duracion = horario.getDuracionTurnoMinutos();
        if (duracion <= 0) {
            return Stream.empty();
        }
        int inicio = minutosDelDia(horario.getHoraInicio());
        int fin = minutosDelDia(horario.getHoraFin());
        return IntStream.iterate(inicio, minuto -> minuto + duracion <= fin, minuto -> minuto + duracion)
                .mapToObj(minuto -> TurnoLibre.builder()
                        .horaInicio(horaDelMinuto(minuto))
                        .horaFin(horaDelMinuto(minuto + duracion))
                        .build());
    }

    // Confirmada o retenida da igual: en los dos casos el turno no está libre.
    private List<TurnoLibre> descartarOcupados(List<TurnoLibre> turnos, Long profesionalId, LocalDate fecha) {
        Set<LocalTime> ocupados = ocupacionesCatedraClient.getOcupaciones(profesionalId, fecha, fecha).stream()
                .filter(ocupacion -> fecha.equals(ocupacion.getFecha()))
                .map(Ocupacion::getHoraInicio)
                .collect(Collectors.toSet());
        return turnos.stream()
                .filter(turno -> !ocupados.contains(turno.getHoraInicio()))
                .toList();
    }

    private static int minutosDelDia(LocalTime hora) {
        return hora.toSecondOfDay() / SEGUNDOS_POR_MINUTO;
    }

    private static LocalTime horaDelMinuto(int minuto) {
        return LocalTime.ofSecondOfDay((long) minuto * SEGUNDOS_POR_MINUTO);
    }
}
