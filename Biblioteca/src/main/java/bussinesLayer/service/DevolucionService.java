package bussinesLayer.service;

import bussinesLayer.dto.DevolucionDTO;
import bussinesLayer.dto.ResultadoDanioPerdidaDTO;
import bussinesLayer.dto.SedeDTO;
import bussinesLayer.dto.SolicitudDevolucionDTO;

import java.math.BigDecimal;
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

    /**
     * RF-41: el bibliotecario registra que el libro de un prestamo ACTIVO o VENCIDO se devolvio danado o se perdio.
     * El ejemplar pasa a DANADO o PERDIDO (sale del stock disponible), el prestamo se cierra, se aplica la multa
     * configurada (RF-75) y la sancion si su politica esta activa (RF-74), y se notifica al usuario.
     *
     * @param tipo  DANADO o PERDIDO
     * @param monto opcional; si es nulo se cobra el monto configurado
     */
    ResultadoDanioPerdidaDTO registrarDanioOPerdida(Long prestamoId, String tipo, Long bibliotecarioId,
                                                    BigDecimal monto, String observaciones);
}
