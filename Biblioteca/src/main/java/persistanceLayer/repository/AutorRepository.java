package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.AutorEntity;

import java.util.List;

@Repository
public interface AutorRepository extends JpaRepository<AutorEntity, Long> {

    List<AutorEntity> findByNombreContainingIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);
}