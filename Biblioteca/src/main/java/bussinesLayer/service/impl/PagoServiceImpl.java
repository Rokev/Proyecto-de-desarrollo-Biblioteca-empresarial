package bussinesLayer.service.impl;

import bussinesLayer.dto.*;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.PagoService;
import bussinesLayer.service.SuscripcionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    private static final String ESTADO_APROBADO = "APROBADO";
    private static final String ESTADO_RECHAZADO = "RECHAZADO";
    private static final String ESTADO_EXITOSO = "EXITOSO";

    private final PagoDAO pagoDAO;
    private final UsuarioDAO usuarioDAO;
    private final MetodoPagoDAO metodoPagoDAO;
    private final MultaDAO multaDAO;
    private final SuscripcionDAO suscripcionDAO;
    private final ReservaSalaDAO reservaSalaDAO;
    private final NotificacionDAO notificacionDAO;
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
        if (solicitud.getReservaSalaId() != null) pago.setReservaSalaId(solicitud.getReservaSalaId());

        PagoDTO pagoGuardado = pagoDAO.save(pago);

        // Si la tarjeta fue rechazada
        if (!esAprobado) {
            notificar(solicitud.getUsuarioId(), "PAGO_RECHAZADO", "Pago no procesado",
                    "El pago por $" + solicitud.getMonto() + " con concepto " + solicitud.getConcepto() + " fue rechazado por la pasarela.");
            return crearComprobante(pagoGuardado, ultimosCuatro, "La transacción fue rechazada por la entidad bancaria simulada.");
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
                if (solicitud.getPlanSuscripcionId() == null) {
                    throw new BusinessValidationException("Debe especificar el planSuscripcionId para comprar una suscripción");
                }
                suscripcionDAO.findByUsuarioIdAndEstado(solicitud.getUsuarioId(), "PENDIENTE_PAGO")
                        .or(() -> suscripcionDAO.findByUsuarioIdAndEstado(solicitud.getUsuarioId(), "ACTIVA"))
                        .ifPresent(suscripcion -> suscripcionService.activarSuscripcion(suscripcion.getId()));
                break;

            case CONCEPTO_ENVIO_INTERCIUDAD:
            case CONCEPTO_RESERVA_SALA:
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

    private void activarSuscripcionSiCorresponde(PagoDTO pago) {
        if (!CONCEPTO_SUSCRIPCION.equalsIgnoreCase(pago.getConcepto())
                || (!ESTADO_EXITOSO.equalsIgnoreCase(pago.getEstado()) && !ESTADO_APROBADO.equalsIgnoreCase(pago.getEstado()))
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
            case CONCEPTO_ENVIO_INTERCIUDAD -> {
                // Validación para envío interciudad
            }
            default -> throw new BusinessValidationException(
                    "Concepto inválido: " + dto.getConcepto() + ". Valores permitidos: MULTA, RECARGO, SUSCRIPCION, RESERVA_SALA, ENVIO_INTERCIUDAD");
        }
        dto.setConcepto(concepto);
    }

    private boolean esTarjetaAprobada(String numeroTarjeta) {
        if (numeroTarjeta == null || numeroTarjeta.endsWith("0000") || numeroTarjeta.endsWith("9999")) {
            return false;
        }
        return true;
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
}