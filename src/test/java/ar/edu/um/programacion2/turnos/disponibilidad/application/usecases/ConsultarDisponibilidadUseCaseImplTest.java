package ar.edu.um.programacion2.turnos.disponibilidad.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.FechaPasadaException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.ProfesionalDeshabilitadoException;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.AgendaProfesional;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.Disponibilidad;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.EstadoOcupacion;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.HorarioAtencion;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.Ocupacion;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.TurnoLibre;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.ports.out.CatalogoClient;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.ports.out.OcupacionesCatedraClient;

@ExtendWith(MockitoExtension.class)
class ConsultarDisponibilidadUseCaseImplTest {

    private static final Long PROFESIONAL_ID = 201L;
    private static final ZoneId MENDOZA = ZoneId.of("America/Argentina/Mendoza");
    // "Hoy" en los tests: lunes 2 de noviembre de 2026.
    private static final LocalDate HOY = LocalDate.of(2026, 11, 2);
    private static final LocalDate PROXIMO_LUNES = LocalDate.of(2026, 11, 9);

    @Mock
    private CatalogoClient catalogoClient;

    @Mock
    private OcupacionesCatedraClient ocupacionesCatedraClient;

    @Test
    void parteElHorarioEnTurnosDeLaDuracionIndicada() {
        agendaHabilitada(horario(DayOfWeek.MONDAY, 12, 0, 15, 0, 30));
        sinOcupaciones(PROXIMO_LUNES);

        List<TurnoLibre> turnos = consultar(a(HOY, 8, 0), PROXIMO_LUNES).getTurnos();

        assertThat(horasDeInicio(turnos)).containsExactly(
                LocalTime.of(12, 0), LocalTime.of(12, 30), LocalTime.of(13, 0),
                LocalTime.of(13, 30), LocalTime.of(14, 0), LocalTime.of(14, 30));
        assertThat(turnos.get(5).getHoraFin()).isEqualTo(LocalTime.of(15, 0));
    }

    @Test
    void noIncluyeElUltimoTurnoSiNoEntraCompleto() {
        agendaHabilitada(horario(DayOfWeek.MONDAY, 12, 0, 13, 45, 30));
        sinOcupaciones(PROXIMO_LUNES);

        List<TurnoLibre> turnos = consultar(a(HOY, 8, 0), PROXIMO_LUNES).getTurnos();

        assertThat(horasDeInicio(turnos)).containsExactly(LocalTime.of(12, 0), LocalTime.of(12, 30), LocalTime.of(13, 0));
        assertThat(turnos.get(2).getHoraFin()).isEqualTo(LocalTime.of(13, 30));
    }

    @Test
    void usaSoloLosHorariosDelDiaYOrdenaLosTurnosPorHora() {
        agendaHabilitada(
                horario(DayOfWeek.MONDAY, 17, 0, 18, 0, 30),
                horario(DayOfWeek.TUESDAY, 9, 0, 10, 0, 30),
                horario(DayOfWeek.MONDAY, 9, 0, 10, 0, 30));
        sinOcupaciones(PROXIMO_LUNES);

        List<TurnoLibre> turnos = consultar(a(HOY, 8, 0), PROXIMO_LUNES).getTurnos();

        assertThat(horasDeInicio(turnos)).containsExactly(
                LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(17, 0), LocalTime.of(17, 30));
    }

    @Test
    void descartaLosTurnosOcupadosConfirmadosORetenidos() {
        agendaHabilitada(horario(DayOfWeek.MONDAY, 12, 0, 14, 0, 30));
        when(ocupacionesCatedraClient.getOcupaciones(PROFESIONAL_ID, PROXIMO_LUNES, PROXIMO_LUNES)).thenReturn(List.of(
                ocupacion(PROXIMO_LUNES, 12, 30, EstadoOcupacion.CONFIRMADA),
                ocupacion(PROXIMO_LUNES, 13, 0, EstadoOcupacion.RETENIDA),
                ocupacion(PROXIMO_LUNES.plusDays(1), 12, 0, EstadoOcupacion.CONFIRMADA)));

        Disponibilidad disponibilidad = consultar(a(HOY, 8, 0), PROXIMO_LUNES);

        assertThat(disponibilidad.getProfesionalId()).isEqualTo(PROFESIONAL_ID);
        assertThat(disponibilidad.getFecha()).isEqualTo(PROXIMO_LUNES);
        assertThat(horasDeInicio(disponibilidad.getTurnos())).containsExactly(LocalTime.of(12, 0), LocalTime.of(13, 30));
    }

    @Test
    void siLaFechaEsHoyDescartaLosTurnosQueYaEmpezaron() {
        agendaHabilitada(horario(DayOfWeek.MONDAY, 12, 0, 14, 0, 30));
        sinOcupaciones(HOY);

        List<TurnoLibre> turnos = consultar(a(HOY, 12, 30), HOY).getTurnos();

        assertThat(horasDeInicio(turnos)).containsExactly(LocalTime.of(12, 30), LocalTime.of(13, 0), LocalTime.of(13, 30));
    }

    @Test
    void siNoHayHorariosEseDiaDevuelveListaVaciaSinPreguntarALaCatedra() {
        agendaHabilitada(horario(DayOfWeek.WEDNESDAY, 17, 0, 20, 0, 30));

        assertThat(consultar(a(HOY, 8, 0), PROXIMO_LUNES).getTurnos()).isEmpty();
        verifyNoInteractions(ocupacionesCatedraClient);
    }

    @Test
    void siHoyYaPasaronTodosLosTurnosNoPreguntaALaCatedra() {
        agendaHabilitada(horario(DayOfWeek.MONDAY, 12, 0, 14, 0, 30));

        assertThat(consultar(a(HOY, 19, 0), HOY).getTurnos()).isEmpty();
        verifyNoInteractions(ocupacionesCatedraClient);
    }

    @Test
    void siEstanTodosOcupadosDevuelveListaVacia() {
        agendaHabilitada(horario(DayOfWeek.MONDAY, 12, 0, 13, 0, 30));
        when(ocupacionesCatedraClient.getOcupaciones(PROFESIONAL_ID, PROXIMO_LUNES, PROXIMO_LUNES)).thenReturn(List.of(
                ocupacion(PROXIMO_LUNES, 12, 0, EstadoOcupacion.CONFIRMADA),
                ocupacion(PROXIMO_LUNES, 12, 30, EstadoOcupacion.RETENIDA)));

        assertThat(consultar(a(HOY, 8, 0), PROXIMO_LUNES).getTurnos()).isEmpty();
    }

    @Test
    void ignoraLosHorariosConDuracionInvalida() {
        agendaHabilitada(horario(DayOfWeek.MONDAY, 12, 0, 13, 0, 0));

        assertThat(consultar(a(HOY, 8, 0), PROXIMO_LUNES).getTurnos()).isEmpty();
    }

    @Test
    void siElProfesionalNoExisteDevuelveVacio() {
        when(catalogoClient.consultarAgenda(PROFESIONAL_ID)).thenReturn(Optional.empty());

        assertThat(crearUseCase(a(HOY, 8, 0)).consultarDisponibilidad(PROFESIONAL_ID, PROXIMO_LUNES)).isEmpty();
        verifyNoInteractions(ocupacionesCatedraClient);
    }

    @Test
    void siElProfesionalEstaDeshabilitadoLanzaExcepcion() {
        when(catalogoClient.consultarAgenda(PROFESIONAL_ID)).thenReturn(Optional.of(AgendaProfesional.builder()
                .profesionalId(PROFESIONAL_ID)
                .habilitado(false)
                .horarios(List.of(horario(DayOfWeek.MONDAY, 12, 0, 15, 0, 30)))
                .build()));
        ConsultarDisponibilidadUseCaseImpl useCase = crearUseCase(a(HOY, 8, 0));

        assertThatThrownBy(() -> useCase.consultarDisponibilidad(PROFESIONAL_ID, PROXIMO_LUNES))
                .isInstanceOf(ProfesionalDeshabilitadoException.class)
                .hasMessageContaining("no admite reservas nuevas");
        verifyNoInteractions(ocupacionesCatedraClient);
    }

    @Test
    void siLaFechaYaPasoLanzaExcepcionSinConsultarNada() {
        ConsultarDisponibilidadUseCaseImpl useCase = crearUseCase(a(HOY, 8, 0));
        LocalDate ayer = HOY.minusDays(1);

        assertThatThrownBy(() -> useCase.consultarDisponibilidad(PROFESIONAL_ID, ayer))
                .isInstanceOf(FechaPasadaException.class)
                .hasMessageContaining("2026-11-01");
        verifyNoInteractions(catalogoClient, ocupacionesCatedraClient);
    }

    private Disponibilidad consultar(LocalDateTime ahora, LocalDate fecha) {
        Optional<Disponibilidad> disponibilidad = crearUseCase(ahora).consultarDisponibilidad(PROFESIONAL_ID, fecha);
        verify(catalogoClient).consultarAgenda(PROFESIONAL_ID);
        return disponibilidad.orElseThrow();
    }

    private ConsultarDisponibilidadUseCaseImpl crearUseCase(LocalDateTime ahora) {
        Clock clock = Clock.fixed(ahora.atZone(MENDOZA).toInstant(), MENDOZA);
        return new ConsultarDisponibilidadUseCaseImpl(catalogoClient, ocupacionesCatedraClient, clock);
    }

    private void agendaHabilitada(HorarioAtencion... horarios) {
        when(catalogoClient.consultarAgenda(PROFESIONAL_ID)).thenReturn(Optional.of(AgendaProfesional.builder()
                .profesionalId(PROFESIONAL_ID)
                .habilitado(true)
                .horarios(List.of(horarios))
                .build()));
    }

    private void sinOcupaciones(LocalDate fecha) {
        when(ocupacionesCatedraClient.getOcupaciones(PROFESIONAL_ID, fecha, fecha)).thenReturn(List.of());
    }

    private static LocalDateTime a(LocalDate fecha, int hora, int minuto) {
        return fecha.atTime(hora, minuto);
    }

    private static HorarioAtencion horario(DayOfWeek dia, int horaInicio, int minutoInicio, int horaFin, int minutoFin,
            int duracion) {
        return HorarioAtencion.builder()
                .diaSemana(dia)
                .horaInicio(LocalTime.of(horaInicio, minutoInicio))
                .horaFin(LocalTime.of(horaFin, minutoFin))
                .duracionTurnoMinutos(duracion)
                .build();
    }

    private static Ocupacion ocupacion(LocalDate fecha, int hora, int minuto, EstadoOcupacion estado) {
        return Ocupacion.builder().fecha(fecha).horaInicio(LocalTime.of(hora, minuto)).estado(estado).build();
    }

    private static List<LocalTime> horasDeInicio(List<TurnoLibre> turnos) {
        return turnos.stream().map(TurnoLibre::getHoraInicio).toList();
    }
}
