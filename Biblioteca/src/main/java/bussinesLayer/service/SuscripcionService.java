package bussinesLayer.service;

import bussinesLayer.dto.SuscripcionDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de suscripciones de usuarios.
 */
public interface SuscripcionService {

    SuscripcionDTO crearSuscripcion(SuscripcionDTO suscripcionDTO);

    SuscripcionDTO obtenerSuscripcionPorId(Long id);

    List<SuscripcionDTO> obtenerTodasLasSuscripciones();

    SuscripcionDTO actualizarSuscripcion(Long id, SuscripcionDTO suscripcionDTO);

    void eliminarSuscripcion(Long id);
}
