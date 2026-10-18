package persistanceLayer.dao;

import bussinesLayer.dto.SesionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.entity.SesionEntity;
import persistanceLayer.entity.UsuarioEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.SesionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de Sesion: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class SesionDAO {

    private final SesionRepository sesionRepository;
    private final Mapper mapper;

    public SesionDTO save(SesionDTO dto) {
        SesionEntity entity = mapper.sesionToEntity(dto);
        entity.setId(null);
        entity.setUsuario(referenciaUsuario(dto.getUsuarioId()));
        SesionEntity guardado = sesionRepository.save(entity);
        return mapper.sesionToDto(guardado);
    }

    public Optional<SesionDTO> findById(Long id) {
        return sesionRepository.findById(id).map(mapper::sesionToDto);
    }

    public List<SesionDTO> findAll() {
        return sesionRepository.findAll().stream().map(mapper::sesionToDto).toList();
    }

    public Optional<SesionDTO> update(Long id, SesionDTO dto) {
        return sesionRepository.findById(id).map(existente -> {
            existente.setToken(dto.getToken());
            existente.setFechaInicio(dto.getFechaInicio());
            existente.setFechaExpiracion(dto.getFechaExpiracion());
            existente.setFechaCierre(dto.getFechaCierre());
            if (dto.getActiva() != null) {
                existente.setActiva(dto.getActiva());
            }
            existente.setUsuario(referenciaUsuario(dto.getUsuarioId()));
            return mapper.sesionToDto(sesionRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return sesionRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (sesionRepository.existsById(id)) {
            sesionRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<SesionDTO> findByToken(String token) {
        return sesionRepository.findByToken(token).map(mapper::sesionToDto);
    }

    public Optional<SesionDTO> findByTokenAndActivaTrue(String token) {
        return sesionRepository.findByTokenAndActivaTrue(token).map(mapper::sesionToDto);
    }

    public List<SesionDTO> findByUsuarioIdAndActivaTrue(Long usuarioId) {
        return sesionRepository.findByUsuarioIdAndActivaTrue(usuarioId).stream().map(mapper::sesionToDto).toList();
    }

    @Transactional
    public int cerrarSesion(String token, LocalDateTime ahora) {
        return sesionRepository.cerrarSesion(token, ahora);
    }

    @Transactional
    public int cerrarExpiradas(LocalDateTime ahora) {
        return sesionRepository.cerrarExpiradas(ahora);
    }

    private UsuarioEntity referenciaUsuario(Long idUsuario) {
        if (idUsuario == null) {
            return null;
        }
        UsuarioEntity referencia = new UsuarioEntity();
        referencia.setId(idUsuario);
        return referencia;
    }
}
