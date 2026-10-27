package ar.edu.um.programacion2.turnos.reserva.application.exception;

import lombok.Getter;

@Getter
public class CatedraRechazoException extends RuntimeException {

    private final String codigo;

    public CatedraRechazoException(String mensaje, String codigo, Throwable causa) {
        super(mensaje, causa);
        this.codigo = codigo;
    }
}
