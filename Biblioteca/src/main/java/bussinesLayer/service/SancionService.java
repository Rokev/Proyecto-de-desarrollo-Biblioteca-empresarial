package bussinesLayer.service;

import bussinesLayer.dto.SancionDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de sanciones a usuarios.
 */
public interface SancionService {

    SancionDTO crearSancion(SancionDTO sancionDTO);

    SancionDTO obtenerSancionPorId(Long id);

    List<SancionDTO> obtenerTodasLasSanciones();

    SancionDTO actualizarSancion(Long id, SancionDTO sancionDTO);

    void eliminarSancion(Long id);

    /**sanciones del usuario; estado null o TODAS devuelve todas. */
    List<SancionDTO> obtenerSancionesDeUsuario(Long usuarioId, String estado);
}
