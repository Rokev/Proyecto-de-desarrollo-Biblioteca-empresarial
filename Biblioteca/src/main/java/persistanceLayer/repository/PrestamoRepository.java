package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.PrestamoEntity;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface PrestamoRepository extends JpaRepository<PrestamoEntity, Long> {

    List<PrestamoEntity> findByUsuarioIdOrderByFechaSolicitudDesc(Long usuarioId);

    List<PrestamoEntity> findByUsuarioIdAndEstadoIgnoreCase(Long usuarioId, String estado);

    List<PrestamoEntity> findByUsuarioIdAndEstadoIn(Long usuarioId, Collection<String> estados);

    long countByUsuarioIdAndEstadoIn(Long usuarioId, Collection<String> estados);

    List<PrestamoEntity> findByEjemplarId(Long ejemplarId);

    /** Hay prestamos en esos estados sobre algun ejemplar del libro. */
    boolean existsByEjemplarLibroIdAndEstadoIn(Long libroId, Collection<String> estados);

    boolean existsByEjemplarIdAndEstadoIn(Long ejemplarId, Collection<String> estados);

    List<PrestamoEntity> findBySedeRetiroId(Long sedeRetiroId);

    List<PrestamoEntity> findByEstadoIgnoreCase(String estado);

    List<PrestamoEntity> findByEstadoIgnoreCaseAndFechaVencimientoBefore(String estado, LocalDate fecha);

    List<PrestamoEntity> findByEstadoIgnoreCaseAndFechaVencimiento(String estado, LocalDate fechaVencimiento);

    @Modifying
    @Query("UPDATE PrestamoEntity p SET p.estado = 'VENCIDO' WHERE p.estado = 'ACTIVO' AND p.fechaVencimiento < :hoy")
    int marcarVencidos(@Param("hoy") LocalDate hoy);
}
