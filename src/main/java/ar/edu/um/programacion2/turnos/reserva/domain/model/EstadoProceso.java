package ar.edu.um.programacion2.turnos.reserva.domain.model;

import java.util.EnumSet;
import java.util.Set;

public enum EstadoProceso {
    INICIADO,
    BLOQUEADO,
    ESPERANDO_TELEFONO,
    TELEFONO_ENVIADO,
    CONFIRMADO,
    CANCELADO,
    VENCIDO,
    INVALIDO,
    FALLIDO;

    public boolean puedePasarA(EstadoProceso nuevo) {
        return siguientes().contains(nuevo);
    }

    public boolean esFinal() {
        return siguientes().isEmpty();
    }

    private Set<EstadoProceso> siguientes() {
        return switch (this) {
            case INICIADO -> EnumSet.of(BLOQUEADO, FALLIDO, CANCELADO);
            case BLOQUEADO -> EnumSet.of(ESPERANDO_TELEFONO, VENCIDO, FALLIDO, CANCELADO);
            case ESPERANDO_TELEFONO -> EnumSet.of(TELEFONO_ENVIADO, VENCIDO, INVALIDO, FALLIDO, CANCELADO);
            case TELEFONO_ENVIADO -> EnumSet.of(ESPERANDO_TELEFONO, CONFIRMADO, VENCIDO, INVALIDO, FALLIDO, CANCELADO);
            case CONFIRMADO -> EnumSet.of(CANCELADO);
            case CANCELADO, VENCIDO, INVALIDO, FALLIDO -> EnumSet.noneOf(EstadoProceso.class);
        };
    }
}
