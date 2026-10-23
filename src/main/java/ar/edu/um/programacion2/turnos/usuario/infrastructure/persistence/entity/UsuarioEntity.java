package ar.edu.um.programacion2.turnos.usuario.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "usuarios")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, unique = true, nullable = false)
    private String login;

    @Column(length = 60, nullable = false)
    private String passwordHash;

    @Column(length = 50)
    private String firstName;

    @Column(length = 50)
    private String lastName;

    @Column(length = 254, unique = true, nullable = false)
    private String email;

    @Column(length = 256)
    private String imageUrl;

    @Column(nullable = false)
    private boolean activated;

    @Column(length = 10, nullable = false)
    private String langKey;

    @Column(unique = true, nullable = false)
    private UUID externalPatientId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_autoridad", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "authority_name")
    private Set<String> authorities;

    @Column(length = 50, nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private Instant createdDate;

    @Column(length = 50)
    private String lastModifiedBy;

    private Instant lastModifiedDate;
}
