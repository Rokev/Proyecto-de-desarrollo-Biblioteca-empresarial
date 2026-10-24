package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "tarifas_envio",
        uniqueConstraints = @UniqueConstraint(name = "uk_tarifa_par_ciudades", columnNames = {"ciudad_a", "ciudad_b"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarifaEnvioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ciudad_a", nullable = false, length = 80)
    private String ciudadA;

    @Column(name = "ciudad_b", nullable = false, length = 80)
    private String ciudadB;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal tarifa;

    private LocalDateTime fechaActualizacion;

    @PrePersist
    @PreUpdate
    void marcarFechaActualizacion() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}