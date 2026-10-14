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
import java.util.ArrayList;
import java.util.List;

/**
 * Suscripción Premium de un usuario (existe un único plan, por eso no hay entidad Plan).
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

    /** ACTIVA, PAUSADA, CANCELADA, VENCIDA. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "ACTIVA";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorMensual;

    @Column(nullable = false)
    @Builder.Default
    private Boolean renovacionAutomatica = true;

    /** Meses consecutivos pagados; al llegar a 5 se otorga un mes gratis. */
    @Column(nullable = false)
    @Builder.Default
    private Integer mesesConsecutivos = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @OneToMany(mappedBy = "suscripcion", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PagoEntity> pagos = new ArrayList<>();
}
