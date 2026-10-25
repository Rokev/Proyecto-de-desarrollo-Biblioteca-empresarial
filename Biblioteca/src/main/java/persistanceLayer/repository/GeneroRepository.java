package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.GeneroEntity;

import java.util.Optional;

@Repository
public interface GeneroRepository extends JpaRepository<GeneroEntity, Long> {

    Optional<GeneroEntity> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);
}
