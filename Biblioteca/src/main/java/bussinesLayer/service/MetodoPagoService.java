package bussinesLayer.service;

import bussinesLayer.dto.MetodoPagoDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de metodos de pago de los usuarios.
 */
public interface MetodoPagoService {

    MetodoPagoDTO crearMetodoPago(MetodoPagoDTO metodoPagoDTO);

    MetodoPagoDTO obtenerMetodoPagoPorId(Long id);

    List<MetodoPagoDTO> obtenerTodosLosMetodosPago();

    MetodoPagoDTO actualizarMetodoPago(Long id, MetodoPagoDTO metodoPagoDTO);

    void eliminarMetodoPago(Long id);
}
