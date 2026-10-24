package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.SancionEntity;

import java.util.List;

package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.SancionEntity;

import java.util.List;

@Repository
public interface SancionRepository extends JpaRepository<SancionEntity, Long> {

    List<SancionEntity> findByUsuarioId(Long usuarioId);

    List<SancionEntity> findByUsuarioIdAndEstadoIgnoreCase(Long usuarioId, String estado);

    boolean existsByUsuarioIdAndTipoIgnoreCaseAndEstadoIgnoreCase(Long usuarioId, String tipo, String estado);

    List<SancionEntity> findByPrestamoId(Long prestamoId);

    List<SancionEntity> findByTipoIgnoreCaseAndEstadoIgnoreCase(String tipo, String estado);
}

