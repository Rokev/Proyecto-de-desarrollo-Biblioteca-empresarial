package bussinesLayer.service.impl;

import bussinesLayer.dto.*;
import bussinesLayer.service.AutomatizacionService;
import bussinesLayer.service.ReservaLibroService;
import bussinesLayer.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import persistanceLayer.dao.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutomatizacionServiceImpl implements AutomatizacionService {

    private static final String PRESTAMO_SOLICITADO = "SOLICITADO";
    private static final String PRESTAMO_ACTIVO = "ACTIVO";
    private static final String PRESTAMO_VENCIDO = "VENCIDO";
    private static final String PRESTAMO_DEVUELTO = "DEVUELTO";
    private static final String PRESTAMO_CANCELADO = "CANCELADO";
    private static final String MODALIDAD_SEDE = "SEDE";

    private static final String RESERVA_NOTIFICADA = "NOTIFICADA";
    private static final String RESERVA_EXPIRADA = "EXPIRADA";

    private static final String EJEMPLAR_DISPONIBLE = "DISPONIBLE";
    private static final String EJEMPLAR_PRESTADO = "PRESTADO";
    private static final String EJEMPLAR_RESERVADO = "RESERVADO";

    private static final String MULTA_PENDIENTE = "PENDIENTE";
    private static final String MULTA_PAGADA = "PAGADA";
    private static final String MULTA_ANULADA = "ANULADA";

    private static final String SANCION_BLOQUEO = "BLOQUEO_PRESTAMOS";
    private static final String SANCION_ACTIVA = "ACTIVA";
    private static final String SANCION_FINALIZADA = "FINALIZADA";

    private static final String SALA_PENDIENTE_PAGO = "PENDIENTE_PAGO";
    private static final String SALA_CONFIRMADA = "CONFIRMADA";
    private static final String SALA_CANCELADA = "CANCELADA";
    private static final String SALA_FINALIZADA = "FINALIZADA";

    /** Prefijos de motivo: permiten reconocer la multa y la sancion por mora de un prestamo. */
    private static final String MOTIVO_MORA = "Mora en la devolucion";
    private static final String MOTIVO_SANCION_MORA = "Retraso en la devolucion";
    private static final String MOTIVO_NO_RECOGIDO = "Pedido interciudad no recogido a tiempo";
    private static final String MOTIVO_SANCION_NO_RECOGIDO = "No recogio el pedido interciudad a tiempo";

    private static final BigDecimal MULTA_POR_DIA_MORA = new BigDecimal("2000.00");
    private static final BigDecimal MULTA_NO_RECOGIDO = new BigDecimal("20000.00");
    private static final int DIAS_SANCION_NO_RECOGIDO = 7;
    private static final int MINUTOS_PAGO_RESERVA_SALA = 30;
    private static final int HORAS_RETIRO_SOLICITUD = 48;

    private static final String PEDIDO_PENDIENTE_PAGO = "PENDIENTE_PAGO";
    private static final String PEDIDO_CANCELADO = "CANCELADO";
    private static final String PEDIDO_EN_ESPERA_DE_RECOGIDA = "EN_ESPERA_DE_RECOGIDA";
    private static final String PEDIDO_DEVUELTO_PENDIENTE_TRASLADO = "DEVUELTO_PENDIENTE_TRASLADO";


    private final PrestamoDAO prestamoDAO;
    private final ReservaLibroDAO reservaLibroDAO;
    private final ReservaSalaDAO reservaSalaDAO;
    private final SalaEstudioDAO salaEstudioDAO;
    private final EventoDAO eventoDAO;
    private final InscripcionEventoDAO inscripcionEventoDAO;
    private final SancionDAO sancionDAO;
    private final EjemplarDAO ejemplarDAO;
    private final LibroDAO libroDAO;
    private final MultaDAO multaDAO;
    private final NotificacionDAO notificacionDAO;
    private final PedidoInterciudadDAO pedidoInterciudadDAO;
    private final ReservaLibroService reservaLibroService;
    private final NotificacionService notificacionService;
    private final PlatformTransactionManager transactionManager;

    // ------------------------------------------------ Seccion 1: prestamos vencidos

    @Override
    public int procesarPrestamosVencidos() {
        List<PrestamoDTO> candidatos = prestamoDAO
                .findByEstadoIgnoreCaseAndFechaVencimientoBefore(PRESTAMO_ACTIVO, LocalDate.now());
        int procesados = 0;
        for (PrestamoDTO candidato : candidatos) {
            if (enTransaccion(candidato.getId(), "marcar prestamo vencido",
                    () -> marcarVencidoYMultar(candidato.getId()))) {
                procesados++;
            }
        }
        log.info("Prestamos marcados como vencidos: {} de {} candidatos", procesados, candidatos.size());
        return procesados;
    }

    @Override
    public long evaluarRetrasoAlDevolver(Long prestamoId) {
        PrestamoDTO prestamo = prestamoDAO.findById(prestamoId).orElse(null);
        if (prestamo == null || prestamo.getFechaVencimiento() == null
                || !(PRESTAMO_ACTIVO.equalsIgnoreCase(prestamo.getEstado())
                        || PRESTAMO_VENCIDO.equalsIgnoreCase(prestamo.getEstado()))
                || !prestamo.getFechaVencimiento().isBefore(LocalDate.now())) {
            return 0; // a tiempo, o el préstamo no está en un estado evaluable
        }
        if (PRESTAMO_ACTIVO.equalsIgnoreCase(prestamo.getEstado())) {
            marcarVencidoYMultar(prestamo.getId());   // la tarea diaria aún no lo había detectado
        } else {
            sincronizarMultaPorMora(prestamo);        // ya estaba vencido: deja la multa con los días finales
        }
        return ChronoUnit.DAYS.between(prestamo.getFechaVencimiento(), LocalDate.now());
    }

    private boolean marcarVencidoYMultar(Long prestamoId) {
        PrestamoDTO prestamo = prestamoDAO.findById(prestamoId).orElse(null);
        if (prestamo == null || !PRESTAMO_ACTIVO.equalsIgnoreCase(prestamo.getEstado())) {
            return false;
        }

        prestamo.setEstado(PRESTAMO_VENCIDO);
        prestamoDAO.update(prestamo.getId(), prestamo);

        sincronizarMultaPorMora(prestamo);

        boolean yaTieneSancion = sancionDAO.findByPrestamoId(prestamo.getId()).stream()
                .anyMatch(x -> SANCION_BLOQUEO.equalsIgnoreCase(x.getTipo()));
        if (!yaTieneSancion) {
            SancionDTO sancion = new SancionDTO();
            sancion.setTipo(SANCION_BLOQUEO);
            sancion.setMotivo(MOTIVO_SANCION_MORA + " (prestamo #" + prestamo.getId() + ")");
            sancion.setFechaAplicacion(LocalDateTime.now());
            sancion.setFechaFin(null);
            sancion.setEstado(SANCION_ACTIVA);
            sancion.setUsuarioId(prestamo.getUsuarioId());
            sancion.setPrestamoId(prestamo.getId());
            sancionDAO.save(sancion);
        }

        notificar(prestamo.getUsuarioId(), "PRESTAMO_VENCIDO", "Tu prestamo esta vencido",
                "El prestamo de \"" + tituloDeEjemplar(prestamo.getEjemplarId()) + "\" vencio el "
                        + prestamo.getFechaVencimiento() + ". Se genero una multa de $" + MULTA_POR_DIA_MORA
                        + " por cada dia de retraso y no podras pedir mas libros hasta devolverlo y pagar la multa.");
        log.info("Prestamo {} marcado como VENCIDO (sancion creada: {})", prestamo.getId(), !yaTieneSancion);
        return true;
    }

    // -------------------------------- Seccion 2a: solicitudes de prestamo no retiradas

    @Override
    public int cancelarSolicitudesNoRetiradas() {
        LocalDateTime limite = LocalDateTime.now().minusHours(HORAS_RETIRO_SOLICITUD);
        List<PrestamoDTO> candidatos = prestamoDAO.findByEstadoIgnoreCase(PRESTAMO_SOLICITADO).stream()
                .filter(p -> MODALIDAD_SEDE.equalsIgnoreCase(p.getModalidad()))
                .filter(p -> p.getFechaSolicitud() != null && p.getFechaSolicitud().isBefore(limite))
                .toList();
        int procesados = 0;
        for (PrestamoDTO candidato : candidatos) {
            if (enTransaccion(candidato.getId(), "cancelar solicitud no retirada",
                    () -> cancelarSolicitud(candidato.getId(), limite))) {
                procesados++;
            }
        }
        log.info("Solicitudes de prestamo canceladas por no retiro: {}", procesados);
        return procesados;
    }

    private boolean cancelarSolicitud(Long prestamoId, LocalDateTime limite) {
        PrestamoDTO prestamo = prestamoDAO.findById(prestamoId).orElse(null);
        if (prestamo == null || !PRESTAMO_SOLICITADO.equalsIgnoreCase(prestamo.getEstado())
                || !MODALIDAD_SEDE.equalsIgnoreCase(prestamo.getModalidad())
                || prestamo.getFechaSolicitud() == null || !prestamo.getFechaSolicitud().isBefore(limite)) {
            return false;
        }

        prestamo.setEstado(PRESTAMO_CANCELADO);
        prestamoDAO.update(prestamo.getId(), prestamo);

        liberarEjemplarYReasignar(prestamo.getEjemplarId(), EJEMPLAR_PRESTADO);

        notificar(prestamo.getUsuarioId(), "SOLICITUD_CANCELADA", "Solicitud de prestamo cancelada",
                "Tu solicitud del libro \"" + tituloDeEjemplar(prestamo.getEjemplarId()) + "\" fue cancelada porque no lo "
                        + "retiraste dentro de las " + HORAS_RETIRO_SOLICITUD + " horas establecidas.");
        log.info("Solicitud de prestamo {} cancelada por no retiro", prestamo.getId());
        return true;
    }

    // ------------------------------------------- Seccion 2b: reservas no retiradas

    @Override
    public int expirarReservasNoRetiradas() {
        List<ReservaLibroDTO> candidatas = reservaLibroDAO
                .findByEstadoIgnoreCaseAndFechaLimiteRetiroBefore(RESERVA_NOTIFICADA, LocalDateTime.now());
        int procesadas = 0;
        for (ReservaLibroDTO candidata : candidatas) {
            if (enTransaccion(candidata.getId(), "expirar reserva",
                    () -> expirarReserva(candidata.getId()))) {
                procesadas++;
            }
        }
        log.info("Reservas de libro expiradas por no retiro: {}", procesadas);
        return procesadas;
    }

    private boolean expirarReserva(Long reservaId) {
        ReservaLibroDTO reserva = reservaLibroDAO.findById(reservaId).orElse(null);
        if (reserva == null || !RESERVA_NOTIFICADA.equalsIgnoreCase(reserva.getEstado())
                || reserva.getFechaLimiteRetiro() == null
                || !reserva.getFechaLimiteRetiro().isBefore(LocalDateTime.now())) {
            return false;
        }

        Long ejemplarId = reserva.getEjemplarId();
        reserva.setEstado(RESERVA_EXPIRADA);
        reservaLibroDAO.update(reserva.getId(), reserva);

        if (ejemplarId != null) {
            liberarEjemplarYReasignar(ejemplarId, EJEMPLAR_RESERVADO);
        }

        notificar(reserva.getUsuarioId(), "RESERVA_EXPIRADA", "Tu reserva expiro",
                "Tu reserva del libro \"" + tituloDeLibro(reserva.getLibroId()) + "\" expiro porque no lo retiraste "
                        + "antes del " + reserva.getFechaLimiteRetiro() + ".");
        log.info("Reserva {} expirada por no retiro", reserva.getId());
        return true;
    }

    // ------------------------- Multa creciente, levantar sancion, liberar salas

    @Override
    public int actualizarMultasPorMora() {
        List<PrestamoDTO> vencidos = prestamoDAO.findByEstadoIgnoreCase(PRESTAMO_VENCIDO);
        int actualizadas = 0;
        for (PrestamoDTO vencido : vencidos) {
            if (enTransaccion(vencido.getId(), "actualizar multa por mora", () -> {
                PrestamoDTO p = prestamoDAO.findById(vencido.getId()).orElse(null);
                return p != null && PRESTAMO_VENCIDO.equalsIgnoreCase(p.getEstado()) && sincronizarMultaPorMora(p);
            })) {
                actualizadas++;
            }
        }
        log.info("Multas por mora actualizadas: {} de {} prestamos vencidos", actualizadas, vencidos.size());
        return actualizadas;
    }

    private boolean sincronizarMultaPorMora(PrestamoDTO prestamo) {
        if (prestamo.getFechaVencimiento() == null) {
            return false;
        }
        long dias = Math.max(1, ChronoUnit.DAYS.between(prestamo.getFechaVencimiento(), LocalDate.now()));
        BigDecimal total = MULTA_POR_DIA_MORA.multiply(BigDecimal.valueOf(dias));

        List<MultaDTO> moras = multaDAO.findByPrestamoId(prestamo.getId()).stream()
                .filter(m -> m.getMotivo() != null && m.getMotivo().startsWith(MOTIVO_MORA))
                .filter(m -> !MULTA_ANULADA.equalsIgnoreCase(m.getEstado()))
                .toList();
        BigDecimal pagado = moras.stream()
                .filter(m -> MULTA_PAGADA.equalsIgnoreCase(m.getEstado()))
                .map(MultaDTO::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal restante = total.subtract(pagado);
        if (restante.signum() <= 0) {
            return false;
        }

        String motivo = MOTIVO_MORA + " (prestamo #" + prestamo.getId() + ", " + dias
                + (dias == 1 ? " dia" : " dias") + " de retraso)";
        Optional<MultaDTO> pendiente = moras.stream()
                .filter(m -> MULTA_PENDIENTE.equalsIgnoreCase(m.getEstado()))
                .findFirst();
        if (pendiente.isPresent()) {
            MultaDTO multa = pendiente.get();
            if (multa.getMonto().compareTo(restante) == 0) {
                return false;
            }
            multa.setMonto(restante);
            multa.setMotivo(motivo);
            multaDAO.update(multa.getId(), multa);
            return true;
        }
        MultaDTO multa = new MultaDTO();
        multa.setMonto(restante);
        multa.setMotivo(motivo);
        multa.setFechaGeneracion(LocalDateTime.now());
        multa.setEstado(MULTA_PENDIENTE);
        multa.setUsuarioId(prestamo.getUsuarioId());
        multa.setPrestamoId(prestamo.getId());
        multaDAO.save(multa);
        return true;
    }

    @Override
    public int levantarSancionesPorDevolucion() {
        List<SancionDTO> activas = sancionDAO.findByTipoIgnoreCaseAndEstadoIgnoreCase(SANCION_BLOQUEO, SANCION_ACTIVA).stream()
                .filter(x -> x.getPrestamoId() != null)
                .filter(x -> x.getMotivo() != null && x.getMotivo().startsWith(MOTIVO_SANCION_MORA))
                .toList();
        int levantadas = 0;
        for (SancionDTO candidata : activas) {
            if (enTransaccion(candidata.getId(), "levantar sancion por devolucion", () -> {
                SancionDTO sancion = sancionDAO.findById(candidata.getId()).orElse(null);
                if (sancion == null || !SANCION_ACTIVA.equalsIgnoreCase(sancion.getEstado())) {
                    return false;
                }
                PrestamoDTO prestamo = prestamoDAO.findById(sancion.getPrestamoId()).orElse(null);
                if (prestamo == null || !PRESTAMO_DEVUELTO.equalsIgnoreCase(prestamo.getEstado())) {
                    return false;
                }
                sancion.setEstado(SANCION_FINALIZADA);
                sancion.setFechaFin(LocalDateTime.now());
                sancionDAO.update(sancion.getId(), sancion);
                notificar(sancion.getUsuarioId(), "SANCION_FINALIZADA", "Sancion levantada",
                        "Devolviste el libro y se levanto el bloqueo de prestamos. Si tienes multas pendientes, "
                                + "debes pagarlas para volver a pedir libros.");
                return true;
            })) {
                levantadas++;
            }
        }
        log.info("Sanciones por retraso levantadas: {}", levantadas);
        return levantadas;
    }

    @Override
    public int liberarReservasSalaVencidas() {
        LocalDateTime ahora = LocalDateTime.now();
        int liberadas = 0;

        for (ReservaSalaDTO candidata : reservaSalaDAO.findByEstadoIgnoreCase(SALA_PENDIENTE_PAGO)) {
            if (enTransaccion(candidata.getId(), "cancelar reserva de sala sin pago", () -> {
                ReservaSalaDTO r = reservaSalaDAO.findById(candidata.getId()).orElse(null);
                if (r == null || !SALA_PENDIENTE_PAGO.equalsIgnoreCase(r.getEstado())
                        || r.getFechaCreacion() == null
                        || !r.getFechaCreacion().plusMinutes(MINUTOS_PAGO_RESERVA_SALA).isBefore(ahora)) {
                    return false;
                }
                r.setEstado(SALA_CANCELADA);
                r.setFechaCancelacion(ahora);
                reservaSalaDAO.update(r.getId(), r);
                notificar(r.getUsuarioId(), "RESERVA_SALA_CANCELADA", "Reserva de sala cancelada",
                        "Tu reserva de sala del " + r.getFechaInicio() + " se cancelo porque no se pago en "
                                + MINUTOS_PAGO_RESERVA_SALA + " minutos. La sala quedo disponible.");
                return true;
            })) {
                liberadas++;
            }
        }

        for (ReservaSalaDTO candidata : reservaSalaDAO.findByEstadoIgnoreCase(SALA_CONFIRMADA)) {
            if (candidata.getFechaFin() == null || !candidata.getFechaFin().isBefore(ahora)) {
                continue;
            }
            if (enTransaccion(candidata.getId(), "finalizar reserva de sala", () -> {
                ReservaSalaDTO r = reservaSalaDAO.findById(candidata.getId()).orElse(null);
                if (r == null || !SALA_CONFIRMADA.equalsIgnoreCase(r.getEstado())) {
                    return false;
                }
                r.setEstado(SALA_FINALIZADA);
                reservaSalaDAO.update(r.getId(), r);
                return true;
            })) {
                liberadas++;
            }
        }
        log.info("Reservas de sala liberadas: {}", liberadas);
        return liberadas;
    }

    @Override
    public int cancelarPedidosInterciudadSinPago() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(PedidoInterciudadServiceImpl.MINUTOS_PAGO_ENVIO);
        List<PedidoInterciudadDTO> candidatos = pedidoInterciudadDAO
                .findByEstadoIgnoreCaseOrderByFechaSolicitudAsc(PEDIDO_PENDIENTE_PAGO).stream()
                .filter(p -> p.getFechaSolicitud() != null && p.getFechaSolicitud().isBefore(limite))
                .toList();
        int procesados = 0;
        for (PedidoInterciudadDTO candidato : candidatos) {
            if (enTransaccion(candidato.getId(), "cancelar pedido interciudad sin pago",
                    () -> cancelarPedidoSinPago(candidato.getId(), limite))) {
                procesados++;
            }
        }
        log.info("Pedidos interciudad cancelados por falta de pago: {}", procesados);
        return procesados;
    }

    @Override
    public int procesarPedidosNoRecogidos() {
        LocalDateTime ahora = LocalDateTime.now();
        List<PedidoInterciudadDTO> candidatos = pedidoInterciudadDAO
                .findByEstadoIgnoreCaseOrderByFechaSolicitudAsc(PEDIDO_EN_ESPERA_DE_RECOGIDA).stream()
                .filter(p -> p.getFechaLimiteRecogida() != null && p.getFechaLimiteRecogida().isBefore(ahora))
                .toList();
        int procesados = 0;
        for (PedidoInterciudadDTO candidato : candidatos) {
            if (enTransaccion(candidato.getId(), "procesar pedido interciudad no recogido",
                    () -> marcarPedidoNoRecogido(candidato.getId()))) {
                procesados++;
            }
        }
        log.info("Pedidos interciudad no recogidos a tiempo: {}", procesados);
        return procesados;
    }

    private boolean marcarPedidoNoRecogido(Long pedidoId) {
        LocalDateTime ahora = LocalDateTime.now();
        PedidoInterciudadDTO pedido = pedidoInterciudadDAO.findById(pedidoId).orElse(null);
        if (pedido == null || !PEDIDO_EN_ESPERA_DE_RECOGIDA.equalsIgnoreCase(pedido.getEstado())
                || pedido.getFechaLimiteRecogida() == null || !pedido.getFechaLimiteRecogida().isBefore(ahora)) {
            return false;
        }

        // 1) El libro queda pendiente de traslado hacia la sede de origen (lo toma un repartidor).
        pedido.setEstado(PEDIDO_DEVUELTO_PENDIENTE_TRASLADO);
        pedidoInterciudadDAO.update(pedido.getId(), pedido);

        // 2) El préstamo nunca se entregó: se cancela para que no cuente como vigente.
        if (pedido.getPrestamoId() != null) {
            prestamoDAO.findById(pedido.getPrestamoId()).ifPresent(p -> {
                p.setEstado(PRESTAMO_CANCELADO);
                prestamoDAO.update(p.getId(), p);
            });
        }

        // 3) Multa económica.
        MultaDTO multa = new MultaDTO();
        multa.setMonto(MULTA_NO_RECOGIDO);
        multa.setMotivo(MOTIVO_NO_RECOGIDO + " (pedido #" + pedido.getId() + ")");
        multa.setFechaGeneracion(ahora);
        multa.setEstado(MULTA_PENDIENTE);
        multa.setUsuarioId(pedido.getUsuarioId());
        multa.setPrestamoId(pedido.getPrestamoId());
        multaDAO.save(multa);

        // 4) Sanción temporal: bloquea nuevos préstamos y pedidos hasta fechaFin.
        SancionDTO sancion = new SancionDTO();
        sancion.setTipo(SANCION_BLOQUEO);
        sancion.setMotivo(MOTIVO_SANCION_NO_RECOGIDO + " (pedido #" + pedido.getId() + ")");
        sancion.setFechaAplicacion(ahora);
        sancion.setFechaFin(ahora.plusDays(DIAS_SANCION_NO_RECOGIDO));
        sancion.setEstado(SANCION_ACTIVA);
        sancion.setUsuarioId(pedido.getUsuarioId());
        sancion.setPrestamoId(pedido.getPrestamoId());
        sancionDAO.save(sancion);

        notificar(pedido.getUsuarioId(), "PEDIDO_NO_RECOGIDO", "No recogiste tu pedido a tiempo",
                "El pedido #" + pedido.getId() + " no se recogió antes de " + pedido.getFechaLimiteRecogida()
                        + ". Se generó una multa de $" + MULTA_NO_RECOGIDO + " y no podrás pedir libros hasta pagarla "
                        + "y hasta el " + sancion.getFechaFin().toLocalDate() + ".");
        log.info("Pedido interciudad {} no recogido: multa y sancion aplicadas", pedido.getId());
        return true;
    }

    @Override
    public int finalizarSancionesVencidas() {
        LocalDateTime ahora = LocalDateTime.now();
        List<SancionDTO> candidatas = sancionDAO
                .findByTipoIgnoreCaseAndEstadoIgnoreCase(SANCION_BLOQUEO, SANCION_ACTIVA).stream()
                .filter(s -> s.getFechaFin() != null && s.getFechaFin().isBefore(ahora))
                .toList();
        int finalizadas = 0;
        for (SancionDTO candidata : candidatas) {
            if (enTransaccion(candidata.getId(), "finalizar sancion vencida", () -> {
                SancionDTO sancion = sancionDAO.findById(candidata.getId()).orElse(null);
                if (sancion == null || !SANCION_ACTIVA.equalsIgnoreCase(sancion.getEstado())
                        || sancion.getFechaFin() == null || !sancion.getFechaFin().isBefore(ahora)) {
                    return false;
                }
                sancion.setEstado(SANCION_FINALIZADA);
                sancionDAO.update(sancion.getId(), sancion);
                notificar(sancion.getUsuarioId(), "SANCION_FINALIZADA", "Sancion finalizada",
                        "Tu sancion termino. Si tienes multas pendientes, debes pagarlas para volver a pedir libros.");
                return true;
            })) {
                finalizadas++;
            }
        }
        log.info("Sanciones finalizadas por vencimiento: {}", finalizadas);
        return finalizadas;
    }

    private boolean cancelarPedidoSinPago(Long pedidoId, LocalDateTime limite) {
        PedidoInterciudadDTO pedido = pedidoInterciudadDAO.findById(pedidoId).orElse(null);
        if (pedido == null || !PEDIDO_PENDIENTE_PAGO.equalsIgnoreCase(pedido.getEstado())
                || pedido.getFechaSolicitud() == null || !pedido.getFechaSolicitud().isBefore(limite)) {
            return false;
        }

        pedido.setEstado(PEDIDO_CANCELADO);
        pedido.setFechaCancelacion(LocalDateTime.now());
        pedidoInterciudadDAO.update(pedido.getId(), pedido);

        if (pedido.getPrestamoId() != null) {
            prestamoDAO.findById(pedido.getPrestamoId()).ifPresent(p -> {
                p.setEstado(PRESTAMO_CANCELADO);
                prestamoDAO.update(p.getId(), p);
            });
        }
        liberarEjemplarYReasignar(pedido.getEjemplarId(), EJEMPLAR_RESERVADO);

        notificar(pedido.getUsuarioId(), "PEDIDO_CANCELADO_SIN_PAGO", "Pedido cancelado por falta de pago",
                "Tu pedido #" + pedido.getId() + " se canceló porque el envío no se pagó en "
                        + PedidoInterciudadServiceImpl.MINUTOS_PAGO_ENVIO + " minutos.");
        log.info("Pedido interciudad {} cancelado por falta de pago", pedido.getId());

        notificacionService.notificarSede(pedido.getSedeOrigenId(), "PEDIDO_NO_RECOGIDO",
        "Pedido no recogido, el libro regresará",
        "El pedido #" + pedido.getId() + " no fue recogido en la sede de devolución y volverá a tu sede.");

        return true;
    }

    // ------------------------------------------------ Seccion 3: RECORDATORIOS PREVENTIVOS

    @Override
    public int enviarRecordatoriosPrestamosProximos() {
        LocalDate hoy = LocalDate.now();
        LocalDate en24h = hoy.plusDays(1);
        LocalDate en48h = hoy.plusDays(2);
        int enviados = 0;

        // Recordatorios 48 horas antes de vencer
        List<PrestamoDTO> prestamos48h = prestamoDAO.findByEstadoAndFechaVencimiento(PRESTAMO_ACTIVO, en48h);
        for (PrestamoDTO p : prestamos48h) {
            if (enTransaccion(p.getId(), "enviar recordatorio prestamo 48h", () -> {
                notificar(p.getUsuarioId(), "RECORDATORIO_PRESTAMO", "Tu préstamo vence en 2 días",
                        "Recuerda que la fecha límite para devolver el libro \"" + tituloDeEjemplar(p.getEjemplarId())
                                + "\" (Préstamo #" + p.getId() + ") es el " + p.getFechaVencimiento() + ".");
                return true;
            })) {
                enviados++;
            }
        }

        // Recordatorios 24 horas antes de vencer
        List<PrestamoDTO> prestamos24h = prestamoDAO.findByEstadoAndFechaVencimiento(PRESTAMO_ACTIVO, en24h);
        for (PrestamoDTO p : prestamos24h) {
            if (enTransaccion(p.getId(), "enviar recordatorio prestamo 24h", () -> {
                notificar(p.getUsuarioId(), "RECORDATORIO_PRESTAMO", "¡Atención! Tu préstamo vence mañana",
                        "Mañana " + p.getFechaVencimiento() + " vence el plazo de devolución para el libro \""
                                + tituloDeEjemplar(p.getEjemplarId()) + "\" (Préstamo #" + p.getId() + ").");
                return true;
            })) {
                enviados++;
            }
        }

        log.info("Recordatorios de prestamos enviados: {}", enviados);
        return enviados;
    }

    @Override
    public int enviarRecordatoriosEventosProximos() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime dentro24h = ahora.plusHours(24);
        int enviados = 0;

        List<EventoDTO> eventos = eventoDAO.findByEstadoAndFechaInicioBetween("PROGRAMADO", ahora, dentro24h);
        for (EventoDTO evento : eventos) {
            List<InscripcionEventoDTO> inscripciones = inscripcionEventoDAO
                    .findByEventoIdAndEstadoIgnoreCase(evento.getId(), "ACTIVA");
            for (InscripcionEventoDTO inscripcion : inscripciones) {
                if (enTransaccion(inscripcion.getId(), "enviar recordatorio evento", () -> {
                    notificar(inscripcion.getUsuarioId(), "RECORDATORIO_EVENTO",
                            "Recordatorio de Evento: " + evento.getTitulo(),
                            "Te recordamos que el evento '" + evento.getTitulo() + "' iniciará el "
                                    + evento.getFechaInicio().toLocalDate() + " a las " + evento.getFechaInicio().toLocalTime() + ".");
                    return true;
                })) {
                    enviados++;
                }
            }
        }

        log.info("Recordatorios de eventos enviados: {}", enviados);
        return enviados;
    }

    @Override
    public int enviarRecordatoriosSalasProximas() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime dentro2h = ahora.plusHours(2);
        int enviados = 0;

        // Solo reservas confirmadas (pagadas): las pendientes de pago se cancelan solas a los 30 minutos.
        List<ReservaSalaDTO> reservas = reservaSalaDAO.findByEstadoInAndFechaInicioBetween(
                List.of(SALA_CONFIRMADA), ahora, dentro2h);

        for (ReservaSalaDTO r : reservas) {
            if (enTransaccion(r.getId(), "enviar recordatorio sala", () -> {
                String nombreSala = salaEstudioDAO.findById(r.getSalaId())
                        .map(SalaEstudioDTO::getNombre)
                        .orElse("Sala #" + r.getSalaId());
                // El número de reserva hace único el mensaje, y eso permite detectar si ya se avisó.
                String mensaje = "Tu reserva #" + r.getId() + " en la " + nombreSala + " está programada para el "
                        + r.getFechaInicio().toLocalDate() + " a las " + r.getFechaInicio().toLocalTime() + ".";
                if (notificacionDAO.existsByUsuarioIdAndTipoAndMensaje(
                        r.getUsuarioId(), "RECORDATORIO_RESERVA_SALA", mensaje)) {
                    return false; // ya se envió en una ejecución anterior
                }
                notificar(r.getUsuarioId(), "RECORDATORIO_RESERVA_SALA", "Tu reserva de sala iniciará pronto", mensaje);
                return true;
            })) {
                enviados++;
            }
        }

        log.info("Recordatorios de reservas de salas enviados: {}", enviados);
        return enviados;
    }
    // -------------------------------------------------------------- privados

    private void liberarEjemplarYReasignar(Long ejemplarId, String estadoActual) {
        if (ejemplarDAO.cambiarEstadoSiEs(ejemplarId, estadoActual, EJEMPLAR_DISPONIBLE) == 0) {
            log.warn("Ejemplar {} no estaba en estado {}; no se libera ni se reasigna", ejemplarId, estadoActual);
            return;
        }
        boolean reasignado = reservaLibroService.asignarEjemplarAReservaEnEspera(ejemplarId);
        log.info("Ejemplar {} liberado (reasignado a la lista de espera: {})", ejemplarId, reasignado);
    }

    private boolean enTransaccion(Long id, String accion, Supplier<Boolean> trabajo) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            return Boolean.TRUE.equals(template.execute(status -> trabajo.get()));
        } catch (Exception e) {
            log.error("Error al {} (id {})", accion, id, e);
            return false;
        }
    }

    private String tituloDeEjemplar(Long ejemplarId) {
        return ejemplarDAO.findById(ejemplarId)
                .map(EjemplarDTO::getLibroId)
                .map(this::tituloDeLibro)
                .orElse("(ejemplar " + ejemplarId + ")");
    }

    private String tituloDeLibro(Long libroId) {
        return libroDAO.findById(libroId).map(LibroDTO::getTitulo).orElse("(libro " + libroId + ")");
    }

    private void notificar(Long usuarioId, String tipo, String titulo, String mensaje) {
        NotificacionDTO n = new NotificacionDTO();
        n.setTipo(tipo);
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setCanal("APP");
        n.setFechaCreacion(LocalDateTime.now());
        n.setLeida(false);
        n.setUsuarioId(usuarioId);
        notificacionDAO.save(n);
    }
}