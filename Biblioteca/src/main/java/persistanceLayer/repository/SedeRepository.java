package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.SedeEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface SedeRepository extends JpaRepository<SedeEntity, Long> {

    Optional<SedeEntity> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    List<SedeEntity> findByCiudadIgnoreCase(String ciudad);

    List<SedeEntity> findByEnOperacionTrue();

    List<SedeEntity> findByAliada(Boolean aliada);
}