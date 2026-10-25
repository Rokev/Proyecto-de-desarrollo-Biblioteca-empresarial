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

    /**  61 y  62: filtra por rol, sede, estado y suscripcion vigente; los filtros nulos se ignoran. */
    List<UsuarioDTO> filtrarUsuarios(String rol, Long sedeId, Boolean activo, Boolean conSuscripcion);

    /**  60: desactiva (sin borrar historial) la cuenta de un bibliotecario o repartidor. */
    UsuarioDTO desactivarPersonal(Long id);

    /**  64: suspende la cuenta de un usuario y le notifica. */
    UsuarioDTO suspenderUsuario(Long id);

    /**  65: reactiva una cuenta suspendida o desactivada, conservando su historial, y le notifica. */
    UsuarioDTO reactivarCuenta(Long id);
}
