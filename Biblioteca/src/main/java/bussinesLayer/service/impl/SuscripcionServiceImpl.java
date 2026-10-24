package bussinesLayer.service.impl;

import bussinesLayer.dto.CompraSuscripcionDTO;
import bussinesLayer.dto.PagoDTO;
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
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SuscripcionServiceImpl implements SuscripcionService {

    private static final String ESTADO_PENDIENTE_PAGO = "PENDIENTE_PAGO";
    private static final String ESTADO_ACTIVA = "ACTIVA";
    private static final String ESTADO_CANCELADA = "CANCELADA";
    private static final String ESTADO_VENCIDA = "VENCIDA";

    private static final String CONCEPTO_SUSCRIPCION = "SUSCRIPCION";
    private static final String PAGO_EXITOSO = "EXITOSO";

    /** RF-35: estados que dan beneficios mientras fechaFin no haya pasado.
     *  CANCELADA conserva los beneficios hasta fechaFin (RF-25). */
    private static final List<String> ESTADOS_VIGENTES = List.of(ESTADO_ACTIVA, ESTADO_CANCELADA);

    private final SuscripcionDAO suscripcionDAO;
    private final UsuarioDAO usuarioDAO;
    private final PlanSuscripcionDAO planSuscripcionDAO;
    private final MetodoPagoDAO metodoPagoDAO;
    private final PagoDAO pagoDAO;

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

    // ------------------------------------------------------------ RF-24 compra

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
        if (metodoPagoDAO.findByIdAndUsuarioId(compra.getMetodoPagoId(), compra.getUsuarioId()).isEmpty()) {
            throw new BusinessValidationException("El metodo de pago " + compra.getMetodoPagoId()
                    + " no existe o no pertenece al usuario " + compra.getUsuarioId());
        }
        if (suscripcionDAO.existsByUsuarioIdAndEstadoIn(compra.getUsuarioId(), List.of(ESTADO_PENDIENTE_PAGO))) {
            throw new BusinessValidationException("El usuario tiene una compra de suscripcion pendiente de pago");
        }
        buscarVigente(compra.getUsuarioId()).ifPresent(vigente -> {
            throw new BusinessValidationException(
                    "El usuario ya tiene una suscripcion vigente hasta " + vigente.getFechaFin());
        });

        // 1) Suscripcion en PENDIENTE_PAGO; fechas y valores salen del plan, no del cliente.
        LocalDate hoy = LocalDate.now();
        SuscripcionDTO nueva = new SuscripcionDTO();
        nueva.setUsuarioId(compra.getUsuarioId());
        nueva.setPlanId(plan.getId());
        nueva.setEstado(ESTADO_PENDIENTE_PAGO);
        nueva.setFechaInicio(hoy);
        nueva.setFechaFin(hoy.plusMonths(plan.getDuracionMeses()));
        nueva.setValorMensual(plan.getPrecio()
                .divide(BigDecimal.valueOf(plan.getDuracionMeses()), 2, RoundingMode.HALF_UP));
        nueva.setDescuentoSalas(plan.getDescuentoSalas());
        nueva.setRenovacionAutomatica(Boolean.TRUE.equals(compra.getRenovacionAutomatica()));
        nueva.setMesesConsecutivos(0);
        SuscripcionDTO guardada = suscripcionDAO.save(nueva);

        // 2) Pago de la suscripcion.
        PagoDTO pago = new PagoDTO();
        pago.setMonto(plan.getPrecio());
        pago.setFechaPago(LocalDateTime.now());
        pago.setConcepto(CONCEPTO_SUSCRIPCION);
        pago.setEstado(procesarPago(plan));
        pago.setReferencia("SUS-" + UUID.randomUUID());
        pago.setUsuarioId(compra.getUsuarioId());
        pago.setMetodoPagoId(compra.getMetodoPagoId());
        pago.setSuscripcionId(guardada.getId());
        pagoDAO.save(pago);

        // 3) Si el pago fue exitoso se activan los beneficios; si no, se revierte todo.
        if (!PAGO_EXITOSO.equals(pago.getEstado())) {
            throw new BusinessValidationException("No se pudo procesar el pago de la suscripcion");
        }
        SuscripcionDTO activa = activarSuscripcion(guardada.getId());
        log.info("Suscripcion {} comprada y activada para el usuario {}", activa.getId(), activa.getUsuarioId());
        return activa;
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

    // ---------------------------------------------------------- RF-25 cancelar

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

        // Los beneficios se mantienen hasta fechaFin: la vigencia (RF-35) cuenta las CANCELADAS no vencidas.
        actual.setEstado(ESTADO_CANCELADA);
        actual.setFechaCancelacion(LocalDateTime.now());
        actual.setRenovacionAutomatica(false);

        SuscripcionDTO cancelada = suscripcionDAO.update(suscripcionId, actual)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripcion no encontrada con ID: " + suscripcionId));
        log.info("Suscripcion cancelada ID: {} (beneficios hasta {})", suscripcionId, cancelada.getFechaFin());
        return cancelada;
    }

    // ------------------------------------------------------- RF-35 vigencia

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

    /** Punto de integracion con la pasarela de pagos. Por ahora se simula un pago exitoso. */
    private String procesarPago(PlanSuscripcionDTO plan) {
        return PAGO_EXITOSO;
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