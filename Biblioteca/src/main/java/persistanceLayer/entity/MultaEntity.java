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
 * Multa generada a un usuario, normalmente por devolución atrasada.
 * Alimenta "Multas y pagos", el historial de multas y el reporte de multas pendientes.
 */
@Entity
@Table(name = "multas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MultaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 200)
    private String motivo;

    @Column(nullable = false)
    private LocalDateTime fechaGeneracion;

    /** PENDIENTE, PAGADA. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "PENDIENTE";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    /** Préstamo que originó la multa (puede ser nulo si es otro tipo de sanción). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestamo_id")
    private PrestamoEntity prestamo;

    @OneToMany(mappedBy = "multa", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PagoEntity> pagos = new ArrayList<>();
}
