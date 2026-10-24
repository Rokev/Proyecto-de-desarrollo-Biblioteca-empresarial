package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.TarifaEnvioEntity;

import java.util.Optional;

@Repository
public interface TarifaEnvioRepository extends JpaRepository<TarifaEnvioEntity, Long> {

    @Query("SELECT t FROM TarifaEnvioEntity t WHERE t.ciudadA = :a AND t.ciudadB = :b")
    Optional<TarifaEnvioEntity> findByPar(@Param("a") String ciudadA, @Param("b") String ciudadB);
}