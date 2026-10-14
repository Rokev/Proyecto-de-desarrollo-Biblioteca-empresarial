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
 * Sala de estudio perteneciente a una sede. No se puede eliminar si tiene reservas vigentes.
 */
@Entity
@Table(name = "salas_estudio")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaEstudioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false)
    private Integer capacidad;

    @Column(length = 300)
    private String descripcion;

    /** Tarifa normal por hora; a suscriptores se les aplica descuento. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal tarifaPorHora;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private SedeEntity sede;

    @OneToMany(mappedBy = "sala", fetch = FetchType.LAZY)
    @Builder.Default
    private List<ReservaSalaEntity> reservas = new ArrayList<>();
}
