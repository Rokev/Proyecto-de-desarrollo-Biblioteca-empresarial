package persistanceLayer.dao;

import bussinesLayer.dto.JustificacionRetrasoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.JustificacionRetrasoEntity;
import persistanceLayer.entity.PrestamoEntity;
import persistanceLayer.entity.UsuarioEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.JustificacionRetrasoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de JustificacionRetraso: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class JustificacionRetrasoDAO {

    private final JustificacionRetrasoRepository justificacionRetrasoRepository;
    private final Mapper mapper;

    public JustificacionRetrasoDTO save(JustificacionRetrasoDTO dto) {
        JustificacionRetrasoEntity entity = mapper.justificacionRetrasoToEntity(dto);
        entity.setId(null);
        entity.setPrestamo(referenciaPrestamo(dto.getPrestamoId()));
        entity.setUsuario(referenciaUsuario(dto.getUsuarioId()));
        entity.setBibliotecario(referenciaBibliotecario(dto.getBibliotecarioId()));
        JustificacionRetrasoEntity guardado = justificacionRetrasoRepository.save(entity);
        return mapper.justificacionRetrasoToDto(guardado);
    }

    public Optional<JustificacionRetrasoDTO> findById(Long id) {
        return justificacionRetrasoRepository.findById(id).map(mapper::justificacionRetrasoToDto);
    }

    public List<JustificacionRetrasoDTO> findAll() {
        return justificacionRetrasoRepository.findAll().stream().map(mapper::justificacionRetrasoToDto).toList();
    }

    public Optional<JustificacionRetrasoDTO> update(Long id, JustificacionRetrasoDTO dto) {
        return justificacionRetrasoRepository.findById(id).map(existente -> {
            existente.setTexto(dto.getTexto());
            existente.setFechaEnvio(dto.getFechaEnvio());
            if (dto.getEstado() != null) {
                existente.setEstado(dto.getEstado());
            }
            existente.setRespuesta(dto.getRespuesta());
            existente.setFechaRespuesta(dto.getFechaRespuesta());
            existente.setPrestamo(referenciaPrestamo(dto.getPrestamoId()));
            existente.setUsuario(referenciaUsuario(dto.getUsuarioId()));
            existente.setBibliotecario(referenciaBibliotecario(dto.getBibliotecarioId()));
            return mapper.justificacionRetrasoToDto(justificacionRetrasoRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return justificacionRetrasoRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (justificacionRetrasoRepository.existsById(id)) {
            justificacionRetrasoRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<JustificacionRetrasoDTO> findByPrestamoId(Long prestamoId) {
        return justificacionRetrasoRepository.findByPrestamoId(prestamoId).stream().map(mapper::justificacionRetrasoToDto).toList();
    }

    public List<JustificacionRetrasoDTO> findByUsuarioId(Long usuarioId) {
        return justificacionRetrasoRepository.findByUsuarioId(usuarioId).stream().map(mapper::justificacionRetrasoToDto).toList();
    }

    public List<JustificacionRetrasoDTO> findByBibliotecarioId(Long bibliotecarioId) {
        return justificacionRetrasoRepository.findByBibliotecarioId(bibliotecarioId).stream().map(mapper::justificacionRetrasoToDto).toList();
    }

    public List<JustificacionRetrasoDTO> findByEstadoIgnoreCase(String estado) {
        return justificacionRetrasoRepository.findByEstadoIgnoreCase(estado).stream().map(mapper::justificacionRetrasoToDto).toList();
    }

    public List<JustificacionRetrasoDTO> findByBibliotecarioIdAndEstadoIgnoreCase(Long bibliotecarioId, String estado) {
        return justificacionRetrasoRepository.findByBibliotecarioIdAndEstadoIgnoreCase(bibliotecarioId, estado).stream().map(mapper::justificacionRetrasoToDto).toList();
    }

    private PrestamoEntity referenciaPrestamo(Long idPrestamo) {
        if (idPrestamo == null) {
            return null;
        }
        PrestamoEntity referencia = new PrestamoEntity();
        referencia.setId(idPrestamo);
        return referencia;
    }

    private UsuarioEntity referenciaUsuario(Long idUsuario) {
        if (idUsuario == null) {
            return null;
        }
        UsuarioEntity referencia = new UsuarioEntity();
        referencia.setId(idUsuario);
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
