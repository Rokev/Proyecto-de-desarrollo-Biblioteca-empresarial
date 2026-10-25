package bussinesLayer.service.impl;

import bussinesLayer.dto.*;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.ConfiguracionSistemaService;
import bussinesLayer.service.PedidoInterciudadService;
import bussinesLayer.service.SuscripcionService;
import bussinesLayer.service.TarifaEnvioService;
import bussinesLayer.service.ReservaLibroService;
import bussinesLayer.service.ValidadorUsuarioService;
import bussinesLayer.service.NotificacionService;
import bussinesLayer.service.AutomatizacionService;
import bussinesLayer.util.ClavePlazo;
import bussinesLayer.util.ConceptoMulta;
import bussinesLayer.util.EstadoPago;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PedidoInterciudadServiceImpl implements PedidoInterciudadService {

    private static final String ESTADO_SOLICITADO = "SOLICITADO";
    private static final String ESTADO_EN_TRANSITO = "EN_TRANSITO";
    private static final String ESTADO_ENTREGADO = "ENTREGADO";
    private static final String ESTADO_NO_RECIBIDO = "NO_RECIBIDO";
    private static final String ESTADO_EN_ESPERA_DE_RECOGIDA = "EN_ESPERA_DE_RECOGIDA";
    private static final String ESTADO_DEVUELTO_PENDIENTE_TRASLADO = "DEVUELTO_PENDIENTE_TRASLADO";
    private static final String ESTADO_EN_CAMINO_SEDE_ORIGEN = "EN_CAMINO_SEDE_ORIGEN";
    private static final String ESTADO_CANCELADO = "CANCELADO";
    private static final String ESTADO_CERRADO = "CERRADO";
    private static final String ESTADO_PENDIENTE_PAGO = "PENDIENTE_PAGO";

    private static final String EJEMPLAR_DISPONIBLE = "DISPONIBLE";
    private static final String EJEMPLAR_RESERVADO = "RESERVADO";
    private static final String EJEMPLAR_EN_TRANSITO = "EN_TRANSITO";
    private static final String EJEMPLAR_PRESTADO = "PRESTADO";


    private static final int DIAS_MAXIMO_SOLICITUD = 7;
    static final int MINUTOS_PAGO_ENVIO = 30;

    private final PedidoInterciudadDAO pedidoDAO;
    private final UsuarioDAO usuarioDAO;
    private final EjemplarDAO ejemplarDAO;
    private final SedeDAO sedeDAO;
    private final PagoDAO pagoDAO;
    private final PrestamoDAO prestamoDAO;
    private final SancionDAO sancionDAO;
    private final MultaDAO multaDAO;
    private final NotificacionDAO notificacionDAO;
    private final SuscripcionService suscripcionService;
    private final TarifaEnvioService tarifaEnvioService;
    private final ConfiguracionSistemaService configuracionService;
    private final ReservaLibroService reservaLibroService;
    private final NotificacionService notificacionService;
    private final ValidadorUsuarioService validadorUsuarioService;
    private final AutomatizacionService automatizacionService;

    // ------------------------------------------------------------Solicitar

    @Override
    public PedidoInterciudadDTO solicitarPedido(SolicitudPedidoInterciudadDTO solicitud) {
        log.info("Solicitud pedido interciudad para usuario {} y libro {}", solicitud.getUsuarioId(), solicitud.getLibroId());

        if (!usuarioDAO.existsById(solicitud.getUsuarioId())) {
            throw new ResourceNotFoundException("Usuario no encontrado: " + solicitud.getUsuarioId());
        }
        suscripcionService.exigirSuscripcionVigente(solicitud.getUsuarioId());

        // Sanciones activas y multas pendientes (mismo control que préstamos y reservas)
        validadorUsuarioService.validarUsuarioHabilitadoParaOperaciones(solicitud.getUsuarioId());

        // La sede de devolución debe existir y estar en operación
        SedeDTO sedeDevolucion = sedeDAO.findById(solicitud.getSedeDevolucionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sede de devolución no encontrada: " + solicitud.getSedeDevolucionId()));
        if (!Boolean.TRUE.equals(sedeDevolucion.getEnOperacion())) {
            throw new BusinessValidationException(
                    "La sede de devolución " + sedeDevolucion.getNombre() + " no está en operación");
        }

        LocalDate hoy = LocalDate.now();
        if (solicitud.getFechaRecepcion().isAfter(hoy.plusDays(DIAS_MAXIMO_SOLICITUD))) {
            throw new BusinessValidationException("La fecha de recepción no puede superar los 7 días a partir de hoy");
        }


        EjemplarDTO ejemplar = reservarEjemplarConTarifa(solicitud.getLibroId(), solicitud.getCiudadEntrega());
        String ciudadOrigen = obtenerCiudadDeSede(ejemplar.getSedeId());
        BigDecimal costoEnvio = tarifaEnvioService.calcularCosto(ciudadOrigen, solicitud.getCiudadEntrega());
        LocalDateTime ahora = LocalDateTime.now();

        // Crear Préstamo Vinculado
        PrestamoDTO prestamo = new PrestamoDTO();
        prestamo.setFechaSolicitud(ahora);
        prestamo.setEstado("SOLICITADO");
        prestamo.setModalidad("INTERCIUDAD");
        prestamo.setDireccionEntrega(solicitud.getDireccionEntrega());
        prestamo.setUsuarioId(solicitud.getUsuarioId());
        prestamo.setEjemplarId(ejemplar.getId());
        prestamo.setSedeRetiroId(solicitud.getSedeDevolucionId());
        PrestamoDTO prestamoGuardado = prestamoDAO.save(prestamo);

        // Crear Pedido Interciudad
        PedidoInterciudadDTO pedido = new PedidoInterciudadDTO();
        pedido.setEstado(ESTADO_PENDIENTE_PAGO);
        pedido.setCiudadEntrega(solicitud.getCiudadEntrega().trim());
        pedido.setDireccionEntrega(solicitud.getDireccionEntrega().trim());
        pedido.setFechaRecepcion(solicitud.getFechaRecepcion());
        pedido.setHoraRecepcion(solicitud.getHoraRecepcion().trim());
        pedido.setCostoEnvio(costoEnvio.doubleValue());
        pedido.setFechaSolicitud(ahora);
        pedido.setUsuarioId(solicitud.getUsuarioId());
        pedido.setEjemplarId(ejemplar.getId());
        pedido.setSedeOrigenId(ejemplar.getSedeId());
        pedido.setSedeDevolucionId(solicitud.getSedeDevolucionId());
        pedido.setPrestamoId(prestamoGuardado.getId());

        PedidoInterciudadDTO guardado = pedidoDAO.save(pedido);
        notificar(solicitud.getUsuarioId(), "PEDIDO_PENDIENTE_PAGO", "Paga el envío para confirmar tu pedido",
                "Tu pedido #" + guardado.getId() + " quedó pendiente de pago ($" + costoEnvio + "). Tienes "
                        + MINUTOS_PAGO_ENVIO + " minutos para pagar el envío o se cancelará automáticamente.");
        return guardado;
    }

    // ------------------------------------------------------------ Cancelar

    @Override
    public PedidoInterciudadDTO cancelarPedido(Long pedidoId, Long usuarioId) {
        PedidoInterciudadDTO pedido = buscarPedido(pedidoId);
        if (!pedido.getUsuarioId().equals(usuarioId)) {
            throw new BusinessValidationException("El pedido no pertenece al usuario especificado");
        }
        boolean pendientePago = ESTADO_PENDIENTE_PAGO.equalsIgnoreCase(pedido.getEstado());
        if (!pendientePago && !ESTADO_SOLICITADO.equalsIgnoreCase(pedido.getEstado())) {
            notificar(usuarioId, "CANCELACION_BLOQUEADA", "No se pudo cancelar el pedido",
                    "El pedido #" + pedidoId + " ya no está en estado SOLICITADO ni PENDIENTE_PAGO y no se puede cancelar.");
            throw new BusinessValidationException("Solo se pueden cancelar pedidos en estado SOLICITADO o PENDIENTE_PAGO");
        }

        cambiarEstadoEjemplar(pedido.getEjemplarId(), EJEMPLAR_RESERVADO, EJEMPLAR_DISPONIBLE);
        PrestamoDTO prestamo = prestamoDAO.findById(pedido.getPrestamoId()).orElseThrow();
        prestamo.setEstado("CANCELADO");
        prestamoDAO.update(prestamo.getId(), prestamo);

        pedido.setEstado(ESTADO_CANCELADO);
        pedido.setFechaCancelacion(LocalDateTime.now());
        Optional<PagoDTO> reembolso = reembolsarEnvio(pedido);
        PedidoInterciudadDTO cancelado = pedidoDAO.update(pedido.getId(), pedido).orElseThrow();

        String detalleReembolso = reembolso
                .map(p -> " Se reembolsó $" + p.getMonto() + " del envío (Ref: " + p.getReferencia() + ").")
                .orElse("");
        notificar(usuarioId, "PEDIDO_CANCELADO", "Pedido cancelado",
                "El pedido #" + pedidoId + " fue cancelado." + detalleReembolso);
        return cancelado;
    }

    // ------------------------------------------------------------ Consultas

    @Override
    @Transactional(readOnly = true)
    public PedidoInterciudadDTO obtenerPedidoPorId(Long id) {
        return buscarPedido(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoInterciudadDTO> obtenerPedidosDeUsuario(Long usuarioId, String estado) {
        if (estado == null || estado.isBlank()) {
            return pedidoDAO.findByUsuarioIdOrderByFechaSolicitudDesc(usuarioId);
        }
        return pedidoDAO.findByUsuarioIdAndEstadoIgnoreCaseOrderByFechaSolicitudDesc(usuarioId, estado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoInterciudadDTO> obtenerPedidosSedeOrigen(Long sedeOrigenId) {
        return pedidoDAO.findBySedeOrigenAndEstados(sedeOrigenId, List.of(ESTADO_SOLICITADO, ESTADO_EN_CAMINO_SEDE_ORIGEN));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoInterciudadDTO> obtenerPedidosSedeDevolucion(Long sedeDevolucionId) {
        if (!sedeDAO.existsById(sedeDevolucionId)) {
            throw new ResourceNotFoundException("Sede no encontrada: " + sedeDevolucionId);
        }
        // Pedidos que llegan, se devuelven o esperan recogida en esta sede
        return pedidoDAO.findBySedeDevolucionAndEstados(sedeDevolucionId, List.of(
                ESTADO_EN_TRANSITO, ESTADO_ENTREGADO, ESTADO_NO_RECIBIDO,
                ESTADO_EN_ESPERA_DE_RECOGIDA, ESTADO_DEVUELTO_PENDIENTE_TRASLADO));
    }

    // ------------------------------------------------------------ Acciones Bibliotecario

    @Override
    public PedidoInterciudadDTO despacharPedido(Long pedidoId, Long bibliotecarioId, Long repartidorId) {
        PedidoInterciudadDTO pedido = buscarPedido(pedidoId);
        validarEstado(pedido, ESTADO_SOLICITADO);
        if (pagoDAO.findEnvioPagadoPorPedido(pedidoId).isEmpty()) {
            throw new BusinessValidationException(
                    "El pedido #" + pedidoId + " no se puede despachar: el costo de envío aún no ha sido pagado");
        }

        cambiarEstadoEjemplar(pedido.getEjemplarId(), EJEMPLAR_RESERVADO, EJEMPLAR_EN_TRANSITO);
        pedido.setEstado(ESTADO_EN_TRANSITO);
        pedido.setFechaTransito(LocalDateTime.now());
        pedido.setBibliotecarioId(bibliotecarioId);
        pedido.setRepartidorId(repartidorId);

        notificar(pedido.getUsuarioId(), "PEDIDO_EN_TRANSITO", "Tu pedido va en camino",
                "El pedido #" + pedidoId + " se encuentra en tránsito a la dirección indicada.");
        notificacionService.notificarSede(pedido.getSedeDevolucionId(), "PEDIDO_DESPACHADO", "Pedido interciudad despachado",
        "El pedido #" + pedidoId + " salió hacia el usuario y deberá devolverse en tu sede ("
                + pedido.getSedeDevolucionNombre() + ").");
        return pedidoDAO.update(pedido.getId(), pedido).orElseThrow();
    }

@Override
public PedidoInterciudadDTO registrarDevolucionEnSede(Long pedidoId, Long bibliotecarioId, Long sedeId) {
    PedidoInterciudadDTO pedido = buscarPedido(pedidoId);
    validarEstado(pedido, ESTADO_ENTREGADO);

    if (!pedido.getSedeDevolucionId().equals(sedeId)) {
        throw new BusinessValidationException("El pedido debe devolverse en la sede de devolución elegida: " + pedido.getSedeDevolucionNombre());
    }

    LocalDateTime ahora = LocalDateTime.now();

    long diasRetraso = automatizacionService.evaluarRetrasoAlDevolver(pedido.getPrestamoId());

    // El libro queda físicamente en la sede de devolución, esperando traslado
    cambiarEstadoEjemplar(pedido.getEjemplarId(), EJEMPLAR_PRESTADO, EJEMPLAR_EN_TRANSITO);

    pedido.setEstado(ESTADO_DEVUELTO_PENDIENTE_TRASLADO);
    pedido.setBibliotecarioId(bibliotecarioId);

    PrestamoDTO prestamo = prestamoDAO.findById(pedido.getPrestamoId()).orElseThrow();
    prestamo.setEstado("DEVUELTO");
    prestamo.setFechaDevolucion(ahora);
    prestamoDAO.update(prestamo.getId(), prestamo);

    String avisoRetraso = diasRetraso > 0
            ? " La devolución tuvo " + diasRetraso + (diasRetraso == 1 ? " día" : " días")
                    + " de retraso: revisa tus multas pendientes."
            : "";
    notificar(pedido.getUsuarioId(), "LIBRO_DEVUELTO", "Devolución registrada",
            "Se registró la devolución exitosa del pedido #" + pedidoId + " en la sede elegida." + avisoRetraso);

    notificacionService.notificarSede(pedido.getSedeOrigenId(), "LIBRO_DEVUELTO_REGRESA",
            "Libro devuelto, pronto volverá a tu sede",
            "El libro del pedido #" + pedidoId + " fue devuelto en la sede " + pedido.getSedeDevolucionNombre()
                    + " y pronto volverá a tu sede.");

    return pedidoDAO.update(pedido.getId(), pedido).orElseThrow();
}

    @Override
    public PedidoInterciudadDTO registrarRecepcionNoRecibido(Long pedidoId, Long bibliotecarioId) { 
        PedidoInterciudadDTO pedido = buscarPedido(pedidoId);
        validarEstado(pedido, ESTADO_NO_RECIBIDO);

        LocalDateTime ahora = LocalDateTime.now();
        pedido.setEstado(ESTADO_EN_ESPERA_DE_RECOGIDA);
        pedido.setFechaLimiteRecogida(ahora.plusDays(configuracionService.plazo(ClavePlazo.DIAS_RECOGIDA_SEDE_DEVOLUCION)));
        pedido.setBibliotecarioId(bibliotecarioId);

        notificar(pedido.getUsuarioId(), "EN_ESPERA_DE_RECOGIDA", "Tu libro está en la sede de devolución",
                "Puedes recoger tu pedido #" + pedidoId + " en la sede de devolución antes de: " + pedido.getFechaLimiteRecogida());
        notificacionService.notificarSede(pedido.getSedeOrigenId(), "LIBRO_DEVUELTO_REGRESA",
        "Libro devuelto, pronto volverá a tu sede",
        "El libro del pedido #" + pedidoId + " fue devuelto en la sede " + pedido.getSedeDevolucionNombre()
                + " y pronto volverá a tu sede.");
        return pedidoDAO.update(pedido.getId(), pedido).orElseThrow();
    }

    @Override
    public PedidoInterciudadDTO registrarEntregaEnSede(Long pedidoId, Long bibliotecarioId) { 
        PedidoInterciudadDTO pedido = buscarPedido(pedidoId);
        validarEstado(pedido, ESTADO_EN_ESPERA_DE_RECOGIDA);

        LocalDateTime ahora = LocalDateTime.now();
        int diasPrestamo = configuracionService.plazo(ClavePlazo.DIAS_PRESTAMO_INTERCIUDAD);
        cambiarEstadoEjemplar(pedido.getEjemplarId(), EJEMPLAR_EN_TRANSITO, EJEMPLAR_PRESTADO);

        PrestamoDTO prestamo = prestamoDAO.findById(pedido.getPrestamoId()).orElseThrow();
        prestamo.setEstado("ACTIVO");
        prestamo.setFechaEntrega(ahora);
        prestamo.setFechaVencimiento(ahora.toLocalDate().plusDays(diasPrestamo));
        prestamoDAO.update(prestamo.getId(), prestamo);

        pedido.setEstado(ESTADO_ENTREGADO);
        pedido.setFechaEntrega(ahora);
        pedido.setFechaLimiteDevolucion(ahora.plusDays(diasPrestamo));
        pedido.setBibliotecarioId(bibliotecarioId);

        return pedidoDAO.update(pedido.getId(), pedido).orElseThrow();
    }

    @Override
    public PedidoInterciudadDTO registrarRecepcionEnSedeOrigen(Long pedidoId, Long bibliotecarioId) {
        PedidoInterciudadDTO pedido = buscarPedido(pedidoId);

        if (!ESTADO_EN_CAMINO_SEDE_ORIGEN.equalsIgnoreCase(pedido.getEstado())) {
            throw new BusinessValidationException("El pedido #" + pedidoId
                    + " no puede recibirse en la sede de origen (estado actual: " + pedido.getEstado()
                    + "). Debe estar EN_CAMINO_SEDE_ORIGEN");
        }

        cambiarEstadoEjemplar(pedido.getEjemplarId(), EJEMPLAR_EN_TRANSITO, EJEMPLAR_DISPONIBLE);

        pedido.setEstado(ESTADO_CERRADO);
        pedido.setBibliotecarioId(bibliotecarioId);
        PedidoInterciudadDTO cerrado = pedidoDAO.update(pedido.getId(), pedido).orElseThrow();

        reservaLibroService.asignarEjemplarAReservaEnEspera(pedido.getEjemplarId());
        return cerrado;
    }

    // ------------------------------------------------------------ Acciones Repartidor

    @Override
    @Transactional(readOnly = true)
    public List<PedidoInterciudadDTO> obtenerAsignacionesRepartidor(Long repartidorId) {
        if (!usuarioDAO.existsById(repartidorId)) {
            throw new ResourceNotFoundException("Repartidor no encontrado con ID: " + repartidorId);
        }
        // Asignados explícitamente al repartidor (EN_TRANSITO, EN_CAMINO_SEDE_ORIGEN)
        List<PedidoInterciudadDTO> asignados = pedidoDAO.findByRepartidorIdAndEstados(
                repartidorId, List.of(ESTADO_EN_TRANSITO, ESTADO_EN_CAMINO_SEDE_ORIGEN, ESTADO_DEVUELTO_PENDIENTE_TRASLADO));
        
        // Traslados pendientes de retorno en cualquier sede que aún no tengan o tengan repartidor asignado
        List<PedidoInterciudadDTO> pendientesTraslado = pedidoDAO.findPendientesDeTraslado();
        
        // Combinar sin duplicados
        java.util.Map<Long, PedidoInterciudadDTO> mapa = new java.util.LinkedHashMap<>();
        for (PedidoInterciudadDTO p : asignados) {
            mapa.put(p.getId(), p);
        }
        for (PedidoInterciudadDTO p : pendientesTraslado) {
            mapa.putIfAbsent(p.getId(), p);
        }
    
        return new java.util.ArrayList<>(mapa.values());
    }

    @Override
    public PedidoInterciudadDTO confirmarEntregaDomicilio(Long pedidoId, Long repartidorId) { 
        PedidoInterciudadDTO pedido = buscarPedido(pedidoId);
        validarEstado(pedido, ESTADO_EN_TRANSITO);

        LocalDateTime ahora = LocalDateTime.now();
        int diasPrestamo = configuracionService.plazo(ClavePlazo.DIAS_PRESTAMO_INTERCIUDAD);
        cambiarEstadoEjemplar(pedido.getEjemplarId(), EJEMPLAR_EN_TRANSITO, EJEMPLAR_PRESTADO);

        PrestamoDTO prestamo = prestamoDAO.findById(pedido.getPrestamoId()).orElseThrow();
        prestamo.setEstado("ACTIVO");
        prestamo.setFechaEntrega(ahora);
        prestamo.setFechaVencimiento(ahora.toLocalDate().plusDays(diasPrestamo));
        prestamoDAO.update(prestamo.getId(), prestamo);

        pedido.setEstado(ESTADO_ENTREGADO);
        pedido.setFechaEntrega(ahora);
        pedido.setFechaLimiteDevolucion(ahora.plusDays(diasPrestamo));
        pedido.setRepartidorId(repartidorId);

        notificar(pedido.getUsuarioId(), "ENTREGA_CONFIRMADA", "Pedido entregado con éxito",
                "Tu pedido #" + pedidoId + " ha sido entregado a domicilio. Debes devolverlo en la sede: " + pedido.getSedeDevolucionNombre());
        return pedidoDAO.update(pedido.getId(), pedido).orElseThrow();
    }

    @Override
    public PedidoInterciudadDTO registrarUsuarioNoRecibio(Long pedidoId, Long repartidorId) { 
        PedidoInterciudadDTO pedido = buscarPedido(pedidoId);
        validarEstado(pedido, ESTADO_EN_TRANSITO);

        pedido.setEstado(ESTADO_NO_RECIBIDO);
        pedido.setRepartidorId(repartidorId);

        // Generar recargo por intento de entrega fallido
        MultaDTO recargo = new MultaDTO();
        BigDecimal montoRecargo = configuracionService.montoMulta(ConceptoMulta.RECARGO_PEDIDO_NO_RECIBIDO);
        recargo.setMonto(montoRecargo);
        recargo.setMotivo("Recargo por no recibir pedido interciudad en la fecha y hora indicadas (Pedido #" + pedidoId + ")");
        recargo.setFechaGeneracion(LocalDateTime.now());
        recargo.setEstado("PENDIENTE");
        recargo.setUsuarioId(pedido.getUsuarioId());
        recargo.setPrestamoId(pedido.getPrestamoId());
        multaDAO.save(recargo);

        notificar(pedido.getUsuarioId(), "PEDIDO_NO_RECIBIDO", "Intento de entrega no completado",
                "No fue posible entregar el pedido #" + pedidoId + ". El paquete se trasladará a la sede de devolución ("
                        + pedido.getSedeDevolucionNombre() + "). Se aplicó un recargo de $" + montoRecargo);

        return pedidoDAO.update(pedido.getId(), pedido).orElseThrow();
    }

    @Override
    public PedidoInterciudadDTO iniciarTrasladoRetorno(Long pedidoId, Long repartidorId) {
        PedidoInterciudadDTO pedido = buscarPedido(pedidoId);
        validarEstado(pedido, ESTADO_DEVUELTO_PENDIENTE_TRASLADO);

        pedido.setEstado(ESTADO_EN_CAMINO_SEDE_ORIGEN);
        pedido.setRepartidorId(repartidorId);

        notificacionService.notificarSede(pedido.getSedeOrigenId(), "TRASLADO_EN_CAMINO",
        "Libro en camino a tu sede",
        "El libro del pedido #" + pedido.getId() + " va en camino desde la sede "
                + pedido.getSedeDevolucionNombre() + ".");

        return pedidoDAO.update(pedido.getId(), pedido).orElseThrow();
    }

    // ------------------------------------------------------------ Métodos Auxiliares


    private EjemplarDTO reservarEjemplarConTarifa(Long libroId, String ciudadEntrega) {
        List<EjemplarDTO> candidatos = ejemplarDAO.findDisponiblesEnOtraCiudad(libroId, ciudadEntrega);
        if (candidatos.isEmpty()) {
            throw new BusinessValidationException("No hay ejemplares disponibles en ciudades distintas a " + ciudadEntrega);
        }
        for (EjemplarDTO candidato : candidatos) {
            String ciudadOrigen = obtenerCiudadDeSede(candidato.getSedeId());
            if (tarifaEnvioService.buscarCosto(ciudadOrigen, ciudadEntrega).isEmpty()) {
                continue;
            }
            if (ejemplarDAO.cambiarEstadoSiEs(candidato.getId(), EJEMPLAR_DISPONIBLE, EJEMPLAR_RESERVADO) == 1) {
                return candidato;
            }
        }
        throw new BusinessValidationException(
                "No hay ejemplares disponibles con tarifa de envío definida hacia " + ciudadEntrega);
    }

    private String obtenerCiudadDeSede(Long sedeId) {
        return sedeDAO.findById(sedeId)
                .map(SedeDTO::getCiudad)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada: " + sedeId));
    }

    private void cambiarEstadoEjemplar(Long ejemplarId, String esperado, String nuevo) {
        if (ejemplarDAO.cambiarEstadoSiEs(ejemplarId, esperado, nuevo) == 0) {
            throw new BusinessValidationException("El ejemplar " + ejemplarId + " no estaba en estado " + esperado);
        }
    }

    private PedidoInterciudadDTO buscarPedido(Long id) {
        return pedidoDAO.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pedido interciudad no encontrado con ID: " + id));
    }

    private void validarEstado(PedidoInterciudadDTO pedido, String estadoEsperado) {
        if (!estadoEsperado.equalsIgnoreCase(pedido.getEstado())) {
            throw new BusinessValidationException("El pedido #" + pedido.getId() + " no está en estado " + estadoEsperado + " (Estado actual: " + pedido.getEstado() + ")");
        }
    }

    /** Marca como REEMBOLSADO el pago de envío aprobado del pedido. Vacío si el pedido no se había pagado. */
    private Optional<PagoDTO> reembolsarEnvio(PedidoInterciudadDTO pedido) {
        Optional<PagoDTO> pagoOpt = pagoDAO.findEnvioPagadoPorPedido(pedido.getId());
        pagoOpt.ifPresent(pago -> {
            pago.setEstado(EstadoPago.REEMBOLSADO);
            pagoDAO.update(pago.getId(), pago);
            log.info("Reembolso del envío del pedido {}: pago {} por {}", pedido.getId(), pago.getReferencia(), pago.getMonto());
        });
        return pagoOpt;
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