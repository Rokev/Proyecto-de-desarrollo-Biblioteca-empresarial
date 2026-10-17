package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
/**
 * Sanción aplicada a un usuario (por ejemplo, bloqueo de préstamos por retraso) (RF-13).
 * Se consulta junto con las multas.
 */
@Entity
@Table(name = "sanciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SancionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** BLOQUEO_PRESTAMOS, SUSPENSION, ADVERTENCIA, etc. */
    @Column(nullable = false, length = 40)
    private String tipo;

    @Column(nullable = false, length = 200)
    private String motivo;

    @Column(nullable = false)
    private LocalDateTime fechaAplicacion;

    /** Fin de la restricción (nula si es indefinida o ya fue levantada). */
    private LocalDateTime fechaFin;

    /** ACTIVA, FINALIZADA, ANULADA. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "ACTIVA";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    /** Préstamo que originó la sanción (opcional). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestamo_id")
    private PrestamoEntity prestamo;
}
