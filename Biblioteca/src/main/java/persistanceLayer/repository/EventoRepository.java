package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.EventoEntity;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventoRepository extends JpaRepository<EventoEntity, Long> {

    List<EventoEntity> findBySedeIdOrderByFechaInicioAsc(Long sedeId);

    List<EventoEntity> findByEstadoIgnoreCase(String estado);

    List<EventoEntity> findByFechaInicioGreaterThanEqualAndEstadoIgnoreCaseOrderByFechaInicioAsc(LocalDateTime desde, String estado);

    /** Descuenta cupos de forma atómica; devuelve 0 si no alcanzan o el evento no está programado. */
    @Modifying
    @Query("UPDATE EventoEntity e SET e.cuposDisponibles = e.cuposDisponibles - :cantidad WHERE e.id = :id AND e.estado = 'PROGRAMADO' AND e.cuposDisponibles >= :cantidad")
    int descontarCupos(@Param("id") Long id, @Param("cantidad") int cantidad);

    /** Devuelve cupos al cancelar una inscripción sin pasarse del cupo máximo. */
    @Modifying
    @Query("UPDATE EventoEntity e SET e.cuposDisponibles = e.cuposDisponibles + :cantidad WHERE e.id = :id AND e.cuposDisponibles + :cantidad <= e.cupoMaximo")
    int liberarCupos(@Param("id") Long id, @Param("cantidad") int cantidad);
}
