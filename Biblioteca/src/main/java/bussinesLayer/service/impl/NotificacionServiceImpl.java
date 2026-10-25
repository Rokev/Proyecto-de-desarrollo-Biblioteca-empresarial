package bussinesLayer.service.impl;

import bussinesLayer.dto.NotificacionDTO;
import bussinesLayer.dto.UsuarioDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.NotificacionDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionDAO notificacionDAO;
    private final UsuarioDAO usuarioDAO;

    /** Debe coincidir con el nombre del rol del bibliotecario en la tabla roles. */
    private static final String ROL_BIBLIOTECARIO = "BIBLIOTECARIO";

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
    @Override
    public void notificarSede(Long sedeId, String tipo, String titulo, String mensaje) {
        List<UsuarioDTO> bibliotecarios = usuarioDAO.findBySedeIdAndRolNombreIgnoreCase(sedeId, ROL_BIBLIOTECARIO);
        if (bibliotecarios.isEmpty()) {
            log.warn("La sede {} no tiene bibliotecarios a quienes notificar: {}", sedeId, titulo);
            return;
        }
        LocalDateTime ahora = LocalDateTime.now();
        for (UsuarioDTO bibliotecario : bibliotecarios) {
            NotificacionDTO n = new NotificacionDTO();
            n.setTipo(tipo);
            n.setTitulo(titulo);
            n.setMensaje(mensaje);
            n.setCanal("APP");
            n.setFechaCreacion(ahora);
            n.setLeida(false);
            n.setUsuarioId(bibliotecario.getId());
            notificacionDAO.save(n);
        }
    }
}
