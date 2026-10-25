package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Registro del log de auditoría ( 79). Cada fila es una acción relevante hecha en el sistema.
 * El usuario se guarda como id + nombre (sin llave foránea) para que el registro se conserve
 * aunque la cuenta se desactive o se modifique después. Los registros nunca se editan ni se borran.
 */
@Entity
@Table(name = "auditoria", indexes = {
        @Index(name = "idx_auditoria_fecha", columnList = "fecha"),
        @Index(name = "idx_auditoria_usuario", columnList = "usuario_id"),
        @Index(name = "idx_auditoria_modulo", columnList = "modulo"),
        @Index(name = "idx_auditoria_accion", columnList = "accion")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    /** Quién realizó la acción (nulo si fue el propio sistema, p. ej. una tarea programada). */
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "usuario_nombre", length = 120)
    private String usuarioNombre;

    /** CREAR, MODIFICAR, DESACTIVAR, REACTIVAR, SUSPENDER, ELIMINAR, GENERAR_REPORTE... */
    @Column(nullable = false, length = 30)
    private String accion;

    /** USUARIOS, SEDES, TARIFAS_ENVIO, PLANES, POLITICAS, REPORTES... */
    @Column(nullable = false, length = 40)
    private String modulo;

    /** Id del registro afectado (opcional). */
    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(nullable = false, length = 500)
    private String descripcion;
}
