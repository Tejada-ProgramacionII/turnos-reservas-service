package ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.web.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.CatalogoNoDisponibleException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.CatalogoRechazoException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.CatedraNoDisponibleException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.CatedraRechazoException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.FechaPasadaException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.ProfesionalDeshabilitadoException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.ProfesionalNoEncontradoException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.service.DisponibilidadService;
import ar.edu.um.programacion2.turnos.disponibilidad.infrastructure.web.mapper.DisponibilidadDtoMapper;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/disponibilidad")
@RequiredArgsConstructor
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;
    private final DisponibilidadDtoMapper disponibilidadDtoMapper;

    @GetMapping
    public ResponseEntity<Object> consultarDisponibilidad(
            @RequestParam Long profesionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        try {
            return ResponseEntity.ok(disponibilidadDtoMapper.toResponse(
                    disponibilidadService.consultarDisponibilidad(profesionalId, fecha)));
        } catch (FechaPasadaException e) {
            return problema(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (ProfesionalNoEncontradoException e) {
            return problema(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (ProfesionalDeshabilitadoException e) {
            return problema(HttpStatus.CONFLICT, e.getMessage());
        } catch (CatalogoNoDisponibleException | CatedraNoDisponibleException e) {
            return problema(HttpStatus.SERVICE_UNAVAILABLE, "La disponibilidad no se puede consultar ahora");
        } catch (CatalogoRechazoException | CatedraRechazoException e) {
            return problema(HttpStatus.BAD_GATEWAY, "Error de integración al consultar la disponibilidad");
        }
    }

    private static ResponseEntity<Object> problema(HttpStatus status, String detalle) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detalle));
    }
}
