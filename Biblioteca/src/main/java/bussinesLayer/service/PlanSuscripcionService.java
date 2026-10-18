package bussinesLayer.service;

import bussinesLayer.dto.PlanSuscripcionDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de planes de suscripcion.
 */
public interface PlanSuscripcionService {

    PlanSuscripcionDTO crearPlanSuscripcion(PlanSuscripcionDTO planSuscripcionDTO);

    PlanSuscripcionDTO obtenerPlanSuscripcionPorId(Long id);

    List<PlanSuscripcionDTO> obtenerTodosLosPlanesSuscripcion();

    PlanSuscripcionDTO actualizarPlanSuscripcion(Long id, PlanSuscripcionDTO planSuscripcionDTO);

    void eliminarPlanSuscripcion(Long id);
}
