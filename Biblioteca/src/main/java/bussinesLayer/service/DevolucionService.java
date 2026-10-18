package bussinesLayer.service;

import bussinesLayer.dto.DevolucionDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de devoluciones de prestamos.
 */
public interface DevolucionService {

    DevolucionDTO crearDevolucion(DevolucionDTO devolucionDTO);

    DevolucionDTO obtenerDevolucionPorId(Long id);

    List<DevolucionDTO> obtenerTodasLasDevoluciones();

    DevolucionDTO actualizarDevolucion(Long id, DevolucionDTO devolucionDTO);

    void eliminarDevolucion(Long id);
}
