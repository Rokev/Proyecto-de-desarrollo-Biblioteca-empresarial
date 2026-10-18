package bussinesLayer.service;

import bussinesLayer.dto.GeneroDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de generos literarios.
 */
public interface GeneroService {

    GeneroDTO crearGenero(GeneroDTO generoDTO);

    GeneroDTO obtenerGeneroPorId(Long id);

    List<GeneroDTO> obtenerTodosLosGeneros();

    GeneroDTO actualizarGenero(Long id, GeneroDTO generoDTO);

    void eliminarGenero(Long id);
}
