package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
/**
 * Justificación de un retraso en la devolución, enviada por el usuario a través del chatbot (RF-15).
 * Se almacena y se dirige al bibliotecario para que revise la sanción.
 */
@Entity
@Table(name = "justificaciones_retraso")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JustificacionRetrasoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    @Column(nullable = false)
    private LocalDateTime fechaEnvio;

    /** PENDIENTE, ACEPTADA, RECHAZADA. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "PENDIENTE";

    @Column(columnDefinition = "TEXT")
    private String respuesta;

    private LocalDateTime fechaRespuesta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prestamo_id", nullable = false)
    private PrestamoEntity prestamo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    /** Bibliotecario al que se envía la justificación. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bibliotecario_id")
    private UsuarioEntity bibliotecario;
}
