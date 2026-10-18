package persistanceLayer.dao;

import bussinesLayer.dto.NotificacionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.entity.NotificacionEntity;
import persistanceLayer.entity.UsuarioEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.NotificacionRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de Notificacion: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class NotificacionDAO {

    private final NotificacionRepository notificacionRepository;
    private final Mapper mapper;

    public NotificacionDTO save(NotificacionDTO dto) {
        NotificacionEntity entity = mapper.notificacionToEntity(dto);
        entity.setId(null);
        entity.setUsuario(referenciaUsuario(dto.getUsuarioId()));
        NotificacionEntity guardado = notificacionRepository.save(entity);
        return mapper.notificacionToDto(guardado);
    }

    public Optional<NotificacionDTO> findById(Long id) {
        return notificacionRepository.findById(id).map(mapper::notificacionToDto);
    }

    public List<NotificacionDTO> findAll() {
        return notificacionRepository.findAll().stream().map(mapper::notificacionToDto).toList();
    }

    public Optional<NotificacionDTO> update(Long id, NotificacionDTO dto) {
        return notificacionRepository.findById(id).map(existente -> {
            existente.setTipo(dto.getTipo());
            existente.setTitulo(dto.getTitulo());
            existente.setMensaje(dto.getMensaje());
            if (dto.getCanal() != null) {
                existente.setCanal(dto.getCanal());
            }
            existente.setFechaCreacion(dto.getFechaCreacion());
            if (dto.getLeida() != null) {
                existente.setLeida(dto.getLeida());
            }
            existente.setUsuario(referenciaUsuario(dto.getUsuarioId()));
            return mapper.notificacionToDto(notificacionRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return notificacionRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (notificacionRepository.existsById(id)) {
            notificacionRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<NotificacionDTO> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId) {
        return notificacionRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId).stream().map(mapper::notificacionToDto).toList();
    }

    public List<NotificacionDTO> findByUsuarioIdAndLeidaFalseOrderByFechaCreacionDesc(Long usuarioId) {
        return notificacionRepository.findByUsuarioIdAndLeidaFalseOrderByFechaCreacionDesc(usuarioId).stream().map(mapper::notificacionToDto).toList();
    }

    public long countByUsuarioIdAndLeidaFalse(Long usuarioId) {
        return notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    @Transactional
    public int marcarTodasLeidas(Long usuarioId) {
        return notificacionRepository.marcarTodasLeidas(usuarioId);
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
