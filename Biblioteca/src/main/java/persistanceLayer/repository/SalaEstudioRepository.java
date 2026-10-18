package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.SalaEstudioEntity;

import java.util.List;

@Repository
public interface SalaEstudioRepository extends JpaRepository<SalaEstudioEntity, Long> {

    List<SalaEstudioEntity> findBySedeId(Long sedeId);

    List<SalaEstudioEntity> findBySedeIdAndActivaTrue(Long sedeId);

    List<SalaEstudioEntity> findByActivaTrue();

    List<SalaEstudioEntity> findByCapacidadGreaterThanEqualAndActivaTrue(Integer capacidad);

    boolean existsBySedeIdAndNombreIgnoreCase(Long sedeId, String nombre);
}
