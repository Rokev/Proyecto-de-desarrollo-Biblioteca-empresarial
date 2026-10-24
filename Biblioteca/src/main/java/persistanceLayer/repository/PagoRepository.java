package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.PagoEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /** Pagos de envío aprobados de un pedido (APROBADO desde la pasarela simulada, EXITOSO desde el CRUD). */
    @Query("SELECT p FROM PagoEntity p WHERE p.pedidoInterciudad.id = :pedidoId "
            + "AND UPPER(p.concepto) = 'ENVIO_INTERCIUDAD' AND UPPER(p.estado) IN ('APROBADO', 'EXITOSO') "
            + "ORDER BY p.id DESC")
    List<PagoEntity> findEnviosPagadosPorPedido(@Param("pedidoId") Long pedidoId);
}
