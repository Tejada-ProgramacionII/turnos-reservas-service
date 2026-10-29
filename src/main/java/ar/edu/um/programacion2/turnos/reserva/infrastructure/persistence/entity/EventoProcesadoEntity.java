package ar.edu.um.programacion2.turnos.reserva.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "eventos_procesados")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoProcesadoEntity {
    @Id
    @Column(length = 100)
    private String eventId;

    @Column(nullable = false)
    private Instant procesadoEn;
}
