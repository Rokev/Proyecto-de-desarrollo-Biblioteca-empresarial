package persistanceLayer.dao;

import bussinesLayer.dto.UsuarioDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.RolEntity;
import persistanceLayer.entity.SedeEntity;
import persistanceLayer.entity.UsuarioEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de Usuario: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class UsuarioDAO {

    private final UsuarioRepository usuarioRepository;
    private final Mapper mapper;

    public UsuarioDTO save(UsuarioDTO dto) {
        UsuarioEntity entity = mapper.usuarioToEntity(dto);
        entity.setId(null);
        if (entity.getFechaRegistro() == null) {
            entity.setFechaRegistro(LocalDateTime.now());
        }
        entity.setRol(referenciaRol(dto.getRolId()));
        entity.setSede(referenciaSede(dto.getSedeId()));
        UsuarioEntity guardado = usuarioRepository.save(entity);
        return mapper.usuarioToDto(guardado);
    }

    public Optional<UsuarioDTO> findById(Long id) {
        return usuarioRepository.findById(id).map(mapper::usuarioToDto);
    }

    public List<UsuarioDTO> findAll() {
        return usuarioRepository.findAll().stream().map(mapper::usuarioToDto).toList();
    }

    public Optional<UsuarioDTO> update(Long id, UsuarioDTO dto) {
        return usuarioRepository.findById(id).map(existente -> {
            existente.setNombre(dto.getNombre());
            existente.setApellido(dto.getApellido());
            existente.setDocumento(dto.getDocumento());
            existente.setNombreUsuario(dto.getNombreUsuario());
            existente.setCorreo(dto.getCorreo());
            if (dto.getContrasena() != null) {
                existente.setContrasena(dto.getContrasena());
            }
            existente.setTelefono(dto.getTelefono());
            existente.setDireccion(dto.getDireccion());
            if (dto.getActivo() != null) {
                existente.setActivo(dto.getActivo());
            }
            existente.setRol(referenciaRol(dto.getRolId()));
            existente.setSede(referenciaSede(dto.getSedeId()));
            return mapper.usuarioToDto(usuarioRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return usuarioRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (usuarioRepository.existsById(id)) {
            usuarioRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<UsuarioDTO> findByNombreUsuarioIgnoreCase(String nombreUsuario) {
        return usuarioRepository.findByNombreUsuarioIgnoreCase(nombreUsuario).map(mapper::usuarioToDto);
    }

    public Optional<UsuarioDTO> findByCorreoIgnoreCase(String correo) {
        return usuarioRepository.findByCorreoIgnoreCase(correo).map(mapper::usuarioToDto);
    }

    public Optional<UsuarioDTO> findByDocumento(String documento) {
        return usuarioRepository.findByDocumento(documento).map(mapper::usuarioToDto);
    }

    public boolean existsByNombreUsuarioIgnoreCase(String nombreUsuario) {
        return usuarioRepository.existsByNombreUsuarioIgnoreCase(nombreUsuario);
    }

    public boolean existsByCorreoIgnoreCase(String correo) {
        return usuarioRepository.existsByCorreoIgnoreCase(correo);
    }

    public boolean existsByDocumento(String documento) {
        return usuarioRepository.existsByDocumento(documento);
    }

    public List<UsuarioDTO> findByRolId(Long rolId) {
        return usuarioRepository.findByRolId(rolId).stream().map(mapper::usuarioToDto).toList();
    }

    public List<UsuarioDTO> findBySedeId(Long sedeId) {
        return usuarioRepository.findBySedeId(sedeId).stream().map(mapper::usuarioToDto).toList();
    }

    public List<UsuarioDTO> findBySedeIdAndRolNombreIgnoreCase(Long sedeId, String rol) {
        return usuarioRepository.findBySedeIdAndRolNombreIgnoreCase(sedeId, rol).stream().map(mapper::usuarioToDto).toList();
    }

    public List<UsuarioDTO> findByActivoTrue() {
        return usuarioRepository.findByActivoTrue().stream().map(mapper::usuarioToDto).toList();
    }

    /** Reemplaza el hash de la contraseña (el hash lo calcula la capa de negocio). */
    public boolean updateContrasena(Long id, String contrasenaHash) {
        return usuarioRepository.findById(id).map(usuario -> {
            usuario.setContrasena(contrasenaHash);
            usuarioRepository.save(usuario);
            return true;
        }).orElse(false);
    }

    private RolEntity referenciaRol(Long idRol) {
        if (idRol == null) {
            return null;
        }
        RolEntity referencia = new RolEntity();
        referencia.setId(idRol);
        return referencia;
    }

    private SedeEntity referenciaSede(Long idSede) {
        if (idSede == null) {
            return null;
        }
        SedeEntity referencia = new SedeEntity();
        referencia.setId(idSede);
        return referencia;
    }

    public List<UsuarioDTO> filtrar(String rol, Long sedeId, Boolean activo) {
        return usuarioRepository.filtrar(rol, sedeId, activo).stream().map(mapper::usuarioToDto).toList();
    }
}
