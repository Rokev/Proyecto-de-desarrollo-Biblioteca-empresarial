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

    /** Momento en que el usuario solicita el préstamo (RF-07). */
    @Column(nullable = false)
    private LocalDateTime fechaSolicitud;

    /** Momento en que se registra la entrega del libro; desde aquí corre el plazo (RF-07 / RF-48). */
    private LocalDateTime fechaEntrega;

    /** Fecha límite de devolución (más extensa si el usuario tiene suscripción).
     *  Nula hasta que se registre la entrega. */
    private LocalDate fechaVencimiento;

    /** Nula mientras el préstamo siga activo. */
    private LocalDateTime fechaDevolucion;

    /** SOLICITADO, ACTIVO, DEVUELTO, VENCIDO, CANCELADO. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "SOLICITADO";

    /** SEDE (recogida en la biblioteca), DOMICILIO o INTERCIUDAD (beneficios de suscripción). */
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

    /** Sede donde el usuario retira el libro (RF-12 muestra libro, sede y estado). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_retiro_id", nullable = false)
    private SedeEntity sedeRetiro;

    /** Solicitud de devolución en sede diferente (opcional). */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToOne(mappedBy = "prestamo", fetch = FetchType.LAZY)
    private DevolucionEntity devolucion;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "prestamo", fetch = FetchType.LAZY)
    @Builder.Default
    private List<MultaEntity> multas = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "prestamo", fetch = FetchType.LAZY)
    @Builder.Default
    private List<SancionEntity> sanciones = new ArrayList<>();


}
