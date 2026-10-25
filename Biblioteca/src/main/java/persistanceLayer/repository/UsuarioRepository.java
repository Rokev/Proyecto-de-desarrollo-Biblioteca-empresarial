package persistanceLayer.repository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.UsuarioEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {

    Optional<UsuarioEntity> findByNombreUsuarioIgnoreCase(String nombreUsuario);

    Optional<UsuarioEntity> findByCorreoIgnoreCase(String correo);

    Optional<UsuarioEntity> findByDocumento(String documento);

    boolean existsByNombreUsuarioIgnoreCase(String nombreUsuario);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByDocumento(String documento);

    List<UsuarioEntity> findByRolId(Long rolId);

    List<UsuarioEntity> findBySedeId(Long sedeId);

    List<UsuarioEntity> findBySedeIdAndRolNombreIgnoreCase(Long sedeId, String rol);

    List<UsuarioEntity> findByActivoTrue();

    @Query("""
            SELECT u FROM UsuarioEntity u
            WHERE (:rol IS NULL OR LOWER(u.rol.nombre) = LOWER(:rol))
              AND (:sedeId IS NULL OR u.sede.id = :sedeId)
              AND (:activo IS NULL OR u.activo = :activo)
            ORDER BY u.id
            """)
    List<UsuarioEntity> filtrar(@Param("rol") String rol, @Param("sedeId") Long sedeId, @Param("activo") Boolean activo);
}
