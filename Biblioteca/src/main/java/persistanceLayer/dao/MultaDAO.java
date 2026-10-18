package persistanceLayer.dao;

import bussinesLayer.dto.MultaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.MultaEntity;
import persistanceLayer.entity.PrestamoEntity;
import persistanceLayer.entity.UsuarioEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.MultaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de Multa: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class MultaDAO {

    private final MultaRepository multaRepository;
    private final Mapper mapper;

    public MultaDTO save(MultaDTO dto) {
        MultaEntity entity = mapper.multaToEntity(dto);
        entity.setId(null);
        entity.setUsuario(referenciaUsuario(dto.getUsuarioId()));
        entity.setPrestamo(referenciaPrestamo(dto.getPrestamoId()));
        MultaEntity guardado = multaRepository.save(entity);
        return mapper.multaToDto(guardado);
    }

    public Optional<MultaDTO> findById(Long id) {
        return multaRepository.findById(id).map(mapper::multaToDto);
    }

    public List<MultaDTO> findAll() {
        return multaRepository.findAll().stream().map(mapper::multaToDto).toList();
    }

    public Optional<MultaDTO> update(Long id, MultaDTO dto) {
        return multaRepository.findById(id).map(existente -> {
            existente.setMonto(dto.getMonto());
            existente.setMotivo(dto.getMotivo());
            existente.setFechaGeneracion(dto.getFechaGeneracion());
            if (dto.getEstado() != null) {
                existente.setEstado(dto.getEstado());
            }
            existente.setUsuario(referenciaUsuario(dto.getUsuarioId()));
            existente.setPrestamo(referenciaPrestamo(dto.getPrestamoId()));
            return mapper.multaToDto(multaRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return multaRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (multaRepository.existsById(id)) {
            multaRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<MultaDTO> findByUsuarioIdOrderByFechaGeneracionDesc(Long usuarioId) {
        return multaRepository.findByUsuarioIdOrderByFechaGeneracionDesc(usuarioId).stream().map(mapper::multaToDto).toList();
    }

    public List<MultaDTO> findByUsuarioIdAndEstadoIgnoreCase(Long usuarioId, String estado) {
        return multaRepository.findByUsuarioIdAndEstadoIgnoreCase(usuarioId, estado).stream().map(mapper::multaToDto).toList();
    }

    public List<MultaDTO> findByEstadoIgnoreCase(String estado) {
        return multaRepository.findByEstadoIgnoreCase(estado).stream().map(mapper::multaToDto).toList();
    }

    public List<MultaDTO> findByPrestamoId(Long prestamoId) {
        return multaRepository.findByPrestamoId(prestamoId).stream().map(mapper::multaToDto).toList();
    }

    /** Suma de las multas pendientes del usuario (0 si no tiene). */
    public BigDecimal totalPendientePorUsuario(Long usuarioId) {
        BigDecimal total = multaRepository.sumaPendientePorUsuario(usuarioId);
        return total != null ? total : BigDecimal.ZERO;
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
