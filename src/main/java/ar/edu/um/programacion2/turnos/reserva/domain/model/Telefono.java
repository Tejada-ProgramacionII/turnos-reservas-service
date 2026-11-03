package ar.edu.um.programacion2.turnos.reserva.domain.model;

import java.util.Optional;
import java.util.regex.Pattern;

// Regla de la cátedra (contrato 15.5, D-11): se quitan espacios, guiones, paréntesis y puntos,
// y lo que queda tiene que ser un + opcional seguido de 7 a 15 dígitos.
public final class Telefono {

    public static final String FORMATO = "un + opcional y de 7 a 15 dígitos "
            + "(se ignoran espacios, guiones, paréntesis y puntos)";

    private static final Pattern SEPARADORES = Pattern.compile("[\\s\\-().]");
    private static final Pattern VALIDO = Pattern.compile("^\\+?[0-9]{7,15}$");

    private Telefono() {
    }

    public static Optional<String> normalizar(String telefono) {
        if (telefono == null) {
            return Optional.empty();
        }
        String limpio = SEPARADORES.matcher(telefono).replaceAll("");
        return VALIDO.matcher(limpio).matches() ? Optional.of(limpio) : Optional.empty();
    }
}
