package bussinesLayer.service;

import bussinesLayer.dto.NotificacionDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de notificaciones a usuarios.
 */
public interface NotificacionService {

    NotificacionDTO crearNotificacion(NotificacionDTO notificacionDTO);

    NotificacionDTO obtenerNotificacionPorId(Long id);

    List<NotificacionDTO> obtenerTodasLasNotificaciones();

    NotificacionDTO actualizarNotificacion(Long id, NotificacionDTO notificacionDTO);

    void eliminarNotificacion(Long id);

    /** Avisa a los bibliotecarios de una sede */
    void notificarSede(Long sedeId, String tipo, String titulo, String mensaje);
}
