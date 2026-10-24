package bussinesLayer.service.impl;

import bussinesLayer.dto.*;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.PedidoInterciudadService;
import bussinesLayer.service.SuscripcionService;
import bussinesLayer.service.TarifaEnvioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private static final String EJEMPLAR_DISPONIBLE = "DISPONIBLE";
    private static final String EJEMPLAR_RESERVADO = "RESERVADO";
    private static final String EJEMPLAR_EN_TRANSITO = "EN_TRANSITO";
    private static final String EJEMPLAR_PRESTADO = "PRESTADO";

    private static final int DIAS_MAXIMO_SOLICITUD = 7;
    private static final int DIAS_PLAZO_RECOGIDA = 2; 
    private static final int DIAS_PRESTAMO_INTERCIUDAD = 14; 
    private static final BigDecimal RECARGO_NO_RECIBIDO = new BigDecimal("15000.00"); 

    private final PedidoInterciudadDAO pedidoDAO;
    private final UsuarioDAO usuarioDAO;
    private final LibroDAO libroDAO;
    private final EjemplarDAO ejemplarDAO;
    private final SedeDAO sedeDAO;
    private final PrestamoDAO prestamoDAO;
    private final SancionDAO sancionDAO;
    private final MultaDAO multaDAO;
    private final NotificacionDAO notificacionDAO;
    private final SuscripcionService suscripcionService;
    private final TarifaEnvioService tarifaEnvioService;

    // ------------------------------------------------------------Solicitar

    @Override
    public PedidoInterciudadDTO solicitarPedido(SolicitudPedidoInterciudadDTO solicitud) {
        log.info("Solicitud pedido interciudad para usuario {} y libro {}", solicitud.getUsuarioId(), solicitud.getLibroId());

        if (!usuarioDAO.existsById(solicitud.getUsuarioId())) {
            throw new ResourceNotFoundException("Usuario no encontrado: " + solicitud.getUsuarioId());
        }
        suscripcionService.exigirSuscripcionVigente(solicitud.getUsuarioId()); 

        if (!sedeDAO.existsById(solicitud.getSedeDevolucionId())) {
            throw new ResourceNotFoundException("Sede de devolución no encontrada: " + solicitud.getSedeDevolucionId());
        }

        LocalDate hoy = LocalDate.now();
        if (solicitud.getFechaRecepcion().isAfter(hoy.plusDays(DIAS_MAXIMO_SOLICITUD))) {
            throw new BusinessValidationException("La fecha de recepción no puede superar los 7 días a partir de hoy");
        }

        if (sancionDAO.existsByUsuarioIdAndTipoIgnoreCaseAndEstadoIgnoreCase(solicitud.getUsuarioId(), "BLOQUEO_PRESTAMOS", "ACTIVA")) {
            throw new BusinessValidationException("El usuario posee sanciones activas que impiden realizar pedidos");
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
        pedido.setEstado(ESTADO_SOLICITADO);
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

        return pedidoDAO.save(pedido);
    }

    // ------------------------------------------------------------ Cancelar

    @Override
    public PedidoInterciudadDTO cancelarPedido(Long pedidoId, Long usuarioId) {
        PedidoInterciudadDTO pedido = buscarPedido(pedidoId);
        if (!pedido.getUsuarioId().equals(usuarioId)) {
            throw new BusinessValidationException("El pedido no pertenece al usuario especificado");
        }
        if (!ESTADO_SOLICITADO.equalsIgnoreCase(pedido.getEstado())) {
            notificar(usuarioId, "CANCELACION_BLOQUEADA", "No se pudo cancelar el pedido",
                    "El pedido #" + pedidoId + " ya no está en estado SOLICITADO y no se puede cancelar.");
            throw new BusinessValidationException("Solo se pueden cancelar pedidos en estado SOLICITADO");
        }

        cambiarEstadoEjemplar(pedido.getEjemplarId(), EJEMPLAR_RESERVADO, EJEMPLAR_DISPONIBLE);
        PrestamoDTO prestamo = prestamoDAO.findById(pedido.getPrestamoId()).orElseThrow();
        prestamo.setEstado("CANCELADO");
        prestamoDAO.update(prestamo.getId(), prestamo);

        pedido.setEstado(ESTADO_CANCELADO);
        pedido.setFechaCancelacion(LocalDateTime.now());
        return pedidoDAO.update(pedido.getId(), pedido).orElseThrow();
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

    // ------------------------------------------------------------ Acciones Bibliotecario

    @Override
    public PedidoInterciudadDTO despacharPedido(Long pedidoId, Long bibliotecarioId, Long repartidorId) {
        PedidoInterciudadDTO pedido = buscarPedido(pedidoId);
        validarEstado(pedido, ESTADO_SOLICITADO);

        cambiarEstadoEjemplar(pedido.getEjemplarId(), EJEMPLAR_RESERVADO, EJEMPLAR_EN_TRANSITO);
        pedido.setEstado(ESTADO_EN_TRANSITO);
        pedido.setFechaTransito(LocalDateTime.now());
        pedido.setBibliotecarioId(bibliotecarioId);
        pedido.setRepartidorId(repartidorId);

        notificar(pedido.getUsuarioId(), "PEDIDO_EN_TRANSITO", "Tu pedido va en camino",
                "El pedido #" + pedidoId + " se encuentra en tránsito a la dirección indicada.");
        return pedidoDAO.update(pedido.getId(), pedido).orElseThrow();
    }

    @Override
    public PedidoInterciudadDTO registrarDevolucionEnSede(Long pedidoId, Long bibliotecarioId, Long sedeId) { 
        PedidoInterciudadDTO pedido = buscarPedido(pedidoId);
        if (!pedido.getSedeDevolucionId().equals(sedeId)) {
            throw new BusinessValidationException("El pedido debe devolverse en la sede de devolución elegida: " + pedido.getSedeDevolucionNombre());
        }

        pedido.setEstado(ESTADO_DEVUELTO_PENDIENTE_TRASLADO);
        pedido.setBibliotecarioId(bibliotecarioId);

        PrestamoDTO prestamo = prestamoDAO.findById(pedido.getPrestamoId()).orElseThrow();
        prestamo.setEstado("DEVUELTO");
        prestamoDAO.update(prestamo.getId(), prestamo);

        notificar(pedido.getUsuarioId(), "LIBRO_DEVUELTO", "Devolución registrada",
                "Se registró la devolución exitosa del pedido #" + pedidoId + " en la sede elegida.");
        return pedidoDAO.update(pedido.getId(), pedido).orElseThrow();
    }

    @Override
    public PedidoInterciudadDTO registrarRecepcionNoRecibido(Long pedidoId, Long bibliotecarioId) { 
        PedidoInterciudadDTO pedido = buscarPedido(pedidoId);
        validarEstado(pedido, ESTADO_NO_RECIBIDO);

        LocalDateTime ahora = LocalDateTime.now();
        pedido.setEstado(ESTADO_EN_ESPERA_DE_RECOGIDA);
        pedido.setFechaLimiteRecogida(ahora.plusDays(DIAS_PLAZO_RECOGIDA));
        pedido.setBibliotecarioId(bibliotecarioId);

        notificar(pedido.getUsuarioId(), "EN_ESPERA_DE_RECOGIDA", "Tu libro está en la sede de devolución",
                "Puedes recoger tu pedido #" + pedidoId + " en la sede de devolución antes de: " + pedido.getFechaLimiteRecogida());
        return pedidoDAO.update(pedido.getId(), pedido).orElseThrow();
    }

    @Override
    public PedidoInterciudadDTO registrarEntregaEnSede(Long pedidoId, Long bibliotecarioId) { 
        PedidoInterciudadDTO pedido = buscarPedido(pedidoId);
        validarEstado(pedido, ESTADO_EN_ESPERA_DE_RECOGIDA);

        LocalDateTime ahora = LocalDateTime.now();
        cambiarEstadoEjemplar(pedido.getEjemplarId(), EJEMPLAR_EN_TRANSITO, EJEMPLAR_PRESTADO);

        PrestamoDTO prestamo = prestamoDAO.findById(pedido.getPrestamoId()).orElseThrow();
        prestamo.setEstado("ACTIVO");
        prestamo.setFechaEntrega(ahora);
        prestamo.setFechaVencimiento(LocalDate.now().plusDays(DIAS_PRESTAMO_INTERCIUDAD));
        prestamoDAO.update(prestamo.getId(), prestamo);

        pedido.setEstado(ESTADO_ENTREGADO);
        pedido.setFechaEntrega(ahora);
        pedido.setFechaLimiteDevolucion(ahora.plusDays(DIAS_PRESTAMO_INTERCIUDAD));
        pedido.setBibliotecarioId(bibliotecarioId);

        return pedidoDAO.update(pedido.getId(), pedido).orElseThrow();
    }

    @Override
    public PedidoInterciudadDTO registrarRecepcionEnSedeOrigen(Long pedidoId, Long bibliotecarioId) { 
        PedidoInterciudadDTO pedido = buscarPedido(pedidoId);
        cambiarEstadoEjemplar(pedido.getEjemplarId(), EJEMPLAR_EN_TRANSITO, EJEMPLAR_DISPONIBLE);

        pedido.setEstado(ESTADO_CERRADO);
        pedido.setBibliotecarioId(bibliotecarioId);
        return pedidoDAO.update(pedido.getId(), pedido).orElseThrow();
    }

    // ------------------------------------------------------------ Acciones Repartidor

    @Override
    @Transactional(readOnly = true)
    public List<PedidoInterciudadDTO> obtenerAsignacionesRepartidor(Long repartidorId) { 
        return pedidoDAO.findByRepartidorIdAndEstados(repartidorId, List.of(ESTADO_EN_TRANSITO, ESTADO_DEVUELTO_PENDIENTE_TRASLADO, ESTADO_EN_CAMINO_SEDE_ORIGEN));
    }

    @Override
    public PedidoInterciudadDTO confirmarEntregaDomicilio(Long pedidoId, Long repartidorId) { 
        PedidoInterciudadDTO pedido = buscarPedido(pedidoId);
        validarEstado(pedido, ESTADO_EN_TRANSITO);

        LocalDateTime ahora = LocalDateTime.now();
        cambiarEstadoEjemplar(pedido.getEjemplarId(), EJEMPLAR_EN_TRANSITO, EJEMPLAR_PRESTADO);

        PrestamoDTO prestamo = prestamoDAO.findById(pedido.getPrestamoId()).orElseThrow();
        prestamo.setEstado("ACTIVO");
        prestamo.setFechaEntrega(ahora);
        prestamo.setFechaVencimiento(LocalDate.now().plusDays(DIAS_PRESTAMO_INTERCIUDAD));
        prestamoDAO.update(prestamo.getId(), prestamo);

        pedido.setEstado(ESTADO_ENTREGADO);
        pedido.setFechaEntrega(ahora);
        pedido.setFechaLimiteDevolucion(ahora.plusDays(DIAS_PRESTAMO_INTERCIUDAD));
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
        recargo.setMonto(RECARGO_NO_RECIBIDO);
        recargo.setMotivo("Recargo por no recibir pedido interciudad en la fecha y hora indicadas (Pedido #" + pedidoId + ")");
        recargo.setFechaGeneracion(LocalDateTime.now());
        recargo.setEstado("PENDIENTE");
        recargo.setUsuarioId(pedido.getUsuarioId());
        recargo.setPrestamoId(pedido.getPrestamoId());
        multaDAO.save(recargo);

        notificar(pedido.getUsuarioId(), "PEDIDO_NO_RECIBIDO", "Intento de entrega no completado",
                "No fue posible entregar el pedido #" + pedidoId + ". El paquete se trasladará a la sede de devolución ("
                        + pedido.getSedeDevolucionNombre() + "). Se aplicó un recargo de $" + RECARGO_NO_RECIBIDO);

        return pedidoDAO.update(pedido.getId(), pedido).orElseThrow();
    }

    @Override
    public PedidoInterciudadDTO iniciarTrasladoRetorno(Long pedidoId, Long repartidorId) {
        PedidoInterciudadDTO pedido = buscarPedido(pedidoId);
        validarEstado(pedido, ESTADO_DEVUELTO_PENDIENTE_TRASLADO);

        pedido.setEstado(ESTADO_EN_CAMINO_SEDE_ORIGEN);
        pedido.setRepartidorId(repartidorId);

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