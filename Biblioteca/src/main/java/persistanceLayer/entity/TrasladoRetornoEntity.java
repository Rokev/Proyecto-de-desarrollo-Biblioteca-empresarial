package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Regreso de un ejemplar a su sede de origen despues de una devolucion en otra sede (RF-08).
 */
@Entity
@Table(name = "traslados_retorno")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrasladoRetornoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** EN_TRANSITO, RECIBIDO. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "EN_TRANSITO";

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    /** Se llena cuando la sede de origen recibe el ejemplar. */
    private LocalDateTime fechaRecepcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejemplar_id", nullable = false)
    private EjemplarEntity ejemplar;

    /** Sede donde se entrego el libro y desde la que se envia de regreso. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_actual_id", nullable = false)
    private SedeEntity sedeActual;

    /** Sede de origen del ejemplar, a la que vuelve. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_destino_id", nullable = false)
    private SedeEntity sedeDestino;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "devolucion_id", nullable = false, unique = true)
    private DevolucionEntity devolucion;

    /** Bibliotecario de la sede de origen que recibe el ejemplar. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bibliotecario_recepcion_id")
    private UsuarioEntity bibliotecarioRecepcion;
}