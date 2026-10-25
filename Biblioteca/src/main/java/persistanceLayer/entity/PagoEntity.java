package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transacción de pago. Puede corresponder a una multa, a una suscripción o a una
 * reserva de sala: solo una de esas tres relaciones debe estar llena
 * (esa regla se valida en la capa de negocio).
 */
@Entity
@Table(name = "pagos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private LocalDateTime fechaPago;

    /** MULTA, SUSCRIPCION, RESERVA_SALA. */
    @Column(nullable = false, length = 20)
    private String concepto;

    /** PENDIENTE, APROBADO, RECHAZADO, REEMBOLSADO. */
    @Column(nullable = false, length = 20)
    private String estado;

    /** Número de comprobante / referencia de la transacción. */
    @Column(nullable = false, unique = true, length = 60)
    private String referencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "metodo_pago_id", nullable = false)
    private MetodoPagoEntity metodoPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "multa_id")
    private MultaEntity multa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suscripcion_id")
    private SuscripcionEntity suscripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_sala_id")
    private ReservaSalaEntity reservaSala;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_interciudad_id")
    private PedidoInterciudadEntity pedidoInterciudad;
}
