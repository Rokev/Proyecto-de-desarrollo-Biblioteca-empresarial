package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.ReservaLibroEntity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaLibroRepository extends JpaRepository<ReservaLibroEntity, Long> {

    List<ReservaLibroEntity> findByUsuarioIdOrderByFechaReservaDesc(Long usuarioId);

    List<ReservaLibroEntity> findByUsuarioIdAndEstadoIgnoreCase(Long usuarioId, String estado);

    List<ReservaLibroEntity> findByLibroIdAndSedeIdAndEstadoIgnoreCaseOrderByFechaReservaAsc(Long libroId, Long sedeId, String estado);

    Optional<ReservaLibroEntity> findFirstByLibroIdAndSedeIdAndEstadoIgnoreCaseOrderByFechaReservaAsc(Long libroId, Long sedeId, String estado);

    boolean existsByUsuarioIdAndLibroIdAndEstadoIn(Long usuarioId, Long libroId, Collection<String> estados);

    @Modifying
    @Query("UPDATE ReservaLibroEntity r SET r.estado = 'EXPIRADA' WHERE r.estado = 'NOTIFICADA' AND r.fechaLimiteRetiro < :ahora")
    int expirarVencidas(@Param("ahora") LocalDateTime ahora);
}
