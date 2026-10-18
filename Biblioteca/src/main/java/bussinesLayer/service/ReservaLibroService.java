package bussinesLayer.service;

import bussinesLayer.dto.ReservaLibroDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de reservas de libros sin stock.
 */
public interface ReservaLibroService {

    ReservaLibroDTO crearReservaLibro(ReservaLibroDTO reservaLibroDTO);

    ReservaLibroDTO obtenerReservaLibroPorId(Long id);

    List<ReservaLibroDTO> obtenerTodasLasReservasLibro();

    ReservaLibroDTO actualizarReservaLibro(Long id, ReservaLibroDTO reservaLibroDTO);

    void eliminarReservaLibro(Long id);
}
