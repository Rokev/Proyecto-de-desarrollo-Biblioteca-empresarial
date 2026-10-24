package persistanceLayer.dao;

import bussinesLayer.dto.InscripcionEventoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.EventoEntity;
import persistanceLayer.entity.InscripcionEventoEntity;
import persistanceLayer.entity.UsuarioEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.InscripcionEventoRepository;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Acceso a datos de InscripcionEvento: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class InscripcionEventoDAO {

    private final InscripcionEventoRepository inscripcionEventoRepository;
    private final Mapper mapper;

    public InscripcionEventoDTO save(InscripcionEventoDTO dto) {
        InscripcionEventoEntity entity = mapper.inscripcionEventoToEntity(dto);
        entity.setId(null);
        entity.setEvento(referenciaEvento(dto.getEventoId()));
        entity.setUsuario(referenciaUsuario(dto.getUsuarioId()));
        InscripcionEventoEntity guardado = inscripcionEventoRepository.save(entity);
        return mapper.inscripcionEventoToDto(guardado);
    }

    public Optional<InscripcionEventoDTO> findById(Long id) {
        return inscripcionEventoRepository.findById(id).map(mapper::inscripcionEventoToDto);
    }

    public List<InscripcionEventoDTO> findAll() {
        return inscripcionEventoRepository.findAll().stream().map(mapper::inscripcionEventoToDto).toList();
    }

    public Optional<InscripcionEventoDTO> update(Long id, InscripcionEventoDTO dto) {
        return inscripcionEventoRepository.findById(id).map(existente -> {
            existente.setFechaInscripcion(dto.getFechaInscripcion());
            if (dto.getCantidadCupos() != null) {
                existente.setCantidadCupos(dto.getCantidadCupos());
            }
            existente.setFechaCancelacion(dto.getFechaCancelacion());
            if (dto.getEstado() != null) {
                existente.setEstado(dto.getEstado());
            }
            existente.setEvento(referenciaEvento(dto.getEventoId()));
            existente.setUsuario(referenciaUsuario(dto.getUsuarioId()));
            return mapper.inscripcionEventoToDto(inscripcionEventoRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return inscripcionEventoRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (inscripcionEventoRepository.existsById(id)) {
            inscripcionEventoRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<InscripcionEventoDTO> findByEventoId(Long eventoId) {
        return inscripcionEventoRepository.findByEventoId(eventoId).stream().map(mapper::inscripcionEventoToDto).toList();
    }

    public List<InscripcionEventoDTO> findByUsuarioId(Long usuarioId) {
        return inscripcionEventoRepository.findByUsuarioId(usuarioId).stream().map(mapper::inscripcionEventoToDto).toList();
    }

    public Optional<InscripcionEventoDTO> findByEventoIdAndUsuarioId(Long eventoId, Long usuarioId) {
        return inscripcionEventoRepository.findByEventoIdAndUsuarioId(eventoId, usuarioId).map(mapper::inscripcionEventoToDto);
    }

    public boolean existsByEventoIdAndUsuarioId(Long eventoId, Long usuarioId) {
        return inscripcionEventoRepository.existsByEventoIdAndUsuarioId(eventoId, usuarioId);
    }

    public List<InscripcionEventoDTO> findByEventoIdAndEstadoIgnoreCase(Long eventoId, String estado) {
        return inscripcionEventoRepository.findByEventoIdAndEstadoIgnoreCase(eventoId, estado).stream().map(mapper::inscripcionEventoToDto).toList();
    }

    public List<InscripcionEventoDTO> findByUsuarioIdAndEstadoIgnoreCase(Long usuarioId, String estado) {
        return inscripcionEventoRepository.findByUsuarioIdAndEstadoIgnoreCase(usuarioId, estado).stream().map(mapper::inscripcionEventoToDto).toList();
    }

    public List<InscripcionEventoDTO> findActivasEnEventosExclusivosFuturos(LocalDateTime ahora) {
        return inscripcionEventoRepository.findActivasEnEventosExclusivosFuturos(ahora).stream().map(mapper::inscripcionEventoToDto).toList();
    }    

    private EventoEntity referenciaEvento(Long idEvento) {
        if (idEvento == null) {
            return null;
        }
        EventoEntity referencia = new EventoEntity();
        referencia.setId(idEvento);
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
}
