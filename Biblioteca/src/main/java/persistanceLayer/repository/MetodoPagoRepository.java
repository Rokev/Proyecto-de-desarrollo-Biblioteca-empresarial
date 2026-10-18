package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.MetodoPagoEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetodoPagoRepository extends JpaRepository<MetodoPagoEntity, Long> {

    List<MetodoPagoEntity> findByUsuarioId(Long usuarioId);

    Optional<MetodoPagoEntity> findByIdAndUsuarioId(Long id, Long usuarioId);

    Optional<MetodoPagoEntity> findFirstByUsuarioIdAndPredeterminadoTrue(Long usuarioId);

    long countByUsuarioId(Long usuarioId);

    @Modifying
    @Query("UPDATE MetodoPagoEntity m SET m.predeterminado = false WHERE m.usuario.id = :usuarioId")
    int quitarPredeterminado(@Param("usuarioId") Long usuarioId);
}