package ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import ar.edu.um.programacion2.turnos.reserva.domain.model.EstadoProceso;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "procesos_reserva")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcesoReservaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private Long profesionalId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFin;

    @Column(length = 100)
    private String profesionalNombre;

    @Column(length = 100)
    private String profesionalApellido;

    @Column(length = 100)
    private String holdId;

    @Column(length = 100, unique = true)
    private String reservationProcessId;

    @Column
    private Instant expiresAt;

    @Column(length = 100)
    private String requestEventId;

    @Column(length = 500)
    private String mensajePedido;

    @Column(length = 30)
    private String telefono;

    @Column
    private Long reservationId;

    @Column(length = 500)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private EstadoProceso estado;

    @Column(nullable = false)
    private Instant creadoEn;

    @Column(nullable = false)
    private Instant actualizadoEn;
}
