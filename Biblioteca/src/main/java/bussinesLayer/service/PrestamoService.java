package bussinesLayer.service;

import bussinesLayer.dto.PrestamoDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de prestamos de ejemplares.
 */
public interface PrestamoService {

    PrestamoDTO crearPrestamo(PrestamoDTO prestamoDTO);

    PrestamoDTO obtenerPrestamoPorId(Long id);

    List<PrestamoDTO> obtenerTodosLosPrestamos();

    PrestamoDTO actualizarPrestamo(Long id, PrestamoDTO prestamoDTO);

    void eliminarPrestamo(Long id);
}
