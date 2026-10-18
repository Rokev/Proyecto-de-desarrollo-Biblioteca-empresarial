package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.MultaEntity;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface MultaRepository extends JpaRepository<MultaEntity, Long> {

    List<MultaEntity> findByUsuarioIdOrderByFechaGeneracionDesc(Long usuarioId);

    List<MultaEntity> findByUsuarioIdAndEstadoIgnoreCase(Long usuarioId, String estado);

    List<MultaEntity> findByEstadoIgnoreCase(String estado);

    List<MultaEntity> findByPrestamoId(Long prestamoId);

    @Query("SELECT SUM(m.monto) FROM MultaEntity m WHERE m.usuario.id = :usuarioId AND m.estado = 'PENDIENTE'")
    BigDecimal sumaPendientePorUsuario(@Param("usuarioId") Long usuarioId);
}
