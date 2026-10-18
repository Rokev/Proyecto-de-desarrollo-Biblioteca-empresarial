package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.PlanSuscripcionEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanSuscripcionRepository extends JpaRepository<PlanSuscripcionEntity, Long> {

    Optional<PlanSuscripcionEntity> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    List<PlanSuscripcionEntity> findByActivoTrue();
}
