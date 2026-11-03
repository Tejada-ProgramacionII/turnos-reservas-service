package ar.edu.um.programacion2.turnos.reserva.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TelefonoTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "+5492615551234      | +5492615551234",
            "+54 9 261 555-1234  | +5492615551234",
            "(261) 555.1234      | 2615551234",
            "1234567             | 1234567",
            "123456789012345     | 123456789012345",
            "+123456789012345    | +123456789012345"
    })
    void quitaLosSeparadoresYAceptaElNumero(String telefono, String esperado) {
        assertThat(Telefono.normalizar(telefono)).contains(esperado);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "123456", "1234567890123456", "++5492615551234", "54+92615551234",
            "261-555-12a4", "261/5551234", "+", "261_5551234"})
    void rechazaLoQueNoCumpleLaRegla(String telefono) {
        assertThat(Telefono.normalizar(telefono)).isEmpty();
    }
}
