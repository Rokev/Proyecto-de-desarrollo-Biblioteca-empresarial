package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Plazo configurable del sistema (RF-76): días de préstamo, horas de retiro, días de recogida, etc. */
@Entity
@Table(name = "plazos_sistema",
        uniqueConstraints = @UniqueConstraint(name = "uk_plazo_sistema_clave", columnNames = "clave"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlazoSistemaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String clave;

    @Column(nullable = false)
    private Integer valor;

    /** DIAS u HORAS. */
    @Column(nullable = false, length = 10)
    private String unidad;

    @Column(length = 250)
    private String descripcion;

    private LocalDateTime fechaActualizacion;

    @PrePersist
    @PreUpdate
    void marcarFechaActualizacion() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}

