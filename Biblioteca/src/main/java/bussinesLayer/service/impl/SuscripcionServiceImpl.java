package bussinesLayer.service.impl;

import bussinesLayer.dto.CompraSuscripcionDTO;
import bussinesLayer.dto.PlanSuscripcionDTO;
import bussinesLayer.dto.SuscripcionDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.SuscripcionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.MetodoPagoDAO;
import persistanceLayer.dao.PagoDAO;
import persistanceLayer.dao.PlanSuscripcionDAO;
import persistanceLayer.dao.SuscripcionDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SuscripcionServiceImpl implements SuscripcionService {

    private static final String ESTADO_PENDIENTE_PAGO = "PENDIENTE_PAGO";
    private static final String ESTADO_ACTIVA = "ACTIVA";
    private static final String ESTADO_CANCELADA = "CANCELADA";
    private static final String ESTADO_VENCIDA = "VENCIDA";

    private static final List<String> ESTADOS_VIGENTES = List.of(ESTADO_ACTIVA, ESTADO_CANCELADA);

    private final SuscripcionDAO suscripcionDAO;
    private final UsuarioDAO usuarioDAO;
    private final PlanSuscripcionDAO planSuscripcionDAO;

    // ------------------------------------------------------------------ CRUD

    /** Alta administrativa (sin pago). Para el flujo de usuario usar {@link #comprarSuscripcion}. */
    @Override
    public SuscripcionDTO crearSuscripcion(SuscripcionDTO dto) {
        log.info("Creando suscripcion del usuario {} al plan {}", dto.getUsuarioId(), dto.getPlanId());

        PlanSuscripcionDTO plan = validarReferencias(dto, true);

        if (suscripcionDAO.existsByUsuarioIdAndEstadoIn(
                dto.getUsuarioId(), List.of(ESTADO_ACTIVA, ESTADO_PENDIENTE_PAGO))) {
            throw new BusinessValidationException("El usuario ya tiene una suscripcion activa o pendiente de pago");
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
        // El descuento siempre se copia del plan; el cliente no lo decide.
        dto.setDescuentoSalas(plan.getDescuentoSalas());

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
        boolean cambioDePlan = !actual.getPlanId().equals(dto.getPlanId());
        PlanSuscripcionDTO plan = validarReferencias(dto, cambioDePlan);

        // El descuento no lo edita el cliente: se conserva el de la compra,
        // salvo que se cambie de plan, caso en el que se toma el del plan nuevo.
        dto.setDescuentoSalas(cambioDePlan ? plan.getDescuentoSalas() : actual.getDescuentoSalas());

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

    // ------------------------------------------------------------ compra

    @Override
    public SuscripcionDTO comprarSuscripcion(CompraSuscripcionDTO compra) {
        log.info("Compra de suscripcion: usuario {}, plan {}", compra.getUsuarioId(), compra.getPlanId());

        if (!usuarioDAO.existsById(compra.getUsuarioId())) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + compra.getUsuarioId());
        }
        PlanSuscripcionDTO plan = planSuscripcionDAO.findById(compra.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Plan de suscripcion no encontrado con ID: " + compra.getPlanId()));
        if (!Boolean.TRUE.equals(plan.getActivo())) {
            throw new BusinessValidationException("El plan de suscripcion seleccionado no esta activo");
        }
        buscarVigente(compra.getUsuarioId()).ifPresent(vigente -> {
            throw new BusinessValidationException(
                    "El usuario ya tiene una suscripcion vigente hasta " + vigente.getFechaFin());
        });

        // Si ya hay una compra pendiente de pago se reutiliza (permite cambiar de plan o reintentar el pago).
        Optional<SuscripcionDTO> pendiente =
                suscripcionDAO.findByUsuarioIdAndEstado(compra.getUsuarioId(), ESTADO_PENDIENTE_PAGO);
        SuscripcionDTO suscripcion = pendiente.orElseGet(SuscripcionDTO::new);

        LocalDate hoy = LocalDate.now();
        suscripcion.setUsuarioId(compra.getUsuarioId());
        suscripcion.setPlanId(plan.getId());
        suscripcion.setEstado(ESTADO_PENDIENTE_PAGO);
        suscripcion.setFechaInicio(hoy);
        suscripcion.setFechaFin(hoy.plusMonths(plan.getDuracionMeses()));
        suscripcion.setValorMensual(plan.getPrecio()
                .divide(BigDecimal.valueOf(plan.getDuracionMeses()), 2, RoundingMode.HALF_UP));
        suscripcion.setDescuentoSalas(plan.getDescuentoSalas());
        suscripcion.setRenovacionAutomatica(Boolean.TRUE.equals(compra.getRenovacionAutomatica()));
        suscripcion.setMesesConsecutivos(0);

        SuscripcionDTO guardada = pendiente.isPresent()
                ? suscripcionDAO.update(suscripcion.getId(), suscripcion).orElseThrow()
                : suscripcionDAO.save(suscripcion);
        log.info("Suscripcion {} pendiente de pago para el usuario {}", guardada.getId(), guardada.getUsuarioId());
        return guardada;
    }

    @Override
    public SuscripcionDTO activarSuscripcion(Long suscripcionId) {
        SuscripcionDTO actual = suscripcionDAO.findById(suscripcionId)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripcion no encontrada con ID: " + suscripcionId));
        if (!ESTADO_PENDIENTE_PAGO.equalsIgnoreCase(actual.getEstado())) {
            throw new BusinessValidationException("La suscripcion no esta pendiente de pago");
        }
        PlanSuscripcionDTO plan = planSuscripcionDAO.findById(actual.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Plan de suscripcion no encontrado con ID: " + actual.getPlanId()));

        // El periodo vigente empieza cuando se confirma el pago.
        LocalDate hoy = LocalDate.now();
        actual.setEstado(ESTADO_ACTIVA);
        actual.setFechaInicio(hoy);
        actual.setFechaFin(hoy.plusMonths(plan.getDuracionMeses()));

        SuscripcionDTO activada = suscripcionDAO.update(suscripcionId, actual)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripcion no encontrada con ID: " + suscripcionId));
        log.info("Suscripcion activada ID: {}", suscripcionId);
        return activada;
    }

    // ---------------------------------------------------------- cancelar

    @Override
    public SuscripcionDTO cancelarSuscripcion(Long suscripcionId, Long usuarioId) {
        SuscripcionDTO actual = suscripcionDAO.findById(suscripcionId)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripcion no encontrada con ID: " + suscripcionId));

        if (!actual.getUsuarioId().equals(usuarioId)) {
            throw new BusinessValidationException("La suscripcion no pertenece al usuario " + usuarioId);
        }
        if (ESTADO_CANCELADA.equalsIgnoreCase(actual.getEstado())) {
            throw new BusinessValidationException("La suscripcion ya fue cancelada");
        }
        if (!ESTADO_ACTIVA.equalsIgnoreCase(actual.getEstado())) {
            throw new BusinessValidationException("Solo se puede cancelar una suscripcion activa");
        }
        if (actual.getFechaFin().isBefore(LocalDate.now())) {
            throw new BusinessValidationException("La suscripcion ya finalizo");
        }

        actual.setEstado(ESTADO_CANCELADA);
        actual.setFechaCancelacion(LocalDateTime.now());
        actual.setRenovacionAutomatica(false);

        SuscripcionDTO cancelada = suscripcionDAO.update(suscripcionId, actual)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripcion no encontrada con ID: " + suscripcionId));
        log.info("Suscripcion cancelada ID: {} (beneficios hasta {})", suscripcionId, cancelada.getFechaFin());
        return cancelada;
    }

    // ------------------------------------------------------- vigencia

    @Override
    @Transactional(readOnly = true)
    public SuscripcionDTO obtenerSuscripcionVigente(Long usuarioId) {
        verificarUsuario(usuarioId);
        return buscarVigente(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario " + usuarioId + " no tiene una suscripcion vigente"));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tieneSuscripcionVigente(Long usuarioId) {
        return buscarVigente(usuarioId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SuscripcionDTO> buscarSuscripcionVigente(Long usuarioId) {
        return buscarVigente(usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public void exigirSuscripcionVigente(Long usuarioId) {
        verificarUsuario(usuarioId);
        if (buscarVigente(usuarioId).isEmpty()) {
            throw new BusinessValidationException("El usuario " + usuarioId + " no tiene una suscripcion vigente");
        }
    }

    @Override
    public int marcarSuscripcionesVencidas() {
        List<SuscripcionDTO> vencidas =
                suscripcionDAO.findByEstadoIgnoreCaseAndFechaFinBefore(ESTADO_ACTIVA, LocalDate.now());
        for (SuscripcionDTO s : vencidas) {
            s.setEstado(ESTADO_VENCIDA);
            suscripcionDAO.update(s.getId(), s);
        }
        log.info("Suscripciones marcadas como vencidas: {}", vencidas.size());
        return vencidas.size();
    }

    // -------------------------------------------------------------- privados

    /** Vigente = ACTIVA o CANCELADA con fechaFin de hoy en adelante. */
    private Optional<SuscripcionDTO> buscarVigente(Long usuarioId) {
        return suscripcionDAO.findFirstByUsuarioIdAndEstadoInAndFechaFinGreaterThanEqualOrderByFechaFinDesc(
                usuarioId, ESTADOS_VIGENTES, LocalDate.now());
    }

    private void verificarUsuario(Long usuarioId) {
        if (!usuarioDAO.existsById(usuarioId)) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId);
        }
    }


    private PlanSuscripcionDTO validarReferencias(SuscripcionDTO dto, boolean exigirPlanActivo) {
        if (!usuarioDAO.existsById(dto.getUsuarioId())) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + dto.getUsuarioId());
        }
        PlanSuscripcionDTO plan = planSuscripcionDAO.findById(dto.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Plan de suscripcion no encontrado con ID: " + dto.getPlanId()));
        if (exigirPlanActivo && !Boolean.TRUE.equals(plan.getActivo())) {
            throw new BusinessValidationException("El plan de suscripcion seleccionado no esta activo");
        }
        if (!dto.getFechaFin().isAfter(dto.getFechaInicio())) {
            throw new BusinessValidationException("La fechaFin debe ser posterior a la fechaInicio");
        }
        return plan;
    }
}