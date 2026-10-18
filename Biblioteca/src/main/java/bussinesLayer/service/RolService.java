package bussinesLayer.service;

import bussinesLayer.dto.RolDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de roles de usuario.
 */
public interface RolService {

    RolDTO crearRol(RolDTO rolDTO);

    RolDTO obtenerRolPorId(Long id);

    List<RolDTO> obtenerTodosLosRoles();

    RolDTO actualizarRol(Long id, RolDTO rolDTO);

    void eliminarRol(Long id);
}
