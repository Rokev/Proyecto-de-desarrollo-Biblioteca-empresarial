package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reserva de una {@link SalaEstudio} por parte de un usuario.
 * Alimenta "Ver reservas" y el reporte de uso de salas.
 */
@Entity
@Table(name = "reservas_sala")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaSalaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaInicio;

    @Column(nullable = false)
    private LocalDateTime fechaFin;

    /** PENDIENTE_PAGO, CONFIRMADA, CANCELADA, FINALIZADA. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "PENDIENTE_PAGO";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorBase;

    /** Descuento aplicado si el usuario tiene suscripción activa. */
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    /** Se llena al cancelar y liberar la sala (RF-22). */
    private LocalDateTime fechaCancelacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    private SalaEstudioEntity sala;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "reservaSala", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PagoEntity> pagos = new ArrayList<>();
}
