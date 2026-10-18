package bussinesLayer.service;

import bussinesLayer.dto.SesionDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de sesiones de usuario.
 */
public interface SesionService {

    SesionDTO crearSesion(SesionDTO sesionDTO);

    SesionDTO obtenerSesionPorId(Long id);

    List<SesionDTO> obtenerTodasLasSesiones();

    SesionDTO actualizarSesion(Long id, SesionDTO sesionDTO);

    void eliminarSesion(Long id);
}
