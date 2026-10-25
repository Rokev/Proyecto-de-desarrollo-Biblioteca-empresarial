package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.AuditoriaEntity;

@Repository
public interface AuditoriaRepository extends JpaRepository<AuditoriaEntity, Long>, JpaSpecificationExecutor<AuditoriaEntity> {
}
