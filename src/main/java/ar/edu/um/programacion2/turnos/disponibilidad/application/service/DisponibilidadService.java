package ar.edu.um.programacion2.turnos.disponibilidad.application.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import ar.edu.um.programacion2.turnos.config.DisponibilidadProperties;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.CatalogoNoDisponibleException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.CatedraNoDisponibleException;
import ar.edu.um.programacion2.turnos.disponibilidad.application.exception.ProfesionalNoEncontradoException;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.model.Disponibilidad;
import ar.edu.um.programacion2.turnos.disponibilidad.domain.ports.in.ConsultarDisponibilidadUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisponibilidadService {

    private final ConsultarDisponibilidadUseCase consultarDisponibilidadUseCase;
    private final DisponibilidadProperties disponibilidadProperties;

    // Solo se reintenta si el catálogo o la cátedra no responden (CU-03, 3c y 5a); un rechazo no se arregla reintentando.
    public Disponibilidad consultarDisponibilidad(Long profesionalId, LocalDate fecha) {
        int maxIntentos = disponibilidadProperties.getMaxIntentos();
        for (int intento = 1; ; intento++) {
            try {
                return consultarDisponibilidadUseCase.consultarDisponibilidad(profesionalId, fecha)
                        .orElseThrow(() -> new ProfesionalNoEncontradoException(profesionalId));
            } catch (CatalogoNoDisponibleException | CatedraNoDisponibleException e) {
                log.warn("Intento {} de {} de consultar la disponibilidad fallido: {}", intento, maxIntentos, e.getMessage());
                if (intento >= maxIntentos || !esperarEntreIntentos()) {
                    throw e;
                }
            }
        }
    }

    private boolean esperarEntreIntentos() {
        try {
            Thread.sleep(disponibilidadProperties.getEsperaEntreIntentos());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
