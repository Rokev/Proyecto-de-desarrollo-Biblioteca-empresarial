package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Préstamo de un {@link Ejemplar} a un {@link Usuario}.
 * Alimenta "Historial de préstamos" y "Préstamos activos".
 */
@Entity
@Table(name = "prestamos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrestamoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaPrestamo;

    /** Fecha límite de devolución (más extensa si el usuario tiene suscripción). */
    @Column(nullable = false)
    private LocalDate fechaVencimiento;

    /** Nula mientras el préstamo siga activo. */
    private LocalDateTime fechaDevolucion;

    /** ACTIVO, DEVUELTO, VENCIDO. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "ACTIVO";

    /** SEDE (recogida en la biblioteca) o DOMICILIO (beneficio de suscripción). */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String modalidad = "SEDE";

    /** Solo si la modalidad es DOMICILIO. */
    @Column(length = 200)
    private String direccionEntrega;

    @Column(nullable = false)
    @Builder.Default
    private Integer vecesRenovado = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejemplar_id", nullable = false)
    private EjemplarEntity ejemplar;

    /** Solicitud de devolución en sede diferente (opcional). */
    @OneToOne(mappedBy = "prestamo", fetch = FetchType.LAZY)
    private DevolucionEntity devolucion;

    @OneToMany(mappedBy = "prestamo", fetch = FetchType.LAZY)
    @Builder.Default
    private List<MultaEntity> multas = new ArrayList<>();
}
