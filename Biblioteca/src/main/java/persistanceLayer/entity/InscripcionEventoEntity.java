package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Inscripción de un usuario a un evento. Un usuario solo puede inscribirse una vez por evento.
 */
@Entity
@Table(
        name = "inscripciones_evento",
        uniqueConstraints = @UniqueConstraint(columnNames = {"evento_id", "usuario_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InscripcionEventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaInscripcion;

    /** Cantidad de cupos reservados por el usuario. */
    @Column(nullable = false)
    @Builder.Default
    private Integer cantidadCupos = 1;

    /** Se llena al cancelar y liberar el cupo. */
    private LocalDateTime fechaCancelacion;

    /** ACTIVA, CANCELADA. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "ACTIVA";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private EventoEntity evento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;
}
