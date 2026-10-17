package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Devolución de un préstamo en una sede (normalmente distinta a la de origen).
 * Genera un código / QR que el usuario presenta junto con el libro.
 */
@Entity
@Table(name = "devoluciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DevolucionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código alfanumérico o contenido del QR. */
    @Column(nullable = false, unique = true, length = 60)
    private String codigoQr;

    @Column(nullable = false)
    private LocalDateTime fechaSolicitud;

    /** Se llena cuando el bibliotecario registra la devolución en la sede receptora. */
    private LocalDateTime fechaConfirmacion;

    /** PENDIENTE, CONFIRMADA, RECHAZADA. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "PENDIENTE";

    @Column(length = 300)
    private String observaciones;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prestamo_id", nullable = false, unique = true)
    private PrestamoEntity prestamo;

    /** Sede donde se entrega el libro. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_devolucion_id", nullable = false)
    private SedeEntity sedeDevolucion;

    /** Bibliotecario que registra la devolución (RF-08 / RF-62). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bibliotecario_id")
    private UsuarioEntity bibliotecario;
}
