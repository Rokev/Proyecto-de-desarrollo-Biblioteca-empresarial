package persistanceLayer.dao;

import bussinesLayer.dto.DevolucionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.DevolucionEntity;
import persistanceLayer.entity.PrestamoEntity;
import persistanceLayer.entity.SedeEntity;
import persistanceLayer.entity.UsuarioEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.DevolucionesRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de Devolucion: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class DevolucionDAO {

    private final DevolucionesRepository devolucionesRepository;
    private final Mapper mapper;

    public DevolucionDTO save(DevolucionDTO dto) {
        DevolucionEntity entity = mapper.devolucionToEntity(dto);
        entity.setId(null);
        entity.setPrestamo(referenciaPrestamo(dto.getPrestamoId()));
        entity.setSedeDevolucion(referenciaSedeDevolucion(dto.getSedeDevolucionId()));
        entity.setBibliotecario(referenciaBibliotecario(dto.getBibliotecarioId()));
        DevolucionEntity guardado = devolucionesRepository.save(entity);
        return mapper.devolucionToDto(guardado);
    }

    public Optional<DevolucionDTO> findById(Long id) {
        return devolucionesRepository.findById(id).map(mapper::devolucionToDto);
    }

    public List<DevolucionDTO> findAll() {
        return devolucionesRepository.findAll().stream().map(mapper::devolucionToDto).toList();
    }

    public Optional<DevolucionDTO> update(Long id, DevolucionDTO dto) {
        return devolucionesRepository.findById(id).map(existente -> {
            existente.setCodigoQr(dto.getCodigoQr());
            existente.setFechaSolicitud(dto.getFechaSolicitud());
            existente.setFechaConfirmacion(dto.getFechaConfirmacion());
            if (dto.getEstado() != null) {
                existente.setEstado(dto.getEstado());
            }
            existente.setObservaciones(dto.getObservaciones());
            existente.setPrestamo(referenciaPrestamo(dto.getPrestamoId()));
            existente.setSedeDevolucion(referenciaSedeDevolucion(dto.getSedeDevolucionId()));
            existente.setBibliotecario(referenciaBibliotecario(dto.getBibliotecarioId()));
            return mapper.devolucionToDto(devolucionesRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return devolucionesRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (devolucionesRepository.existsById(id)) {
            devolucionesRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<DevolucionDTO> findByCodigoQr(String codigoQr) {
        return devolucionesRepository.findByCodigoQr(codigoQr).map(mapper::devolucionToDto);
    }

    public Optional<DevolucionDTO> findByPrestamoId(Long prestamoId) {
        return devolucionesRepository.findByPrestamoId(prestamoId).map(mapper::devolucionToDto);
    }

    public boolean existsByPrestamoId(Long prestamoId) {
        return devolucionesRepository.existsByPrestamoId(prestamoId);
    }

    public List<DevolucionDTO> findBySedeDevolucionId(Long sedeDevolucionId) {
        return devolucionesRepository.findBySedeDevolucionId(sedeDevolucionId).stream().map(mapper::devolucionToDto).toList();
    }

    public List<DevolucionDTO> findByEstadoIgnoreCase(String estado) {
        return devolucionesRepository.findByEstadoIgnoreCase(estado).stream().map(mapper::devolucionToDto).toList();
    }

    public List<DevolucionDTO> findBySedeDevolucionIdAndEstadoIgnoreCase(Long sedeDevolucionId, String estado) {
        return devolucionesRepository.findBySedeDevolucionIdAndEstadoIgnoreCase(sedeDevolucionId, estado).stream().map(mapper::devolucionToDto).toList();
    }

    private PrestamoEntity referenciaPrestamo(Long idPrestamo) {
        if (idPrestamo == null) {
            return null;
        }
        PrestamoEntity referencia = new PrestamoEntity();
        referencia.setId(idPrestamo);
        return referencia;
    }

    private SedeEntity referenciaSedeDevolucion(Long idSedeDevolucion) {
        if (idSedeDevolucion == null) {
            return null;
        }
        SedeEntity referencia = new SedeEntity();
        referencia.setId(idSedeDevolucion);
        return referencia;
    }

    private UsuarioEntity referenciaBibliotecario(Long idBibliotecario) {
        if (idBibliotecario == null) {
            return null;
        }
        UsuarioEntity referencia = new UsuarioEntity();
        referencia.setId(idBibliotecario);
        return referencia;
    }
}
