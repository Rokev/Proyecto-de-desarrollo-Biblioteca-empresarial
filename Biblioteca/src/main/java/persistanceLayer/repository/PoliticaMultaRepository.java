package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.PoliticaMultaEntity;

import java.util.Optional;

@Repository
public interface PoliticaMultaRepository extends JpaRepository<PoliticaMultaEntity, Long> {

    Optional<PoliticaMultaEntity> findByConcepto(String concepto);
}
