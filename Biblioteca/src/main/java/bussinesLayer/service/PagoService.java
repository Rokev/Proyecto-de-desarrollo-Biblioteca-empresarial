package bussinesLayer.service;

import bussinesLayer.dto.PagoDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de pagos de multas, suscripciones y reservas de sala.
 */
public interface PagoService {

    PagoDTO crearPago(PagoDTO pagoDTO);

    PagoDTO obtenerPagoPorId(Long id);

    List<PagoDTO> obtenerTodosLosPagos();

    PagoDTO actualizarPago(Long id, PagoDTO pagoDTO);

    void eliminarPago(Long id);
}
