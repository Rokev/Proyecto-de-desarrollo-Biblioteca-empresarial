package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.JustificacionRetrasoEntity;

import java.util.List;

@Repository
public interface JustificacionRetrasoRepository extends JpaRepository<JustificacionRetrasoEntity, Long> {

    List<JustificacionRetrasoEntity> findByPrestamoId(Long prestamoId);

    List<JustificacionRetrasoEntity> findByUsuarioId(Long usuarioId);

    List<JustificacionRetrasoEntity> findByBibliotecarioId(Long bibliotecarioId);

    List<JustificacionRetrasoEntity> findByEstadoIgnoreCase(String estado);

    List<JustificacionRetrasoEntity> findByBibliotecarioIdAndEstadoIgnoreCase(Long bibliotecarioId, String estado);
}