package bussinesLayer.service.impl;

import bussinesLayer.dto.SuscripcionDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.SuscripcionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.PlanSuscripcionDAO;
import persistanceLayer.dao.SuscripcionDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SuscripcionServiceImpl implements SuscripcionService {

    private static final String ESTADO_ACTIVA = "ACTIVA";
    private static final String ESTADO_CANCELADA = "CANCELADA";

    private final SuscripcionDAO suscripcionDAO;
    private final UsuarioDAO usuarioDAO;
    private final PlanSuscripcionDAO planSuscripcionDAO;

    @Override
    public SuscripcionDTO crearSuscripcion(SuscripcionDTO dto) {
        log.info("Creando suscripcion del usuario {} al plan {}", dto.getUsuarioId(), dto.getPlanId());

        validarReferencias(dto, true);

        if (suscripcionDAO.existsByUsuarioIdAndEstadoIgnoreCase(dto.getUsuarioId(), ESTADO_ACTIVA)) {
            throw new BusinessValidationException("El usuario ya tiene una suscripcion activa");
        }

        if (dto.getEstado() == null || dto.getEstado().isBlank()) {
            dto.setEstado(ESTADO_ACTIVA);
        }
        if (dto.getRenovacionAutomatica() == null) {
            dto.setRenovacionAutomatica(false);
        }
        if (dto.getMesesConsecutivos() == null) {
            dto.setMesesConsecutivos(0);
        }

        SuscripcionDTO guardada = suscripcionDAO.save(dto);
        log.info("Suscripcion creada con ID: {}", guardada.getId());
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public SuscripcionDTO obtenerSuscripcionPorId(Long id) {
        return suscripcionDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripcion no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SuscripcionDTO> obtenerTodasLasSuscripciones() {
        return suscripcionDAO.findAll();
    }

    @Override
    public SuscripcionDTO actualizarSuscripcion(Long id, SuscripcionDTO dto) {
        SuscripcionDTO actual = suscripcionDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripcion no encontrada con ID: " + id));

        if (!actual.getUsuarioId().equals(dto.getUsuarioId())) {
            throw new BusinessValidationException("No se puede cambiar el usuario de una suscripcion");
        }
        validarReferencias(dto, !actual.getPlanId().equals(dto.getPlanId()));

        if (dto.getEstado() != null && ESTADO_CANCELADA.equalsIgnoreCase(dto.getEstado())
                && !ESTADO_CANCELADA.equalsIgnoreCase(actual.getEstado())
                && dto.getFechaCancelacion() == null) {
            dto.setFechaCancelacion(LocalDateTime.now());
        }

        SuscripcionDTO actualizada = suscripcionDAO.update(id, dto)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripcion no encontrada con ID: " + id));
        log.info("Suscripcion actualizada ID: {}", id);
        return actualizada;
    }

    @Override
    public void eliminarSuscripcion(Long id) {
        if (!suscripcionDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Suscripcion no encontrada con ID: " + id);
        }
        log.info("Suscripcion eliminada ID: {}", id);
    }

    private void validarReferencias(SuscripcionDTO dto, boolean exigirPlanActivo) {
        if (!usuarioDAO.existsById(dto.getUsuarioId())) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + dto.getUsuarioId());
        }
        var plan = planSuscripcionDAO.findById(dto.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Plan de suscripcion no encontrado con ID: " + dto.getPlanId()));
        if (exigirPlanActivo && !Boolean.TRUE.equals(plan.getActivo())) {
            throw new BusinessValidationException("El plan de suscripcion seleccionado no esta activo");
        }
        if (!dto.getFechaFin().isAfter(dto.getFechaInicio())) {
            throw new BusinessValidationException("La fechaFin debe ser posterior a la fechaInicio");
        }
    }
}
