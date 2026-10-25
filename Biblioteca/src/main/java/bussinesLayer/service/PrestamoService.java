package bussinesLayer.service;
import bussinesLayer.dto.PrestamoDetalleDTO;

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

    /** prestamos del usuario (libro, sede, estado y fecha limite). */
    List<PrestamoDetalleDTO> obtenerPrestamosDeUsuario(Long usuarioId, String estado);

    /** prestamos de la sede, filtrables por estado, usuario y libro. */
    List<PrestamoDetalleDTO> obtenerPrestamosDeSede(Long sedeId, String estado, Long usuarioId, Long libroId);
}
