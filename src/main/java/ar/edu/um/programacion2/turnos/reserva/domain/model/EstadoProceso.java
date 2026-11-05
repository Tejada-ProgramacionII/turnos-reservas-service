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

    // CU-10: con el teléfono ya enviado la cátedra está decidiendo, así que no se abandona.
    public boolean esAbandonable() {
        return this == INICIADO || this == BLOQUEADO || this == ESPERANDO_TELEFONO;
    }

    // Solo una reserva confirmada se cancela (CU-08).
    public boolean esCancelable() {
        return this == CONFIRMADO;
    }

    public boolean esFinal() {
        return siguientes().isEmpty();
    }

    private Set<EstadoProceso> siguientes() {
        return switch (this) {
            case INICIADO -> EnumSet.of(BLOQUEADO, FALLIDO, CANCELADO);
            // CONFIRMADO desde BLOQUEADO o ESPERANDO_TELEFONO: la cátedra confirmó aunque acá no se vio
            // un paso intermedio (por ejemplo, un teléfono que dio timeout pero llegó). Manda la cátedra.
            case BLOQUEADO -> EnumSet.of(ESPERANDO_TELEFONO, CONFIRMADO, VENCIDO, FALLIDO, CANCELADO);
            case ESPERANDO_TELEFONO -> EnumSet.of(TELEFONO_ENVIADO, CONFIRMADO, VENCIDO, INVALIDO, FALLIDO, CANCELADO);
            case TELEFONO_ENVIADO -> EnumSet.of(ESPERANDO_TELEFONO, CONFIRMADO, VENCIDO, INVALIDO, FALLIDO, CANCELADO);
            case CONFIRMADO -> EnumSet.of(CANCELADO);
            case CANCELADO, VENCIDO, INVALIDO, FALLIDO -> EnumSet.noneOf(EstadoProceso.class);
        };
    }
}
