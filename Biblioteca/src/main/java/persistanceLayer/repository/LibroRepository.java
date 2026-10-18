package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.LibroEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface LibroRepository extends JpaRepository<LibroEntity, Long> {

    Optional<LibroEntity> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    List<LibroEntity> findByTituloContainingIgnoreCase(String titulo);

    List<LibroEntity> findByIdiomaIgnoreCase(String idioma);

    List<LibroEntity> findByAnioPublicacion(Integer anioPublicacion);

    List<LibroEntity> findByGeneroId(Long generoId);

    List<LibroEntity> findByAutoresId(Long autorId);

    List<LibroEntity> findDistinctByAutoresNombreContainingIgnoreCase(String nombreAutor);

    /** Búsqueda combinada: cada filtro es opcional (null = no filtra). */
    @Query("""
            SELECT DISTINCT l FROM LibroEntity l LEFT JOIN l.autores a
            WHERE (:titulo IS NULL OR LOWER(l.titulo) LIKE LOWER(CONCAT('%', :titulo, '%')))
              AND (:autor IS NULL OR LOWER(a.nombre) LIKE LOWER(CONCAT('%', :autor, '%')))
              AND (:idioma IS NULL OR LOWER(l.idioma) = LOWER(:idioma))
              AND (:anio IS NULL OR l.anioPublicacion = :anio)
              AND (:generoId IS NULL OR l.genero.id = :generoId)
            """)
    List<LibroEntity> buscar(@Param("titulo") String titulo, @Param("autor") String autor, @Param("idioma") String idioma, @Param("anio") Integer anio, @Param("generoId") Long generoId);
}
