package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.PagoEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<PagoEntity, Long> {

    List<PagoEntity> findByUsuarioIdOrderByFechaPagoDesc(Long usuarioId);

    Optional<PagoEntity> findByReferencia(String referencia);

    boolean existsByReferencia(String referencia);

    List<PagoEntity> findByMultaId(Long multaId);

    List<PagoEntity> findBySuscripcionId(Long suscripcionId);

    List<PagoEntity> findByReservaSalaId(Long reservaSalaId);

    List<PagoEntity> findByConceptoIgnoreCase(String concepto);

    List<PagoEntity> findByEstadoIgnoreCase(String estado);
}
