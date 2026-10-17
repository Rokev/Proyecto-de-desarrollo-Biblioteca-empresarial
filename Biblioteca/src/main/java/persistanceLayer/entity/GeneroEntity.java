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
 * Género literario de un libro (búsqueda por género).
 */
@Entity
@Table(name = "generos")
@Data 
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneroEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String nombre;

    @Column(length = 200)
    private String descripcion;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "genero", fetch = FetchType.LAZY)
    @Builder.Default
    private List<LibroEntity> libros = new ArrayList<>();
}
