package ar.edu.um.programacion2.turnos.reserva.infrastructure.web.controller;

import java.net.URI;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
import ar.edu.um.programacion2.turnos.reserva.infrastructure.web.dto.IniciarReservaRequest;
import ar.edu.um.programacion2.turnos.reserva.infrastructure.web.mapper.ProcesoReservaDtoMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    // Sin respuesta de la cátedra: el resultado queda pendiente y lo resuelve CU-09.
    private static final Set<EstadoProceso> PENDIENTES = Set.of(EstadoProceso.INICIADO, EstadoProceso.BLOQUEADO);

    private final ReservaService reservaService;
    private final ProcesoReservaDtoMapper procesoReservaDtoMapper;

    @PostMapping
    public ResponseEntity<Object> iniciarReserva(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody IniciarReservaRequest request) {
        try {
            ProcesoReserva proceso = reservaService.iniciarReserva(jwt.getSubject(), request.getProfesionalId(),
                    request.getFecha(), request.getHoraInicio());
            HttpStatus status = PENDIENTES.contains(proceso.getEstado()) ? HttpStatus.ACCEPTED : HttpStatus.CREATED;
            return ResponseEntity.status(status)
                    .location(URI.create("/api/reservas/" + proceso.getId()))
                    .body(procesoReservaDtoMapper.toResponse(proceso));
        } catch (ReservaInvalidaException e) {
            return problema(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (ProfesionalNoEncontradoException e) {
            return problema(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (ReservaRechazadaException e) {
            ProblemDetail detalle = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
            detalle.setProperty("procesoId", e.getProceso().getId());
            detalle.setProperty("estado", e.getProceso().getEstado().name());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(detalle);
        } catch (CatalogoNoDisponibleException e) {
            return problema(HttpStatus.SERVICE_UNAVAILABLE, "La reserva no se puede iniciar ahora");
        } catch (CatalogoRechazoException | CatedraRechazoException e) {
            return problema(HttpStatus.BAD_GATEWAY, "Error de integración al iniciar la reserva");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getProcesoReserva(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        try {
            return ResponseEntity.ok(procesoReservaDtoMapper.toResponse(
                    reservaService.getProcesoReserva(jwt.getSubject(), id)));
        } catch (ProcesoReservaNoEncontradoException e) {
            return problema(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    private static ResponseEntity<Object> problema(HttpStatus status, String detalle) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detalle));
    }
}
