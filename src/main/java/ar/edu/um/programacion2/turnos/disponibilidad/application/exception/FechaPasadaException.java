package ar.edu.um.programacion2.turnos.disponibilidad.application.exception;

import java.time.LocalDate;

public class FechaPasadaException extends RuntimeException {
    public FechaPasadaException(LocalDate fecha) {
        super("La fecha " + fecha + " ya pasó");
    }
}
