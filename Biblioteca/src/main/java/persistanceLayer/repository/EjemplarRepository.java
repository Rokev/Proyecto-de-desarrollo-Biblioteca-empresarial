package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import persistanceLayer.entity.EjemplarEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface EjemplarRepository extends JpaRepository<EjemplarEntity, Long> {

    Optional<EjemplarEntity> findByCodigoBarras(String codigoBarras);

    boolean existsByCodigoBarras(String codigoBarras);

    List<EjemplarEntity> findByLibroId(Long libroId);

    List<EjemplarEntity> findBySedeId(Long sedeId);

    List<EjemplarEntity> findByLibroIdAndSedeId(Long libroId, Long sedeId);

    List<EjemplarEntity> findByLibroIdAndEstadoIgnoreCase(Long libroId, String estado);

    Optional<EjemplarEntity> findFirstByLibroIdAndSedeIdAndEstadoIgnoreCase(Long libroId, Long sedeId, String estado);

    long countByLibroIdAndSedeIdAndEstadoIgnoreCase(Long libroId, Long sedeId, String estado);

    boolean existsBySedeId(Long sedeId);

    /** Ejemplares DISPONIBLES del libro, en sedes en operacion de una ciudad distinta a la indicada. */
    @Query("""
            SELECT e FROM EjemplarEntity e
            WHERE e.libro.id = :libroId
              AND e.estado = 'DISPONIBLE'
              AND e.sede.enOperacion = true
              AND LOWER(e.sede.ciudad) <> LOWER(:ciudad)
            ORDER BY e.id
            """)
    List<EjemplarEntity> findDisponiblesEnOtraCiudad(@Param("libroId") Long libroId, @Param("ciudad") String ciudad);

    /** Cambia el estado de forma atomica solo si esta en el estado esperado; devuelve 0 si no lo estaba. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE EjemplarEntity e SET e.estado = :nuevo WHERE e.id = :id AND e.estado = :esperado")
    int cambiarEstadoSiEs(@Param("id") Long id, @Param("esperado") String esperado, @Param("nuevo") String nuevo);
}
