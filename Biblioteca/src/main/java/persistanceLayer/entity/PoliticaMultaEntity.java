package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Política de multa (RF-75): monto configurado para cada concepto económico. */
@Entity
@Table(name = "politicas_multa",
        uniqueConstraints = @UniqueConstraint(name = "uk_politica_multa_concepto", columnNames = "concepto"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PoliticaMultaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** MORA_POR_DIA, LIBRO_DANADO, LIBRO_PERDIDO, PEDIDO_NO_RECOGIDO, RECARGO_PEDIDO_NO_RECIBIDO. */
    @Column(nullable = false, length = 40)
    private String concepto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(length = 200)
    private String descripcion;

    private LocalDateTime fechaActualizacion;

    @PrePersist
    @PreUpdate
    void marcarFechaActualizacion() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}

