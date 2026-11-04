package ar.edu.um.programacion2.turnos.reserva.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.programacion2.turnos.reserva.application.exception.CatedraNoDisponibleException;
import ar.edu.um.programacion2.turnos.reserva.application.exception.CatedraRechazoException;
import ar.edu.um.programacion2.turnos.reserva.application.exception.OperacionNoPermitidaException;
import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ReservaCancelada;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ProcesoReservaRepository;
import ar.edu.um.programacion2.turnos.reserva.domain.ports.out.ReservasCatedraClient;
import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class CancelarReservaUseCaseImplTest {

    private static final String LOGIN = "jtejada";
    private static final String PROCESO_CATEDRA = "process-a92b47d0";
    private static final Instant AHORA = Instant.parse("2026-11-03T15:00:00Z");
    private static final Instant CANCELADA_EN = Instant.parse("2026-11-03T15:00:02Z");

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProcesoReservaRepository procesoReservaRepository;

    @Mock
    private ReservasCatedraClient reservasCatedraClient;

    private CancelarReservaUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new CancelarReservaUseCaseImpl(usuarioRepository, procesoReservaRepository, reservasCatedraClient,
                Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    void cancelaLaReservaConfirmadaGuardandoAntesDeLlamarALaCatedra() {
        ProcesoReserva proceso = existe(EstadoProceso.CONFIRMADO);
        guardaLoQueRecibe();
        when(reservasCatedraClient.cancelar(PROCESO_CATEDRA, "No puedo ir")).thenReturn(cancelada(CANCELADA_EN));

        Optional<ProcesoReserva> resultado = useCase.cancelarReserva(LOGIN, 1L, "  No puedo ir ");

        assertThat(resultado).contains(proceso);
        assertThat(proceso.getEstado()).isEqualTo(EstadoProceso.CANCELADO);
        assertThat(proceso.getCanceladaEn()).isEqualTo(CANCELADA_EN);
        assertThat(proceso.getMotivo()).isEqualTo("No puedo ir");
        assertThat(proceso.getActualizadoEn()).isEqualTo(AHORA);
        InOrder orden = inOrder(procesoReservaRepository, reservasCatedraClient);
        orden.verify(procesoReservaRepository).save(proceso);
        orden.verify(reservasCatedraClient).cancelar(PROCESO_CATEDRA, "No puedo ir");
        orden.verify(procesoReservaRepository).save(proceso);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void sinMotivoUsaElMotivoPorDefecto(String motivo) {
        existe(EstadoProceso.CONFIRMADO);
        guardaLoQueRecibe();
        when(reservasCatedraClient.cancelar(PROCESO_CATEDRA, CancelarReservaUseCaseImpl.MOTIVO_POR_DEFECTO))
                .thenReturn(cancelada(CANCELADA_EN));

        assertThat(useCase.cancelarReserva(LOGIN, 1L, motivo))
                .map(ProcesoReserva::getMotivo)
                .contains(CancelarReservaUseCaseImpl.MOTIVO_POR_DEFECTO);
    }

    @Test
    void sinFechaDeLaCatedraUsaLaHoraLocal() {
        ProcesoReserva proceso = existe(EstadoProceso.CONFIRMADO);
        guardaLoQueRecibe();
        when(reservasCatedraClient.cancelar(any(), any())).thenReturn(cancelada(null));

        useCase.cancelarReserva(LOGIN, 1L, null);

        assertThat(proceso.getCanceladaEn()).isEqualTo(AHORA);
    }

    @Test
    void siKafkaLoCanceloMientrasTantoNoLoVuelveATocar() {
        ProcesoReserva confirmado = proceso(EstadoProceso.CONFIRMADO);
        ProcesoReserva yaCancelado = proceso(EstadoProceso.CANCELADO);
        yaCancelado.setMotivo("Cambio solicitado por el usuario");
        when(usuarioRepository.findByLogin(LOGIN)).thenReturn(Optional.of(Usuario.builder().id(7L).build()));
        when(procesoReservaRepository.findById(1L)).thenReturn(Optional.of(confirmado), Optional.of(yaCancelado));
        guardaLoQueRecibe();
        when(reservasCatedraClient.cancelar(any(), any())).thenReturn(cancelada(CANCELADA_EN));

        assertThat(useCase.cancelarReserva(LOGIN, 1L, null)).contains(yaCancelado);
        assertThat(yaCancelado.getEstado()).isEqualTo(EstadoProceso.CANCELADO);
        assertThat(yaCancelado.getMotivo()).isEqualTo("Cambio solicitado por el usuario");
        assertThat(yaCancelado.getCanceladaEn()).isEqualTo(CANCELADA_EN);
    }

    @Test
    void unProcesoAjenoSeTrataComoInexistente() {
        ProcesoReserva ajeno = proceso(EstadoProceso.CONFIRMADO);
        ajeno.setUsuarioId(8L);
        when(usuarioRepository.findByLogin(LOGIN)).thenReturn(Optional.of(Usuario.builder().id(7L).build()));
        when(procesoReservaRepository.findById(1L)).thenReturn(Optional.of(ajeno));

        assertThat(useCase.cancelarReserva(LOGIN, 1L, null)).isEmpty();
        verifyNoInteractions(reservasCatedraClient);
        verify(procesoReservaRepository, never()).save(any());
    }

    @Test
    void unUsuarioInexistenteNoVeNada() {
        when(usuarioRepository.findByLogin("fantasma")).thenReturn(Optional.empty());

        assertThat(useCase.cancelarReserva("fantasma", 1L, null)).isEmpty();
        verifyNoInteractions(procesoReservaRepository, reservasCatedraClient);
    }

    @ParameterizedTest
    @EnumSource(names = {"INICIADO", "BLOQUEADO", "ESPERANDO_TELEFONO"})
    void unProcesoPendienteSeAbandonaNoSeCancela(EstadoProceso estado) {
        existe(estado);

        assertThatThrownBy(() -> useCase.cancelarReserva(LOGIN, 1L, null))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("abandonar")
                .extracting("estado").isEqualTo(estado);
        verifyNoInteractions(reservasCatedraClient);
        verify(procesoReservaRepository, never()).save(any());
    }

    @Test
    void conElTelefonoEnviadoHayQueEsperarElResultado() {
        existe(EstadoProceso.TELEFONO_ENVIADO);

        assertThatThrownBy(() -> useCase.cancelarReserva(LOGIN, 1L, null))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("esperá el resultado")
                .extracting("estado").isEqualTo(EstadoProceso.TELEFONO_ENVIADO);
        verifyNoInteractions(reservasCatedraClient);
    }

    @ParameterizedTest
    @EnumSource(names = {"CANCELADO", "VENCIDO", "INVALIDO", "FALLIDO"})
    void unProcesoTerminadoNoSeCancela(EstadoProceso estado) {
        existe(estado);

        assertThatThrownBy(() -> useCase.cancelarReserva(LOGIN, 1L, null))
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessage("Solo se puede cancelar una reserva confirmada")
                .extracting("estado").isEqualTo(estado);
        verifyNoInteractions(reservasCatedraClient);
    }

    @Test
    void siLaCatedraRechazaElProcesoNoCambia() {
        ProcesoReserva proceso = existe(EstadoProceso.CONFIRMADO);
        guardaLoQueRecibe();
        CatedraRechazoException rechazo = new CatedraRechazoException("HTTP 409", "RESERVATION_NOT_CANCELLABLE", null);
        when(reservasCatedraClient.cancelar(any(), any())).thenThrow(rechazo);

        assertThatThrownBy(() -> useCase.cancelarReserva(LOGIN, 1L, "No puedo ir")).isSameAs(rechazo);
        assertThat(proceso.getEstado()).isEqualTo(EstadoProceso.CONFIRMADO);
        assertThat(proceso.getMotivo()).isNull();
        assertThat(proceso.getCanceladaEn()).isNull();
    }

    @Test
    void siLaCatedraNoRespondeQuedaConfirmadoConElPedidoGuardado() {
        ProcesoReserva proceso = existe(EstadoProceso.CONFIRMADO);
        guardaLoQueRecibe();
        CatedraNoDisponibleException timeout = new CatedraNoDisponibleException("timeout", null);
        when(reservasCatedraClient.cancelar(any(), any())).thenThrow(timeout);

        assertThatThrownBy(() -> useCase.cancelarReserva(LOGIN, 1L, "No puedo ir")).isSameAs(timeout);
        assertThat(proceso.getEstado()).isEqualTo(EstadoProceso.CONFIRMADO);
        assertThat(proceso.getMotivo()).isEqualTo("No puedo ir");
        assertThat(proceso.getCanceladaEn()).isNull();
    }

    private ProcesoReserva existe(EstadoProceso estado) {
        ProcesoReserva proceso = proceso(estado);
        when(usuarioRepository.findByLogin(LOGIN)).thenReturn(Optional.of(Usuario.builder().id(7L).build()));
        when(procesoReservaRepository.findById(1L)).thenReturn(Optional.of(proceso));
        return proceso;
    }

    private void guardaLoQueRecibe() {
        when(procesoReservaRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private static ProcesoReserva proceso(EstadoProceso estado) {
        return ProcesoReserva.builder()
                .id(1L)
                .usuarioId(7L)
                .reservationProcessId(PROCESO_CATEDRA)
                .estado(estado)
                .build();
    }

    private static ReservaCancelada cancelada(Instant canceladaEn) {
        return ReservaCancelada.builder().reservationProcessId(PROCESO_CATEDRA).canceladaEn(canceladaEn).build();
    }
}
