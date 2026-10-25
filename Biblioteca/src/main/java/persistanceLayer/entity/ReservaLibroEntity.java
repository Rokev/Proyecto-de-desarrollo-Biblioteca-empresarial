package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
/**
 * Reserva de un libro sin stock en una sede.
 * Cuando se libera un ejemplar se notifica al usuario y se fija la fecha límite de retiro.
 */
@Entity
@Table(name = "reservas_libro")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaLibroEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaReserva;

    /** Cuándo se avisó al usuario que el libro volvió a estar disponible. */
    private LocalDateTime fechaNotificacion;

    /** Fecha límite para retirar el libro una vez notificado. */
    private LocalDateTime fechaLimiteRetiro;

    /** Se llena al cancelar la reserva. */
    private LocalDateTime fechaCancelacion;

    /** EN_ESPERA, NOTIFICADA, RETIRADA, CANCELADA, EXPIRADA. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "EN_ESPERA";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "libro_id", nullable = false)
    private LibroEntity libro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private SedeEntity sede;

    /** Ejemplar apartado cuando el libro vuelve a estar disponible (nulo mientras espera). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ejemplar_id")
    private EjemplarEntity ejemplar;
}
