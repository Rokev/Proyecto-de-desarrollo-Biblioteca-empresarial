package bussinesLayer.service;

import bussinesLayer.dto.UsuarioDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de usuarios del sistema.
 */
public interface UsuarioService {

    UsuarioDTO crearUsuario(UsuarioDTO usuarioDTO);

    UsuarioDTO obtenerUsuarioPorId(Long id);

    List<UsuarioDTO> obtenerTodosLosUsuarios();

    UsuarioDTO actualizarUsuario(Long id, UsuarioDTO usuarioDTO);

    void eliminarUsuario(Long id);

    /**filtra por rol, sede, estado y suscripcion vigente; los filtros nulos se ignoran. */
    List<UsuarioDTO> filtrarUsuarios(String rol, Long sedeId, Boolean activo, Boolean conSuscripcion);
}
