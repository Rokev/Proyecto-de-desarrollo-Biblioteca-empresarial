package bussinesLayer.service.impl;

import bussinesLayer.dto.PlanSuscripcionDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.PlanSuscripcionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.PlanSuscripcionDAO;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PlanSuscripcionServiceImpl implements PlanSuscripcionService {

    private final PlanSuscripcionDAO planSuscripcionDAO;

    @Override
    public PlanSuscripcionDTO crearPlanSuscripcion(PlanSuscripcionDTO dto) {
        log.info("Creando plan de suscripcion: {}", dto.getNombre());

        if (planSuscripcionDAO.existsByNombreIgnoreCase(dto.getNombre())) {
            throw new BusinessValidationException(
                    "Ya existe un plan de suscripcion con el nombre: " + dto.getNombre());
        }
        if (dto.getActivo() == null) {
            dto.setActivo(true);
        }

        PlanSuscripcionDTO guardado = planSuscripcionDAO.save(dto);
        log.info("Plan de suscripcion creado con ID: {}", guardado.getId());
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public PlanSuscripcionDTO obtenerPlanSuscripcionPorId(Long id) {
        return planSuscripcionDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan de suscripcion no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanSuscripcionDTO> obtenerTodosLosPlanesSuscripcion() {
        return planSuscripcionDAO.findAll();
    }

    @Override
    public PlanSuscripcionDTO actualizarPlanSuscripcion(Long id, PlanSuscripcionDTO dto) {
        if (!planSuscripcionDAO.existsById(id)) {
            throw new ResourceNotFoundException("Plan de suscripcion no encontrado con ID: " + id);
        }

        planSuscripcionDAO.findByNombreIgnoreCase(dto.getNombre())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new BusinessValidationException(
                            "Ya existe otro plan de suscripcion con el nombre: " + dto.getNombre());
                });

        PlanSuscripcionDTO actualizado = planSuscripcionDAO.update(id, dto)
                .orElseThrow(() -> new ResourceNotFoundException("Plan de suscripcion no encontrado con ID: " + id));
        log.info("Plan de suscripcion actualizado ID: {}", id);
        return actualizado;
    }

    @Override
    public void eliminarPlanSuscripcion(Long id) {
        if (!planSuscripcionDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Plan de suscripcion no encontrado con ID: " + id);
        }
        log.info("Plan de suscripcion eliminado ID: {}", id);
    }
}
