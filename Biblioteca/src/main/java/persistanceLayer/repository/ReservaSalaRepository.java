package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.ReservaSalaEntity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface ReservaSalaRepository extends JpaRepository<ReservaSalaEntity, Long> {

    List<ReservaSalaEntity> findByUsuarioIdOrderByFechaInicioDesc(Long usuarioId);

    List<ReservaSalaEntity> findBySalaId(Long salaId);

    List<ReservaSalaEntity> findByEstadoIgnoreCase(String estado);

    boolean existsBySalaIdAndEstadoInAndFechaFinAfter(Long salaId, Collection<String> estados, LocalDateTime ahora);

    /** Reservas vigentes de la sala que se cruzan con el rango [inicio, fin). */
    @Query("""
            SELECT COUNT(r) FROM ReservaSalaEntity r
            WHERE r.sala.id = :salaId
              AND r.estado IN ('PENDIENTE_PAGO', 'CONFIRMADA')
              AND r.fechaInicio < :fin
              AND r.fechaFin > :inicio
            """)
    long contarSolapadas(@Param("salaId") Long salaId, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}