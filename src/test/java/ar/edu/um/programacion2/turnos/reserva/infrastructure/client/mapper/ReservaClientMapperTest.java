package ar.edu.um.programacion2.turnos.reserva.infrastructure.client.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import ar.edu.um.programacion2.turnos.reserva.domain.model.HoldConfirmado;
import ar.edu.um.programacion2.turnos.reserva.domain.model.HoldCreado;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ReservaCancelada;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto.CancelacionCatedraRequest;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto.CancelacionCatedraResponse;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto.ConfirmacionHoldCatedraRequest;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto.ConfirmacionHoldCatedraResponse;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto.HoldCatedraRequest;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.client.dto.HoldCatedraResponse;

class ReservaClientMapperTest {

    private static final Instant INSTANTE = Instant.parse("2026-09-10T14:44:50Z");

    private final ReservaClientMapper mapper = new ReservaClientMapper();

    @Test
    void toHoldRequestArmaElPedidoDelHold() {
        HoldCatedraRequest request = mapper.toHoldRequest(15L, LocalDate.of(2026, 9, 14), LocalTime.of(9, 30), "user-42");

        assertThat(request.getProfessionalId()).isEqualTo(15L);
        assertThat(request.getDate()).isEqualTo(LocalDate.of(2026, 9, 14));
        assertThat(request.getStartTime()).isEqualTo(LocalTime.of(9, 30));
        assertThat(request.getExternalPatientId()).isEqualTo("user-42");
    }

    @Test
    void toConfirmacionRequestArmaElPedidoConElPaciente() {
        ConfirmacionHoldCatedraRequest request = mapper.toConfirmacionRequest("user-42", "Juan", "Perez");

        assertThat(request.getExternalPatientId()).isEqualTo("user-42");
        assertThat(request.getPatientFirstName()).isEqualTo("Juan");
        assertThat(request.getPatientLastName()).isEqualTo("Perez");
    }

    @Test
    void toHoldConfirmadoTraduceLaRespuestaDeLaConfirmacion() {
        ConfirmacionHoldCatedraResponse response = ConfirmacionHoldCatedraResponse.builder()
                .reservationProcessId("process-a92b47d0")
                .status("WAITING_FOR_PHONE")
                .expiresAt(INSTANTE)
                .build();

        HoldConfirmado confirmado = mapper.toHoldConfirmado(response);

        assertThat(confirmado.getReservationProcessId()).isEqualTo("process-a92b47d0");
        assertThat(confirmado.getVenceEn()).isEqualTo(INSTANTE);
    }

    @Test
    void toCancelacionRequestArmaElPedidoConElMotivo() {
        CancelacionCatedraRequest request = mapper.toCancelacionRequest("Abandonado por el usuario");

        assertThat(request.getReason()).isEqualTo("Abandonado por el usuario");
    }

    @Test
    void toHoldCreadoTraduceLaRespuestaDelHold() {
        HoldCatedraResponse response = HoldCatedraResponse.builder()
                .holdId("hold-6c0c2f41")
                .reservationProcessId("process-a92b47d0")
                .status("HELD")
                .expiresAt(INSTANTE)
                .build();

        HoldCreado hold = mapper.toHoldCreado(response);

        assertThat(hold.getHoldId()).isEqualTo("hold-6c0c2f41");
        assertThat(hold.getReservationProcessId()).isEqualTo("process-a92b47d0");
        assertThat(hold.getVenceEn()).isEqualTo(INSTANTE);
    }

    @Test
    void toReservaCanceladaTraduceLaRespuestaDeLaCancelacion() {
        CancelacionCatedraResponse response = CancelacionCatedraResponse.builder()
                .reservationId(8251L)
                .reservationProcessId("process-a92b47d0")
                .status("CANCELLED")
                .cancelledAt(INSTANTE)
                .build();

        ReservaCancelada cancelada = mapper.toReservaCancelada(response);

        assertThat(cancelada.getReservationProcessId()).isEqualTo("process-a92b47d0");
        assertThat(cancelada.getCanceladaEn()).isEqualTo(INSTANTE);
    }

    @Test
    void lasRespuestasDevuelvenNullSiRecibenNull() {
        assertThat(mapper.toHoldCreado(null)).isNull();
        assertThat(mapper.toHoldConfirmado(null)).isNull();
        assertThat(mapper.toReservaCancelada(null)).isNull();
    }
}
