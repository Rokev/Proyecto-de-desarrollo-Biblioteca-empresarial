package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
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
}
