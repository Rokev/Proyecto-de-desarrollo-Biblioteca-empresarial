package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.TrasladoRetornoEntity;

import java.util.List;

@Repository
public interface TrasladoRetornoRepository extends JpaRepository<TrasladoRetornoEntity, Long> {

    List<TrasladoRetornoEntity> findBySedeDestinoIdOrderByFechaCreacionAsc(Long sedeDestinoId);

    List<TrasladoRetornoEntity> findBySedeDestinoIdAndEstadoIgnoreCaseOrderByFechaCreacionAsc(Long sedeDestinoId, String estado);
}