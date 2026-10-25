package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.PlazoSistemaEntity;

import java.util.Optional;

@Repository
public interface PlazoSistemaRepository extends JpaRepository<PlazoSistemaEntity, Long> {

    Optional<PlazoSistemaEntity> findByClave(String clave);
}

