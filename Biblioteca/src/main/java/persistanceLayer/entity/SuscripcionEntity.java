package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Suscripción de un usuario a un {@link PlanSuscripcionEntity} (RF-23, RF-24, RF-25).
 * Da descuento en salas, plazo extendido de devolución, libros a domicilio,
 * entrada a eventos y un mes gratis tras 5 meses consecutivos.
 */
@Entity
@Table(name = "suscripciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuscripcionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    /** PENDIENTE_PAGO (creada, aún sin pago exitoso), ACTIVA, PAUSADA,
     *  CANCELADA (el usuario canceló; conserva beneficios hasta fechaFin) o VENCIDA. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "ACTIVA";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorMensual;

    /** Copia del descuento en salas del plan al momento de la compra (fracción entre 0 y 1).
     *  Si el plan cambia después, esta suscripción conserva el valor con el que se compró. */
    @Column(nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal descuentoSalas = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean renovacionAutomatica = true;

    /** Fecha en que el usuario solicitó cancelar. Los beneficios se mantienen hasta
     *  {@code fechaFin} (RF-25). */
    private LocalDateTime fechaCancelacion;

    /** Meses consecutivos pagados; al llegar a 5 se otorga un mes gratis. */
    @Column(nullable = false)
    @Builder.Default
    private Integer mesesConsecutivos = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanSuscripcionEntity plan;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "suscripcion", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PagoEntity> pagos = new ArrayList<>();
}
