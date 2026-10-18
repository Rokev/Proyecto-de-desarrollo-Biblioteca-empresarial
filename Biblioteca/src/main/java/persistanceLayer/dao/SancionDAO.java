package persistanceLayer.dao;

import bussinesLayer.dto.SancionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.PrestamoEntity;
import persistanceLayer.entity.SancionEntity;
import persistanceLayer.entity.UsuarioEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.SancionRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de Sancion: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class SancionDAO {

    private final SancionRepository sancionRepository;
    private final Mapper mapper;

    public SancionDTO save(SancionDTO dto) {
        SancionEntity entity = mapper.sancionToEntity(dto);
        entity.setId(null);
        entity.setUsuario(referenciaUsuario(dto.getUsuarioId()));
        entity.setPrestamo(referenciaPrestamo(dto.getPrestamoId()));
        SancionEntity guardado = sancionRepository.save(entity);
        return mapper.sancionToDto(guardado);
    }

    public Optional<SancionDTO> findById(Long id) {
        return sancionRepository.findById(id).map(mapper::sancionToDto);
    }

    public List<SancionDTO> findAll() {
        return sancionRepository.findAll().stream().map(mapper::sancionToDto).toList();
    }

    public Optional<SancionDTO> update(Long id, SancionDTO dto) {
        return sancionRepository.findById(id).map(existente -> {
            existente.setTipo(dto.getTipo());
            existente.setMotivo(dto.getMotivo());
            existente.setFechaAplicacion(dto.getFechaAplicacion());
            existente.setFechaFin(dto.getFechaFin());
            if (dto.getEstado() != null) {
                existente.setEstado(dto.getEstado());
            }
            existente.setUsuario(referenciaUsuario(dto.getUsuarioId()));
            existente.setPrestamo(referenciaPrestamo(dto.getPrestamoId()));
            return mapper.sancionToDto(sancionRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return sancionRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (sancionRepository.existsById(id)) {
            sancionRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<SancionDTO> findByUsuarioId(Long usuarioId) {
        return sancionRepository.findByUsuarioId(usuarioId).stream().map(mapper::sancionToDto).toList();
    }

    public List<SancionDTO> findByUsuarioIdAndEstadoIgnoreCase(Long usuarioId, String estado) {
        return sancionRepository.findByUsuarioIdAndEstadoIgnoreCase(usuarioId, estado).stream().map(mapper::sancionToDto).toList();
    }

    public boolean existsByUsuarioIdAndTipoIgnoreCaseAndEstadoIgnoreCase(Long usuarioId, String tipo, String estado) {
        return sancionRepository.existsByUsuarioIdAndTipoIgnoreCaseAndEstadoIgnoreCase(usuarioId, tipo, estado);
    }

    public List<SancionDTO> findByPrestamoId(Long prestamoId) {
        return sancionRepository.findByPrestamoId(prestamoId).stream().map(mapper::sancionToDto).toList();
    }

    private UsuarioEntity referenciaUsuario(Long idUsuario) {
        if (idUsuario == null) {
            return null;
        }
        UsuarioEntity referencia = new UsuarioEntity();
        referencia.setId(idUsuario);
        return referencia;
    }

    private PrestamoEntity referenciaPrestamo(Long idPrestamo) {
        if (idPrestamo == null) {
            return null;
        }
        PrestamoEntity referencia = new PrestamoEntity();
        referencia.setId(idPrestamo);
        return referencia;
    }
}
