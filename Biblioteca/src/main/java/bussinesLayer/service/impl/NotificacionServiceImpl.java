package bussinesLayer.service.impl;

import bussinesLayer.dto.NotificacionDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.NotificacionDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionDAO notificacionDAO;
    private final UsuarioDAO usuarioDAO;

    @Override
    public NotificacionDTO crearNotificacion(NotificacionDTO notificacionDTO) {
        log.info("Creando notificacion para el usuario: {}", notificacionDTO.getUsuarioId());

        validarUsuario(notificacionDTO);
        if (notificacionDTO.getLeida() == null) {
            notificacionDTO.setLeida(false);
        }

        NotificacionDTO guardada = notificacionDAO.save(notificacionDTO);
        log.info("Notificacion creada con ID: {}", guardada.getId());
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificacionDTO obtenerNotificacionPorId(Long id) {
        return notificacionDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificacion no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificacionDTO> obtenerTodasLasNotificaciones() {
        return notificacionDAO.findAll();
    }

    @Override
    public NotificacionDTO actualizarNotificacion(Long id, NotificacionDTO notificacionDTO) {
        if (!notificacionDAO.existsById(id)) {
            throw new ResourceNotFoundException("Notificacion no encontrada con ID: " + id);
        }

        validarUsuario(notificacionDTO);

        NotificacionDTO actualizada = notificacionDAO.update(id, notificacionDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Notificacion no encontrada con ID: " + id));
        log.info("Notificacion actualizada ID: {}", id);
        return actualizada;
    }

    @Override
    public void eliminarNotificacion(Long id) {
        if (!notificacionDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Notificacion no encontrada con ID: " + id);
        }
        log.info("Notificacion eliminada ID: {}", id);
    }

    private void validarUsuario(NotificacionDTO dto) {
        if (!usuarioDAO.existsById(dto.getUsuarioId())) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + dto.getUsuarioId());
        }
    }
}
