package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

/**
 * Copia física de un {@link Libro} ubicada en una {@link Sede}.
 * Es la que se presta, se devuelve (incluso en otra sede) y determina la disponibilidad.
 */
@Entity
@Table(name = "ejemplares")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EjemplarEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código de barras / inventario de la copia. */
    @Column(nullable = false, unique = true, length = 40)
    private String codigoBarras;

    /** DISPONIBLE, RESERVADO, PRESTADO, EN_TRANSITO, DANADO, etc. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "DISPONIBLE";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "libro_id", nullable = false)
    private LibroEntity libro;

    /** Sede donde se encuentra actualmente la copia. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private SedeEntity sede;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "ejemplar", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PrestamoEntity> prestamos = new ArrayList<>();
}
