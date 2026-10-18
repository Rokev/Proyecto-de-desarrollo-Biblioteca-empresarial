package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.DevolucionEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface DevolucionesRepository extends JpaRepository<DevolucionEntity, Long> {

    Optional<DevolucionEntity> findByCodigoQr(String codigoQr);

    Optional<DevolucionEntity> findByPrestamoId(Long prestamoId);

    boolean existsByPrestamoId(Long prestamoId);

    List<DevolucionEntity> findBySedeDevolucionId(Long sedeDevolucionId);

    List<DevolucionEntity> findByEstadoIgnoreCase(String estado);

    List<DevolucionEntity> findBySedeDevolucionIdAndEstadoIgnoreCase(Long sedeDevolucionId, String estado);
}

