package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.SuscripcionEntity;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SuscripcionRepository extends JpaRepository<SuscripcionEntity, Long> {

    List<SuscripcionEntity> findByUsuarioId(Long usuarioId);

    List<SuscripcionEntity> findByPlanId(Long planId);

    boolean existsByPlanIdAndEstadoIgnoreCase(Long planId, String estado);

    boolean existsByUsuarioIdAndEstadoIgnoreCase(Long usuarioId, String estado);

    Optional<SuscripcionEntity> findFirstByUsuarioIdAndEstadoInAndFechaFinGreaterThanEqualOrderByFechaFinDesc(Long usuarioId, Collection<String> estados, LocalDate fecha);

    List<SuscripcionEntity> findByEstadoIgnoreCaseAndFechaFinBefore(String estado, LocalDate fecha);
}