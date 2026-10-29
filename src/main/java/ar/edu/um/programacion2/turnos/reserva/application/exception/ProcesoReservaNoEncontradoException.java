package ar.edu.um.programacion2.turnos.reserva.application.exception;

public class ProcesoReservaNoEncontradoException extends RuntimeException {
    public ProcesoReservaNoEncontradoException(Long id) {
        super("No se encontró el proceso de reserva con id: " + id);
    }
}
