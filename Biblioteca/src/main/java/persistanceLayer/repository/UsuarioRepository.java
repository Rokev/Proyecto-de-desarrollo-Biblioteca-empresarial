package persistanceLayer.repository;

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
}

