package bussinesLayer.service;

import bussinesLayer.dto.LibroDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de libros del catalogo.
 */
public interface LibroService {

    LibroDTO crearLibro(LibroDTO libroDTO);

    LibroDTO obtenerLibroPorId(Long id);

    List<LibroDTO> obtenerTodosLosLibros();

    LibroDTO actualizarLibro(Long id, LibroDTO libroDTO);

    void eliminarLibro(Long id);
}
