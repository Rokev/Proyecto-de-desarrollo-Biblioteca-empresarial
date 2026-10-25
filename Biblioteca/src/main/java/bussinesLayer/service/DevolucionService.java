package bussinesLayer.service;

import bussinesLayer.dto.DevolucionDTO;
import bussinesLayer.dto.SedeDTO;
import bussinesLayer.dto.SolicitudDevolucionDTO;

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

    /** Sede en operacion mas cercana a las coordenadas del usuario (la sugiere el sistema). */
    SedeDTO sugerirSedeCercana(double latitud, double longitud);

    /** El usuario solicita devolver un prestamo en cualquier sede; genera el codigo QR. */
    DevolucionDTO solicitarDevolucion(SolicitudDevolucionDTO solicitud);

    /** El bibliotecario de la sede receptora confirma la devolucion presentando el codigo QR. */
    DevolucionDTO confirmarDevolucion(String codigoQr, Long bibliotecarioId);
}
