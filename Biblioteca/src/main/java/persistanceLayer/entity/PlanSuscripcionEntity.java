package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
/**
 * Plan de suscripción disponible en la plataforma, con su precio, beneficios y vigencia (RF-23, RF-24).
 */
@Entity
@Table(name = "planes_suscripcion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanSuscripcionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String nombre;

    @Column(length = 300)
    private String descripcion;

    /** Beneficios del plan (descuento en salas, plazo extendido, domicilio, eventos, etc.). */
    @Column(columnDefinition = "TEXT")
    private String beneficios;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    /** Vigencia del plan en meses. */
    @Column(nullable = false)
    private Integer duracionMeses;

    /** Descuento en salas de estudio como fracción entre 0 y 1 (0.15 = 15 %).
     *  Se copia a la suscripción al comprar el plan (RF-29). */
    @Column(nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal descuentoSalas = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "plan", fetch = FetchType.LAZY)
    @Builder.Default
    private List<SuscripcionEntity> suscripciones = new ArrayList<>();
}
