package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.SesionEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SesionRepository extends JpaRepository<SesionEntity, Long> {

    Optional<SesionEntity> findByToken(String token);


    Optional<SesionEntity> findByTokenAndActivaTrue(String token);

    List<SesionEntity> findByUsuarioIdAndActivaTrue(Long usuarioId);

    @Modifying
    @Query("UPDATE SesionEntity s SET s.activa = false, s.fechaCierre = :ahora WHERE s.token = :token AND s.activa = true")
    int cerrarSesion(@Param("token") String token, @Param("ahora") LocalDateTime ahora);

    @Modifying
    @Query("UPDATE SesionEntity s SET s.activa = false, s.fechaCierre = :ahora WHERE s.activa = true AND s.fechaExpiracion < :ahora")
    int cerrarExpiradas(@Param("ahora") LocalDateTime ahora);
}
