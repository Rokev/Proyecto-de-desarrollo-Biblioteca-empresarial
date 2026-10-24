package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.PedidoInterciudadEntity;

import java.util.Collection;
import java.util.List;

@Repository
public interface PedidoInterciudadRepository extends JpaRepository<PedidoInterciudadEntity, Long> {

    List<PedidoInterciudadEntity> findByUsuarioIdOrderByFechaSolicitudDesc(Long usuarioId);

    List<PedidoInterciudadEntity> findByUsuarioIdAndEstadoIgnoreCaseOrderByFechaSolicitudDesc(Long usuarioId, String estado);

    List<PedidoInterciudadEntity> findByEstadoIgnoreCaseOrderByFechaSolicitudAsc(String estado);

    List<PedidoInterciudadEntity> findByRepartidorIdAndEstadoInOrderByFechaSolicitudAsc(Long repartidorId, Collection<String> estados);

    List<PedidoInterciudadEntity> findBySedeOrigenIdAndEstadoInOrderByFechaSolicitudAsc(Long sedeOrigenId, Collection<String> estados);

    List<PedidoInterciudadEntity> findBySedeDevolucionIdAndEstadoInOrderByFechaSolicitudAsc(Long sedeDevolucionId, Collection<String> estados);
}