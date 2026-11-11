package bussinesLayer.service;

import bussinesLayer.dto.ActualizarPerfilDTO;
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

    /** filtra por rol, sede, estado y suscripcion vigente; los filtros nulos se ignoran. */
    List<UsuarioDTO> filtrarUsuarios(String rol, Long sedeId, Boolean activo, Boolean conSuscripcion);

    /** desactiva (sin borrar historial) la cuenta de un bibliotecario o repartidor. */
    UsuarioDTO desactivarPersonal(Long id);

    /** suspende la cuenta de un usuario y le notifica. */
    UsuarioDTO suspenderUsuario(Long id);

    /** reactiva una cuenta suspendida o desactivada, conservando su historial, y le notifica. */
    UsuarioDTO reactivarCuenta(Long id);

    /** El propio usuario actualiza nombre, apellido, telefono y direccion. */
    UsuarioDTO actualizarPerfil(Long id, ActualizarPerfilDTO perfil);
}
