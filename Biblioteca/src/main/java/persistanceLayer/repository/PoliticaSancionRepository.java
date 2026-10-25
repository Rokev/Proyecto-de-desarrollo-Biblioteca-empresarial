package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.PoliticaSancionEntity;

import java.util.Optional;

@Repository
public interface PoliticaSancionRepository extends JpaRepository<PoliticaSancionEntity, Long> {

    Optional<PoliticaSancionEntity> findByInfraccion(String infraccion);
}

