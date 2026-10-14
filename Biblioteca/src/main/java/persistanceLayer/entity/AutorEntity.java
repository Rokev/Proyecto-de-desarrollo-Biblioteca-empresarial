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
 * Autor de uno o varios libros del catálogo (búsqueda por autor).
 */
@Entity
@Table(name = "autores")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 80)
    private String nacionalidad;

    @Column(columnDefinition = "TEXT")
    private String biografia;

    @ManyToMany(mappedBy = "autores", fetch = FetchType.LAZY)
    @Builder.Default
    private List<LibroEntity> libros = new ArrayList<>();
}
