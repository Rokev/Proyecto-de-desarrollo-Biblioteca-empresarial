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

    /**  72: impide nuevas compras del plan sin afectar las suscripciones vigentes. */
    PlanSuscripcionDTO desactivarPlan(Long id);

    /**  73: vuelve a permitir nuevas compras del plan. */
    PlanSuscripcionDTO reactivarPlan(Long id);
}
