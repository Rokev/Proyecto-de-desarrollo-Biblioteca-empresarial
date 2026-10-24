package persistanceLayer.dao;

import bussinesLayer.dto.PrestamoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.entity.EjemplarEntity;
import persistanceLayer.entity.PrestamoEntity;
import persistanceLayer.entity.SedeEntity;
import persistanceLayer.entity.UsuarioEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.PrestamoRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de Prestamo: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class PrestamoDAO {

    private final PrestamoRepository prestamoRepository;
    private final Mapper mapper;

    public PrestamoDTO save(PrestamoDTO dto) {
        PrestamoEntity entity = mapper.prestamoToEntity(dto);
        entity.setId(null);
        entity.setUsuario(referenciaUsuario(dto.getUsuarioId()));
        entity.setEjemplar(referenciaEjemplar(dto.getEjemplarId()));
        entity.setSedeRetiro(referenciaSedeRetiro(dto.getSedeRetiroId()));
        PrestamoEntity guardado = prestamoRepository.save(entity);
        return mapper.prestamoToDto(guardado);
    }

    public Optional<PrestamoDTO> findById(Long id) {
        return prestamoRepository.findById(id).map(mapper::prestamoToDto);
    }

    public List<PrestamoDTO> findAll() {
        return prestamoRepository.findAll().stream().map(mapper::prestamoToDto).toList();
    }

    public Optional<PrestamoDTO> update(Long id, PrestamoDTO dto) {
        return prestamoRepository.findById(id).map(existente -> {
            existente.setFechaSolicitud(dto.getFechaSolicitud());
            existente.setFechaEntrega(dto.getFechaEntrega());
            existente.setFechaVencimiento(dto.getFechaVencimiento());
            existente.setFechaDevolucion(dto.getFechaDevolucion());
            if (dto.getEstado() != null) {
                existente.setEstado(dto.getEstado());
            }
            if (dto.getModalidad() != null) {
                existente.setModalidad(dto.getModalidad());
            }
            existente.setDireccionEntrega(dto.getDireccionEntrega());
            if (dto.getVecesRenovado() != null) {
                existente.setVecesRenovado(dto.getVecesRenovado());
            }
            existente.setUsuario(referenciaUsuario(dto.getUsuarioId()));
            existente.setEjemplar(referenciaEjemplar(dto.getEjemplarId()));
            existente.setSedeRetiro(referenciaSedeRetiro(dto.getSedeRetiroId()));
            return mapper.prestamoToDto(prestamoRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return prestamoRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (prestamoRepository.existsById(id)) {
            prestamoRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<PrestamoDTO> findByUsuarioIdOrderByFechaSolicitudDesc(Long usuarioId) {
        return prestamoRepository.findByUsuarioIdOrderByFechaSolicitudDesc(usuarioId).stream().map(mapper::prestamoToDto).toList();
    }

    public List<PrestamoDTO> findByUsuarioIdAndEstadoIgnoreCase(Long usuarioId, String estado) {
        return prestamoRepository.findByUsuarioIdAndEstadoIgnoreCase(usuarioId, estado).stream().map(mapper::prestamoToDto).toList();
    }

    public List<PrestamoDTO> findByUsuarioIdAndEstadoIn(Long usuarioId, Collection<String> estados) {
        return prestamoRepository.findByUsuarioIdAndEstadoIn(usuarioId, estados).stream().map(mapper::prestamoToDto).toList();
    }

    public long countByUsuarioIdAndEstadoIn(Long usuarioId, Collection<String> estados) {
        return prestamoRepository.countByUsuarioIdAndEstadoIn(usuarioId, estados);
    }

    public List<PrestamoDTO> findByEjemplarId(Long ejemplarId) {
        return prestamoRepository.findByEjemplarId(ejemplarId).stream().map(mapper::prestamoToDto).toList();
    }

    public boolean existsByEjemplarIdAndEstadoIn(Long ejemplarId, Collection<String> estados) {
        return prestamoRepository.existsByEjemplarIdAndEstadoIn(ejemplarId, estados);
    }

    public List<PrestamoDTO> findBySedeRetiroId(Long sedeRetiroId) {
        return prestamoRepository.findBySedeRetiroId(sedeRetiroId).stream().map(mapper::prestamoToDto).toList();
    }

    public List<PrestamoDTO> findByEstadoIgnoreCase(String estado) {
        return prestamoRepository.findByEstadoIgnoreCase(estado).stream().map(mapper::prestamoToDto).toList();
    }

    public List<PrestamoDTO> findByEstadoIgnoreCaseAndFechaVencimientoBefore(String estado, LocalDate fecha) {
        return prestamoRepository.findByEstadoIgnoreCaseAndFechaVencimientoBefore(estado, fecha).stream().map(mapper::prestamoToDto).toList();
    }


public List<PrestamoDTO> findByEstadoAndFechaVencimiento(String estado, LocalDate fechaVencimiento) {
    return prestamoRepository.findByEstadoIgnoreCaseAndFechaVencimiento(estado, fechaVencimiento)
            .stream()
            .map(mapper::prestamoToDto)
            .toList();
}

    @Transactional
    public int marcarVencidos(LocalDate hoy) {
        return prestamoRepository.marcarVencidos(hoy);
    }

    private UsuarioEntity referenciaUsuario(Long idUsuario) {
        if (idUsuario == null) {
            return null;
        }
        UsuarioEntity referencia = new UsuarioEntity();
        referencia.setId(idUsuario);
        return referencia;
    }

    private EjemplarEntity referenciaEjemplar(Long idEjemplar) {
        if (idEjemplar == null) {
            return null;
        }
        EjemplarEntity referencia = new EjemplarEntity();
        referencia.setId(idEjemplar);
        return referencia;
    }

    private SedeEntity referenciaSedeRetiro(Long idSedeRetiro) {
        if (idSedeRetiro == null) {
            return null;
        }
        SedeEntity referencia = new SedeEntity();
        referencia.setId(idSedeRetiro);
        return referencia;
    }
}
