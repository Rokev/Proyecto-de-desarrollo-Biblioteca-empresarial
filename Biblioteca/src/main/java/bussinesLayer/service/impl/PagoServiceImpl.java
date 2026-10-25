package bussinesLayer.service.impl;
import java.time.LocalDate;

import bussinesLayer.dto.*;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.PagoService;
import bussinesLayer.util.EstadoPago;
import bussinesLayer.service.SuscripcionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.math.BigDecimal;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PagoServiceImpl implements PagoService {

    private static final String CONCEPTO_MULTA = "MULTA";
    private static final String CONCEPTO_RECARGO = "RECARGO";
    private static final String CONCEPTO_SUSCRIPCION = "SUSCRIPCION";
    private static final String CONCEPTO_RESERVA_SALA = "RESERVA_SALA";
    private static final String CONCEPTO_ENVIO_INTERCIUDAD = "ENVIO_INTERCIUDAD";

    /** Tarjetas de prueba predefinidas. Para agregar o cambiar tarjetas, editar solo estos conjuntos. */
    private static final Set<String> TARJETAS_APROBADAS = Set.of("4111111111111111", "5555555555554444");
    private static final Set<String> TARJETAS_RECHAZADAS = Set.of("4000000000000002", "4000000000009995");

    private static final String ESTADO_APROBADO = EstadoPago.APROBADO;
    private static final String ESTADO_RECHAZADO = EstadoPago.RECHAZADO;

    private final PagoDAO pagoDAO;
    private final UsuarioDAO usuarioDAO;
    private final MetodoPagoDAO metodoPagoDAO;
    private final MultaDAO multaDAO;
    private final SuscripcionDAO suscripcionDAO;
    private final ReservaSalaDAO reservaSalaDAO;
    private final NotificacionDAO notificacionDAO;
    private final PedidoInterciudadDAO pedidoInterciudadDAO;
    private final PlanSuscripcionDAO planSuscripcionDAO;

    private final SuscripcionService suscripcionService;

    // =========================================================================
    // MÉTODOS CRUD TRADICIONALES
    // =========================================================================

    @Override
    public PagoDTO crearPago(PagoDTO pagoDTO) {
        log.info("Creando pago con referencia: {}", pagoDTO.getReferencia());

        if (pagoDAO.existsByReferencia(pagoDTO.getReferencia())) {
            throw new BusinessValidationException(
                    "Ya existe un pago registrado con la referencia: " + pagoDTO.getReferencia());
        }
        validarPago(pagoDTO);

        PagoDTO guardado = pagoDAO.save(pagoDTO);
        activarSuscripcionSiCorresponde(guardado);
        log.info("Pago creado con ID: {}", guardado.getId());
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public PagoDTO obtenerPagoPorId(Long id) {
        return pagoDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoDTO> obtenerTodosLosPagos() {
        return pagoDAO.findAll();
    }

    @Override
    public PagoDTO actualizarPago(Long id, PagoDTO pagoDTO) {
        if (!pagoDAO.existsById(id)) {
            throw new ResourceNotFoundException("Pago no encontrado con ID: " + id);
        }

        pagoDAO.findByReferencia(pagoDTO.getReferencia())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new BusinessValidationException(
                            "Ya existe otro pago registrado con la referencia: " + pagoDTO.getReferencia());
                });
        validarPago(pagoDTO);

        PagoDTO actualizado = pagoDAO.update(id, pagoDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado con ID: " + id));
        activarSuscripcionSiCorresponde(actualizado);
        log.info("Pago actualizado con ID: {}", id);
        return actualizado;
    }

    @Override
    public void eliminarPago(Long id) {
        if (!pagoDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Pago no encontrado con ID: " + id);
        }
        log.info("Pago eliminado con ID: {}", id);
    }

    // =========================================================================
    // PASARELA DE PAGO SIMULADO
    // =========================================================================

    @Override
    public ComprobantePagoDTO procesarPagoSimulado(SolicitudPagoDTO solicitud) {
        log.info("Procesando pago simulado para usuario ID: {}, concepto: {}, monto: {}",
                solicitud.getUsuarioId(), solicitud.getConcepto(), solicitud.getMonto());

        if (!usuarioDAO.existsById(solicitud.getUsuarioId())) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + solicitud.getUsuarioId());
        }
        if (CONCEPTO_ENVIO_INTERCIUDAD.equalsIgnoreCase(solicitud.getConcepto())) {
            PedidoInterciudadDTO pedido = validarPagoEnvio(
                    solicitud.getUsuarioId(), solicitud.getPedidoInterciudadId(), solicitud.getMonto());
            exigirEnvioPendienteDePago(pedido);
        }
        if (CONCEPTO_RESERVA_SALA.equalsIgnoreCase(solicitud.getConcepto())) {
            validarPagoReservaSala(solicitud.getUsuarioId(), solicitud.getReservaSalaId(), solicitud.getMonto());
        }
        SuscripcionDTO suscripcionPorPagar = null;
        if (CONCEPTO_SUSCRIPCION.equalsIgnoreCase(solicitud.getConcepto())) {
            suscripcionPorPagar = validarPagoSuscripcion(
                    solicitud.getUsuarioId(), solicitud.getPlanSuscripcionId(), solicitud.getMonto());
        }
        if (CONCEPTO_MULTA.equalsIgnoreCase(solicitud.getConcepto())
                || CONCEPTO_RECARGO.equalsIgnoreCase(solicitud.getConcepto())) {
            validarPagoMulta(solicitud.getUsuarioId(), solicitud.getMultaId(), solicitud.getMonto());
        }

        boolean esAprobado = esTarjetaAprobada(solicitud.getNumeroTarjeta());
        String estadoPago = esAprobado ? ESTADO_APROBADO : ESTADO_RECHAZADO;
        String ultimosCuatro = solicitud.getNumeroTarjeta().substring(solicitud.getNumeroTarjeta().length() - 4);
        String numReferencia = "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime ahora = LocalDateTime.now();

        // Registrar la transacción de pago
        PagoDTO pago = new PagoDTO();
        pago.setMonto(solicitud.getMonto());
        pago.setConcepto(solicitud.getConcepto().toUpperCase());
        pago.setEstado(estadoPago);
        pago.setFechaPago(ahora);
        pago.setReferencia(numReferencia);
        pago.setUsuarioId(solicitud.getUsuarioId());
        pago.setMetodoPagoId(1L); // Método de pago por defecto para la pasarela simulada

        if (solicitud.getMultaId() != null) pago.setMultaId(solicitud.getMultaId());
        if (suscripcionPorPagar != null) pago.setSuscripcionId(suscripcionPorPagar.getId());
        if (solicitud.getReservaSalaId() != null) pago.setReservaSalaId(solicitud.getReservaSalaId());
        if (CONCEPTO_ENVIO_INTERCIUDAD.equalsIgnoreCase(solicitud.getConcepto())) {
            pago.setPedidoInterciudadId(solicitud.getPedidoInterciudadId());
        }

        PagoDTO pagoGuardado = pagoDAO.save(pago);

        // Si la tarjeta fue rechazada
        if (!esAprobado) {
            notificar(solicitud.getUsuarioId(), "PAGO_RECHAZADO", "Pago no procesado",
                    "El pago por $" + solicitud.getMonto() + " con concepto " + solicitud.getConcepto() + " fue rechazado por la pasarela.");
            return crearComprobante(pagoGuardado, ultimosCuatro, mensajeRechazo(solicitud.getNumeroTarjeta()));
        }

        // Ejecutar lógica de negocio si el pago fue APROBADO
        String conceptoUpper = solicitud.getConcepto().toUpperCase();
        switch (conceptoUpper) {
            case CONCEPTO_MULTA:
            case CONCEPTO_RECARGO:
                if (solicitud.getMultaId() == null) {
                    throw new BusinessValidationException("Debe especificar la multaId para procesar un pago de multa o recargo");
                }
                MultaDTO multa = multaDAO.findById(solicitud.getMultaId())
                        .orElseThrow(() -> new ResourceNotFoundException("Multa no encontrada con ID: " + solicitud.getMultaId()));
                multa.setEstado("PAGADA");
                multaDAO.update(multa.getId(), multa);
                break;

            case CONCEPTO_SUSCRIPCION:
                suscripcionService.activarSuscripcion(suscripcionPorPagar.getId());
                break;

            case CONCEPTO_ENVIO_INTERCIUDAD:
                PedidoInterciudadDTO pedidoPagado = pedidoInterciudadDAO.findById(solicitud.getPedidoInterciudadId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Pedido interciudad no encontrado con ID: " + solicitud.getPedidoInterciudadId()));
                        
                pedidoPagado.setEstado("SOLICITADO"); // pago aprobado: el pedido queda confirmado
                pedidoInterciudadDAO.update(pedidoPagado.getId(), pedidoPagado);
                        
                notificar(solicitud.getUsuarioId(), "PEDIDO_CONFIRMADO", "Pedido interciudad confirmado",
                        "Tu pedido #" + pedidoPagado.getId() + " fue confirmado tras el pago del envío ($"
                                + solicitud.getMonto() + "). Lo recibirás en " + pedidoPagado.getCiudadEntrega()
                                + " el " + pedidoPagado.getFechaRecepcion() + " (" + pedidoPagado.getHoraRecepcion()
                                + ") y deberás devolverlo en la sede " + pedidoPagado.getSedeDevolucionNombre() + ".");
                log.info("Pedido interciudad {} confirmado por pago aprobado {}", pedidoPagado.getId(), numReferencia);
                break;
            case CONCEPTO_RESERVA_SALA:
                ReservaSalaDTO reservaSala = reservaSalaDAO.findById(solicitud.getReservaSalaId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Reserva de sala no encontrada con ID: " + solicitud.getReservaSalaId()));
                reservaSala.setEstado("CONFIRMADA");
                reservaSalaDAO.update(reservaSala.getId(), reservaSala);
                break;

            default:
                throw new BusinessValidationException("Concepto de pago no soportado: " + solicitud.getConcepto());
        }

        notificar(solicitud.getUsuarioId(), "PAGO_APROBADO", "Pago exitoso",
                "Se procesó correctamente tu pago por $" + solicitud.getMonto() + " (Ref: " + numReferencia + ").");

        return crearComprobante(pagoGuardado, ultimosCuatro, "Pago procesado y aprobado exitosamente.");
    }

    @Override
    @Transactional(readOnly = true)
    public ComprobantePagoDTO obtenerComprobantePorId(Long id) {
        PagoDTO pago = pagoDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de pago no encontrado con ID: " + id));
        return crearComprobante(pago, "****", "Consulta de comprobante");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComprobantePagoDTO> obtenerPagosPorUsuario(Long usuarioId) {
        if (!usuarioDAO.existsById(usuarioId)) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId);
        }
        return pagoDAO.findByUsuarioIdOrderByFechaPagoDesc(usuarioId).stream()
                .map(pago -> crearComprobante(pago, "****", "Historial de transacciones de usuario"))
                .toList();
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================

    private ReservaSalaDTO validarPagoReservaSala(Long usuarioId, Long reservaId, BigDecimal monto) {
        if (reservaId == null) {
            throw new BusinessValidationException("El campo reservaSalaId es obligatorio para pagos de RESERVA_SALA");
        }
        ReservaSalaDTO reserva = reservaSalaDAO.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva de sala no encontrada con ID: " + reservaId));
        if (!reserva.getUsuarioId().equals(usuarioId)) {
            throw new BusinessValidationException("La reserva " + reservaId + " no pertenece al usuario " + usuarioId);
        }
        if (!"PENDIENTE_PAGO".equalsIgnoreCase(reserva.getEstado())) {
            throw new BusinessValidationException("Solo se puede pagar una reserva en estado PENDIENTE_PAGO (estado actual: "
                    + reserva.getEstado() + ")");
        }
        if (monto.compareTo(reserva.getValorTotal()) != 0) {
            throw new BusinessValidationException("El monto del pago (" + monto
                    + ") no coincide con el valorTotal de la reserva (" + reserva.getValorTotal() + ")");
        }
        return reserva;
    }

    private SuscripcionDTO validarPagoSuscripcion(Long usuarioId, Long planId, BigDecimal monto) {
    if (planId == null) {
        throw new BusinessValidationException("Debe especificar el planSuscripcionId para pagar una suscripción");
    }
    SuscripcionDTO pendiente = suscripcionDAO.findByUsuarioIdAndEstado(usuarioId, "PENDIENTE_PAGO")
            .orElseThrow(() -> new BusinessValidationException(
                    "El usuario no tiene una compra de suscripción pendiente de pago. "
                            + "Inicie la compra en POST /api/v1/suscripciones/comprar"));
    if (!pendiente.getPlanId().equals(planId)) {
        throw new BusinessValidationException("El plan a pagar (" + planId
                + ") no coincide con el de la compra pendiente (" + pendiente.getPlanId() + ")");
    }
    PlanSuscripcionDTO plan = planSuscripcionDAO.findById(planId)
            .orElseThrow(() -> new ResourceNotFoundException("Plan de suscripción no encontrado con ID: " + planId));
    if (!Boolean.TRUE.equals(plan.getActivo())) {
        throw new BusinessValidationException("El plan de suscripción seleccionado ya no está activo");
    }
    if (monto.compareTo(plan.getPrecio()) != 0) {
        throw new BusinessValidationException("El monto del pago (" + monto
                + ") no coincide con el precio del plan (" + plan.getPrecio() + ")");
    }
    return pendiente;
}

    /**
     * Valida el pago de una multa o recargo antes de cobrar: la multa debe existir, ser del usuario que paga,
     * estar pendiente y el monto debe ser exactamente el de la multa.
     */
    private MultaDTO validarPagoMulta(Long usuarioId, Long multaId, BigDecimal monto) {
        if (multaId == null) {
            throw new BusinessValidationException("Debe especificar la multaId para pagar una multa o recargo");
        }
        MultaDTO multa = multaDAO.findById(multaId)
                .orElseThrow(() -> new ResourceNotFoundException("Multa no encontrada con ID: " + multaId));
        if (!multa.getUsuarioId().equals(usuarioId)) {
            throw new BusinessValidationException("La multa " + multaId + " no pertenece al usuario " + usuarioId);
        }
        if (!"PENDIENTE".equalsIgnoreCase(multa.getEstado())) {
            throw new BusinessValidationException("Solo se puede pagar una multa PENDIENTE (estado actual: "
                    + multa.getEstado() + ")");
        }
        if (monto.compareTo(multa.getMonto()) != 0) {
            throw new BusinessValidationException("El monto del pago (" + monto
                    + ") no coincide con el monto de la multa (" + multa.getMonto() + ")");
        }
        return multa;
    }

    private void activarSuscripcionSiCorresponde(PagoDTO pago) {
        if (!CONCEPTO_SUSCRIPCION.equalsIgnoreCase(pago.getConcepto())
                || !ESTADO_APROBADO.equalsIgnoreCase(pago.getEstado())
                || pago.getSuscripcionId() == null) {
            return;
        }
        suscripcionDAO.findById(pago.getSuscripcionId())
                .filter(s -> "PENDIENTE_PAGO".equalsIgnoreCase(s.getEstado()))
                .ifPresent(s -> suscripcionService.activarSuscripcion(s.getId()));
    }

    private void validarPago(PagoDTO dto) {
        if (!usuarioDAO.existsById(dto.getUsuarioId())) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + dto.getUsuarioId());
        }
        if (metodoPagoDAO.findByIdAndUsuarioId(dto.getMetodoPagoId(), dto.getUsuarioId()).isEmpty()) {
            throw new BusinessValidationException(
                    "El método de pago " + dto.getMetodoPagoId() + " no existe o no pertenece al usuario " + dto.getUsuarioId());
        }

        String concepto = dto.getConcepto().toUpperCase();
        switch (concepto) {
            case CONCEPTO_MULTA, CONCEPTO_RECARGO -> {
                if (dto.getMultaId() == null) {
                    throw new BusinessValidationException("El campo multaId es obligatorio para pagos de MULTA o RECARGO");
                }
                if (!multaDAO.existsById(dto.getMultaId())) {
                    throw new ResourceNotFoundException("Multa no encontrada con ID: " + dto.getMultaId());
                }
            }
            case CONCEPTO_SUSCRIPCION -> {
                if (dto.getSuscripcionId() == null) {
                    throw new BusinessValidationException("El campo suscripcionId es obligatorio para pagos de SUSCRIPCION");
                }
                if (!suscripcionDAO.existsById(dto.getSuscripcionId())) {
                    throw new ResourceNotFoundException("Suscripción no encontrada con ID: " + dto.getSuscripcionId());
                }
            }
            case CONCEPTO_RESERVA_SALA -> {
                if (dto.getReservaSalaId() == null) {
                    throw new BusinessValidationException("El campo reservaSalaId es obligatorio para pagos de RESERVA_SALA");
                }
                var reserva = reservaSalaDAO.findById(dto.getReservaSalaId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Reserva de sala no encontrada con ID: " + dto.getReservaSalaId()));
                if (dto.getMonto().compareTo(reserva.getValorTotal()) != 0) {
                    throw new BusinessValidationException("El monto del pago (" + dto.getMonto()
                            + ") no coincide con el valorTotal de la reserva (" + reserva.getValorTotal() + ")");
                }
            }
            case CONCEPTO_ENVIO_INTERCIUDAD ->
                    validarPagoEnvio(dto.getUsuarioId(), dto.getPedidoInterciudadId(), dto.getMonto());
            default -> throw new BusinessValidationException(
                    "Concepto inválido: " + dto.getConcepto() + ". Valores permitidos: MULTA, RECARGO, SUSCRIPCION, RESERVA_SALA, ENVIO_INTERCIUDAD");
        }
        String estado = dto.getEstado() == null ? "" : dto.getEstado().trim().toUpperCase();
        if (!EstadoPago.TODOS.contains(estado)) {
            throw new BusinessValidationException("Estado de pago inválido: " + dto.getEstado()
                    + ". Valores permitidos: PENDIENTE, APROBADO, RECHAZADO, REEMBOLSADO");
        }
        dto.setEstado(estado);
        dto.setConcepto(concepto);
    }

    private PedidoInterciudadDTO validarPagoEnvio(Long usuarioId, Long pedidoId, BigDecimal monto) {
        if (pedidoId == null) {
            throw new BusinessValidationException("El campo pedidoInterciudadId es obligatorio para pagos de ENVIO_INTERCIUDAD");
        }
        PedidoInterciudadDTO pedido = pedidoInterciudadDAO.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido interciudad no encontrado con ID: " + pedidoId));
        if (!pedido.getUsuarioId().equals(usuarioId)) {
            throw new BusinessValidationException("El pedido " + pedidoId + " no pertenece al usuario " + usuarioId);
        }
        BigDecimal costoEnvio = BigDecimal.valueOf(pedido.getCostoEnvio());
        if (monto.compareTo(costoEnvio) != 0) {
            throw new BusinessValidationException("El monto del pago (" + monto
                    + ") no coincide con el costo de envío del pedido (" + costoEnvio + ")");
        }
        return pedido;
    }

    private void exigirEnvioPendienteDePago(PedidoInterciudadDTO pedido) {
        if (!"PENDIENTE_PAGO".equalsIgnoreCase(pedido.getEstado())) {
            throw new BusinessValidationException("Solo se puede pagar el envío de un pedido en estado PENDIENTE_PAGO (estado actual: "
                    + pedido.getEstado() + ")");
        }
        if (pagoDAO.findEnvioPagadoPorPedido(pedido.getId()).isPresent()) {
            throw new BusinessValidationException("El envío del pedido " + pedido.getId() + " ya fue pagado");
        }
    }

    /** Solo estas tarjetas de prueba aprueban el pago (RF-14). Cualquier otra se rechaza. */
    private boolean esTarjetaAprobada(String numeroTarjeta) {
        return TARJETAS_APROBADAS.contains(numeroTarjeta);
    }

    /** Mensaje del comprobante cuando el pago es rechazado: distingue la tarjeta de rechazo de una desconocida. */
    private String mensajeRechazo(String numeroTarjeta) {
        if (TARJETAS_RECHAZADAS.contains(numeroTarjeta)) {
            return "La transacción fue rechazada por la entidad bancaria simulada.";
        }
        return "Tarjeta no reconocida: solo se aceptan las tarjetas de prueba predefinidas.";
    }

    private ComprobantePagoDTO crearComprobante(PagoDTO pago, String ultimosCuatro, String mensaje) {
        ComprobantePagoDTO comprobante = new ComprobantePagoDTO();
        comprobante.setId(pago.getId());
        comprobante.setNumeroReferencia(pago.getReferencia());
        comprobante.setEstado(pago.getEstado());
        comprobante.setConcepto(pago.getConcepto());
        comprobante.setMonto(pago.getMonto());
        comprobante.setUltimosCuatroDigitos(ultimosCuatro);
        comprobante.setFechaPago(pago.getFechaPago());
        comprobante.setMensaje(mensaje);
        comprobante.setUsuarioId(pago.getUsuarioId());
        return comprobante;
    }

    private void notificar(Long usuarioId, String tipo, String titulo, String mensaje) {
        NotificacionDTO notificacion = new NotificacionDTO();
        notificacion.setTipo(tipo);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacion.setCanal("APP");
        notificacion.setFechaCreacion(LocalDateTime.now());
        notificacion.setLeida(false);
        notificacion.setUsuarioId(usuarioId);
        notificacionDAO.save(notificacion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoDTO> filtrarPagos(String concepto, String estado, LocalDate desde, LocalDate hasta, Long usuarioId) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new BusinessValidationException("La fecha final no puede ser anterior a la inicial");
        }
        String conceptoNormalizado = "ENVIO".equalsIgnoreCase(concepto) ? CONCEPTO_ENVIO_INTERCIUDAD : concepto;
        return pagoDAO.findAll().stream()
                .filter(p -> conceptoNormalizado == null || conceptoNormalizado.isBlank()
                        || conceptoNormalizado.equalsIgnoreCase(p.getConcepto()))
                .filter(p -> estado == null || estado.isBlank() || estado.equalsIgnoreCase(p.getEstado()))
                .filter(p -> usuarioId == null || usuarioId.equals(p.getUsuarioId()))
                .filter(p -> desde == null || (p.getFechaPago() != null && !p.getFechaPago().toLocalDate().isBefore(desde)))
                .filter(p -> hasta == null || (p.getFechaPago() != null && !p.getFechaPago().toLocalDate().isAfter(hasta)))
                .sorted(java.util.Comparator.comparing(PagoDTO::getFechaPago,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .toList();
    }
}