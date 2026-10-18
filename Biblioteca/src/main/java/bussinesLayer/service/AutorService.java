package bussinesLayer.service;

import bussinesLayer.dto.AutorDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de autores del catalogo.
 */
public interface AutorService {

    AutorDTO crearAutor(AutorDTO autorDTO);

    AutorDTO obtenerAutorPorId(Long id);

    List<AutorDTO> obtenerTodosLosAutores();

    AutorDTO actualizarAutor(Long id, AutorDTO autorDTO);

    void eliminarAutor(Long id);
}
