package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Política de sanción ( 74): qué restricción se aplica ante cada infracción y por cuánto tiempo. */
@Entity
@Table(name = "politicas_sancion",
        uniqueConstraints = @UniqueConstraint(name = "uk_politica_sancion_infraccion", columnNames = "infraccion"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PoliticaSancionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** MORA_DEVOLUCION, PEDIDO_NO_RECOGIDO, LIBRO_DANADO, LIBRO_PERDIDO. */
    @Column(nullable = false, length = 40)
    private String infraccion;

    /** BLOQUEO_PRESTAMOS, SUSPENSION_CUENTA o ADVERTENCIA. */
    @Column(name = "tipo_sancion", nullable = false, length = 40)
    private String tipoSancion;

    /** Días que dura la restricción. Nulo = hasta devolver el libro (solo para la mora). */
    private Integer duracionDias;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;

    @Column(length = 200)
    private String descripcion;

    private LocalDateTime fechaActualizacion;

    @PrePersist
    @PreUpdate
    void marcarFechaActualizacion() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
