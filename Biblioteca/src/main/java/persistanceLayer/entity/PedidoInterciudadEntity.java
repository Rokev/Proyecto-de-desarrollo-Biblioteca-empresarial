package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Pedido de un libro de otra ciudad para entrega a domicilio.
 * Solo lo piden usuarios con suscripcion vigente. Va vinculado a un {@link PrestamoEntity}
 * desde que se solicita; el plazo de devolucion empieza cuando se registra la entrega.
 */
@Entity
@Table(name = "pedidos_interciudad")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoInterciudadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SOLICITADO, EN_TRANSITO, DISPONIBLE_PARA_RETIRO, RETIRADO, CANCELADO. */
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String estado = "SOLICITADO";

    @Column(nullable = false, length = 80)
    private String ciudadEntrega;

    @Column(nullable = false, length = 200)
    private String direccionEntrega;

    @Column(nullable = false)
    private LocalDateTime fechaSolicitud;

    private LocalDateTime fechaTransito;

    private LocalDateTime fechaDisponible;

    /** Momento en que se registra la entrega / retiro del libro. */
    private LocalDateTime fechaRetiro;

    private LocalDateTime fechaCancelacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejemplar_id", nullable = false)
    private EjemplarEntity ejemplar;

    /** Sede donde estaba el ejemplar al solicitarse (a ella debe volver al devolverse). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_origen_id", nullable = false)
    private SedeEntity sedeOrigen;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prestamo_id", nullable = false, unique = true)
    private PrestamoEntity prestamo;

    /** Ultimo bibliotecario que cambio el estado del pedido. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bibliotecario_id")
    private UsuarioEntity bibliotecario;
}