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
 * Título del catálogo (datos bibliográficos). Las copias físicas por sede
 * se representan con {@link Ejemplar}.
 * Permite buscar por título, autor, idioma, año de publicación y género.
 */
@Entity
@Table(name = "libros")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibroEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Evita libros duplicados al crear. */
    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, length = 40)
    private String idioma;

    @Column(nullable = false)
    private Integer anioPublicacion;

    @Column(length = 120)
    private String editorial;

    @Column(columnDefinition = "TEXT")
    private String sinopsis;

    private Integer numeroPaginas;

    @Column(length = 300)
    private String portadaUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "genero_id", nullable = false)
    private GeneroEntity genero;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "libro_autor",
            joinColumns = @JoinColumn(name = "libro_id"),
            inverseJoinColumns = @JoinColumn(name = "autor_id"))
    @Builder.Default
    private List<AutorEntity> autores = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "libro", fetch = FetchType.LAZY)
    @Builder.Default
    private List<EjemplarEntity> ejemplares = new ArrayList<>();
}
