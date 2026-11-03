package ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import ar.edu.um.programacion2.turnos.reserva.domain.model.Pagina;
import ar.edu.um.programacion2.turnos.reserva.domain.model.ProcesoReserva;
import ar.edu.um.programacion2.turnos.usuario.domain.model.Usuario;
import ar.edu.um.programacion2.turnos.usuario.domain.ports.out.UsuarioRepository;

// Contra H2 con las migraciones reales: el orden y la paginación los resuelve la base.
@SpringBootTest
@Transactional
class JpaProcesoReservaRepositoryAdapterConsultaTest {

    private static final Instant BASE = Instant.parse("2026-11-01T12:00:00Z");

    @Autowired
    private JpaProcesoReservaRepositoryAdapter adapter;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Long yo;
    private Long otro;

    @BeforeEach
    void setUp() {
        yo = crearUsuario("yo");
        otro = crearUsuario("otro");
        guardar(yo, EstadoProceso.CONFIRMADO, 1);
        guardar(yo, EstadoProceso.FALLIDO, 2);
        guardar(yo, EstadoProceso.CONFIRMADO, 3);
        guardar(otro, EstadoProceso.CONFIRMADO, 4);
    }

    @Test
    void devuelveSoloLosProcesosDelUsuarioMasRecientesPrimero() {
        Pagina<ProcesoReserva> pagina = adapter.findByUsuarioId(yo, null, 0, 20);

        assertThat(pagina.getTotal()).isEqualTo(3);
        assertThat(pagina.getContenido()).extracting(ProcesoReserva::getCreadoEn)
                .containsExactly(BASE.plusSeconds(3), BASE.plusSeconds(2), BASE.plusSeconds(1));
        assertThat(pagina.getContenido()).allMatch(proceso -> yo.equals(proceso.getUsuarioId()));
    }

    @Test
    void filtraPorEstado() {
        Pagina<ProcesoReserva> pagina = adapter.findByUsuarioId(yo, EstadoProceso.CONFIRMADO, 0, 20);

        assertThat(pagina.getTotal()).isEqualTo(2);
        assertThat(pagina.getContenido()).extracting(ProcesoReserva::getEstado)
                .containsOnly(EstadoProceso.CONFIRMADO);
    }

    @Test
    void paginaYInformaElTotal() {
        Pagina<ProcesoReserva> segunda = adapter.findByUsuarioId(yo, null, 1, 2);

        assertThat(segunda.getTotal()).isEqualTo(3);
        assertThat(segunda.getContenido()).extracting(ProcesoReserva::getCreadoEn)
                .containsExactly(BASE.plusSeconds(1));
    }

    private Long crearUsuario(String login) {
        return usuarioRepository.create(Usuario.builder()
                .login(login)
                .passwordHash("hash")
                .email(login + "@test.com")
                .activated(true)
                .langKey("es")
                .externalPatientId(UUID.randomUUID())
                .authorities(Set.of())
                .createdBy("test")
                .createdDate(BASE)
                .build()).getId();
    }

    private void guardar(Long usuarioId, EstadoProceso estado, int segundos) {
        adapter.save(ProcesoReserva.builder()
                .usuarioId(usuarioId)
                .profesionalId(201L)
                .fecha(LocalDate.of(2026, 11, 2))
                .horaInicio(LocalTime.of(9, 30))
                .horaFin(LocalTime.of(10, 0))
                .estado(estado)
                .creadoEn(BASE.plusSeconds(segundos))
                .actualizadoEn(BASE.plusSeconds(segundos))
                .build());
    }
}
