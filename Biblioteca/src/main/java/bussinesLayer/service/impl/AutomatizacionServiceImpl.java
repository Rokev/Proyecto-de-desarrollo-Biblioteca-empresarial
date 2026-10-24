package bussinesLayer.service.impl;

import bussinesLayer.dto.EjemplarDTO;
import bussinesLayer.dto.LibroDTO;
import bussinesLayer.dto.MultaDTO;
import bussinesLayer.dto.NotificacionDTO;
import bussinesLayer.dto.PrestamoDTO;
import bussinesLayer.dto.ReservaLibroDTO;
import bussinesLayer.dto.ReservaSalaDTO;
import bussinesLayer.dto.SancionDTO;
import bussinesLayer.service.AutomatizacionService;
import bussinesLayer.service.ReservaLibroService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import persistanceLayer.dao.EjemplarDAO;
import persistanceLayer.dao.LibroDAO;
import persistanceLayer.dao.MultaDAO;
import persistanceLayer.dao.NotificacionDAO;
import persistanceLayer.dao.PrestamoDAO;
import persistanceLayer.dao.ReservaLibroDAO;
import persistanceLayer.dao.ReservaSalaDAO;
import persistanceLayer.dao.SancionDAO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Esta clase NO es @Transactional a proposito: cada registro se procesa en su propia transaccion
 * (REQUIRES_NEW). Asi, si falla uno, los demas se guardan igual y el error queda en el log.
 */
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

    /** TODO RF-75/RF-76: pasar a configuracion del administrador cuando exista la entidad de politicas. */
    private static final BigDecimal MULTA_POR_DIA_MORA = new BigDecimal("2000.00");
    /** Minutos que tiene el usuario para pagar una reserva de sala antes de que se libere. */
    private static final int MINUTOS_PAGO_RESERVA_SALA = 30;
    /** Horas para retirar una solicitud de prestamo en sede (RF-76b). Igual a los 2 dias de las reservas. */
    private static final int HORAS_RETIRO_SOLICITUD = 48;

    private final PrestamoDAO prestamoDAO;
    private final ReservaLibroDAO reservaLibroDAO;
    private final ReservaSalaDAO reservaSalaDAO;
    private final SancionDAO sancionDAO;
    private final EjemplarDAO ejemplarDAO;
    private final LibroDAO libroDAO;
    private final MultaDAO multaDAO;
    private final NotificacionDAO notificacionDAO;
    private final ReservaLibroService reservaLibroService;
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

    private boolean marcarVencidoYMultar(Long prestamoId) {
        // Se vuelve a leer dentro de la transaccion: si otro proceso ya lo cambio, no se duplica la multa.
        PrestamoDTO prestamo = prestamoDAO.findById(prestamoId).orElse(null);
        if (prestamo == null || !PRESTAMO_ACTIVO.equalsIgnoreCase(prestamo.getEstado())) {
            return false;
        }

        prestamo.setEstado(PRESTAMO_VENCIDO);
        prestamoDAO.update(prestamo.getId(), prestamo);

        // Multa del primer dia de retraso; luego crece con actualizarMultasPorMora().
        sincronizarMultaPorMora(prestamo);

        boolean yaTieneSancion = sancionDAO.findByPrestamoId(prestamo.getId()).stream()
                .anyMatch(x -> SANCION_BLOQUEO.equalsIgnoreCase(x.getTipo()));
        if (!yaTieneSancion) {
            SancionDTO sancion = new SancionDTO();
            sancion.setTipo(SANCION_BLOQUEO);
            sancion.setMotivo(MOTIVO_SANCION_MORA + " (prestamo #" + prestamo.getId() + ")");
            sancion.setFechaAplicacion(LocalDateTime.now());
            sancion.setFechaFin(null); // indefinida: se levanta al devolver el libro
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
        // Solo retiro en sede: los pedidos interciudad (y a domicilio) no tienen retiro; sus fallos
        // se manejan con las entregas fallidas (RF-84 / RF-55).
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
        // Primero se marca EXPIRADA para que no vuelva a competir por el ejemplar al buscar al siguiente.
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

    /**
     * Deja la multa al dia: dias de retraso x tarifa diaria, descontando lo ya pagado. Si el usuario ya
     * pago la anterior y sigue sin devolver, se crea una nueva por la diferencia.
     * Devuelve true si creo o cambio una multa.
     */
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

    // -------------------------------------------------------------- privados

    /**
     * Devuelve el ejemplar a DISPONIBLE y se lo ofrece al primero de la lista de espera.
     * Si nadie espera, queda DISPONIBLE. {@code asignarEjemplarAReservaEnEspera} ya lo aparta
     * (RESERVADO), pasa la reserva a NOTIFICADA con su nuevo plazo y avisa al usuario.
     */
    private void liberarEjemplarYReasignar(Long ejemplarId, String estadoActual) {
        if (ejemplarDAO.cambiarEstadoSiEs(ejemplarId, estadoActual, EJEMPLAR_DISPONIBLE) == 0) {
            log.warn("Ejemplar {} no estaba en estado {}; no se libera ni se reasigna", ejemplarId, estadoActual);
            return;
        }
        boolean reasignado = reservaLibroService.asignarEjemplarAReservaEnEspera(ejemplarId);
        log.info("Ejemplar {} liberado (reasignado a la lista de espera: {})", ejemplarId, reasignado);
    }

    /** Ejecuta la accion en una transaccion nueva; si falla, solo se pierde ese registro. */
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
