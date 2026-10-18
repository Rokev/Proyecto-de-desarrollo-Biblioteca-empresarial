package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.InscripcionEventoEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface InscripcionEventoRepository extends JpaRepository<InscripcionEventoEntity, Long> {

    List<InscripcionEventoEntity> findByEventoId(Long eventoId);

    List<InscripcionEventoEntity> findByUsuarioId(Long usuarioId);

    Optional<InscripcionEventoEntity> findByEventoIdAndUsuarioId(Long eventoId, Long usuarioId);

    boolean existsByEventoIdAndUsuarioId(Long eventoId, Long usuarioId);

    List<InscripcionEventoEntity> findByEventoIdAndEstadoIgnoreCase(Long eventoId, String estado);

    List<InscripcionEventoEntity> findByUsuarioIdAndEstadoIgnoreCase(Long usuarioId, String estado);
}
