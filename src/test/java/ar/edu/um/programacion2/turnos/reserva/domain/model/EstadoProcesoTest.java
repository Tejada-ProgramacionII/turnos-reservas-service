package ar.edu.um.programacion2.turnos.reserva.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class EstadoProcesoTest {

    private static final List<String> VALIDAS = List.of(
            "INICIADO>BLOQUEADO", "INICIADO>FALLIDO", "INICIADO>CANCELADO",
            "BLOQUEADO>ESPERANDO_TELEFONO", "BLOQUEADO>VENCIDO", "BLOQUEADO>FALLIDO", "BLOQUEADO>CANCELADO",
            "ESPERANDO_TELEFONO>TELEFONO_ENVIADO", "ESPERANDO_TELEFONO>VENCIDO", "ESPERANDO_TELEFONO>INVALIDO",
            "ESPERANDO_TELEFONO>FALLIDO", "ESPERANDO_TELEFONO>CANCELADO",
            "TELEFONO_ENVIADO>ESPERANDO_TELEFONO", "TELEFONO_ENVIADO>CONFIRMADO", "TELEFONO_ENVIADO>VENCIDO",
            "TELEFONO_ENVIADO>INVALIDO", "TELEFONO_ENVIADO>FALLIDO", "TELEFONO_ENVIADO>CANCELADO",
            "CONFIRMADO>CANCELADO");

    static Stream<Arguments> transicionesValidas() {
        return todasLasTransiciones().filter(t -> VALIDAS.contains(clave(t)));
    }

    static Stream<Arguments> transicionesInvalidas() {
        return todasLasTransiciones().filter(t -> !VALIDAS.contains(clave(t)));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("transicionesValidas")
    void permiteLasTransicionesDelDiagrama(EstadoProceso actual, EstadoProceso nuevo) {
        assertThat(actual.puedePasarA(nuevo)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("transicionesInvalidas")
    void rechazaLasTransicionesQueNoEstanEnElDiagrama(EstadoProceso actual, EstadoProceso nuevo) {
        assertThat(actual.puedePasarA(nuevo)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(names = {"CANCELADO", "VENCIDO", "INVALIDO", "FALLIDO"})
    void losEstadosFinalesSonFinales(EstadoProceso estado) {
        assertThat(estado.esFinal()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(names = {"CANCELADO", "VENCIDO", "INVALIDO", "FALLIDO"}, mode = EnumSource.Mode.EXCLUDE)
    void losDemasEstadosNoSonFinales(EstadoProceso estado) {
        assertThat(estado.esFinal()).isFalse();
    }

    @Test
    void elDiagramaTieneDiecinueveTransiciones() {
        assertThat(transicionesValidas()).hasSize(VALIDAS.size());
    }

    private static Stream<Arguments> todasLasTransiciones() {
        return Arrays.stream(EstadoProceso.values())
                .flatMap(actual -> Arrays.stream(EstadoProceso.values()).map(nuevo -> Arguments.of(actual, nuevo)));
    }

    private static String clave(Arguments transicion) {
        Object[] estados = transicion.get();
        return estados[0] + ">" + estados[1];
    }
}
