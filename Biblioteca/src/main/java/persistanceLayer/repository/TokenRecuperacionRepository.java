package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.TokenRecuperacionEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacionEntity, Long> {

    Optional<TokenRecuperacionEntity> findByToken(String token);

    Optional<TokenRecuperacionEntity> findByTokenAndUsadoFalse(String token);

    List<TokenRecuperacionEntity> findByUsuarioIdAndUsadoFalse(Long usuarioId);

    /** Marca el token como usado solo si sigue vigente; devuelve 1 si se consumió, 0 si no. */
    @Modifying
    @Query("UPDATE TokenRecuperacionEntity t SET t.usado = true WHERE t.token = :token AND t.usado = false AND t.fechaExpiracion > :ahora")
    int consumirToken(@Param("token") String token, @Param("ahora") LocalDateTime ahora);

    @Modifying
    @Query("UPDATE TokenRecuperacionEntity t SET t.usado = true WHERE t.usuario.id = :usuarioId AND t.usado = false")
    int invalidarTokensDeUsuario(@Param("usuarioId") Long usuarioId);
}
