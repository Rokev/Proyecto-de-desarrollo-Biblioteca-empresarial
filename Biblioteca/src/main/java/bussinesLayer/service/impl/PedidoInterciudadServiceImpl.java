package bussinesLayer.service.impl;

import bussinesLayer.dto.EjemplarDTO;
import bussinesLayer.dto.NotificacionDTO;
import bussinesLayer.dto.PedidoInterciudadDTO;
import bussinesLayer.dto.PrestamoDTO;
import bussinesLayer.dto.SolicitudPedidoInterciudadDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.PedidoInterciudadService;
import bussinesLayer.service.SuscripcionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.EjemplarDAO;
import persistanceLayer.dao.LibroDAO;
import persistanceLayer.dao.NotificacionDAO;
import persistanceLayer.dao.PedidoInterciudadDAO;
import persistanceLayer.dao.PrestamoDAO;
import persistanceLayer.dao.SancionDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PedidoInterciudadServiceImpl implements PedidoInterciudadService {

    private static final String PEDIDO_SOLICITADO = "SOLICITADO";
    private static final String PEDIDO_EN_TRANSITO = "EN_TRANSITO";
    private static final String PEDIDO_DISPONIBLE = "DISPONIBLE_PARA_RETIRO";
    private static final String PEDIDO_RETIRADO = "RETIRADO";
    private static final String PEDIDO_CANCELADO = "CANCELADO";
    private static final List<String> ESTADOS_PEDIDO =
            List.of(PEDIDO_SOLICITADO, PEDIDO_EN_TRANSITO, PEDIDO_DISPONIBLE, PEDIDO_RETIRADO, PEDIDO_CANCELADO);

    private static final String EJEMPLAR_DISPONIBLE = "DISPONIBLE";
    private static final String EJEMPLAR_RESERVADO = "RESERVADO";
    private static final String EJEMPLAR_EN_TRANSITO = "EN_TRANSITO";
    private static final String EJEMPLAR_PRESTADO = "PRESTADO";

    private static final String PRESTAMO_SOLICITADO = "SOLICITADO";
    private static final String PRESTAMO_ACTIVO = "ACTIVO";
    private static final String PRESTAMO_CANCELADO = "CANCELADO";
    private static final String MODALIDAD_INTERCIUDAD = "INTERCIUDAD";

    /** Mismo plazo que PrestamoServiceImpl. El plazo extendido para suscriptores (RF-48) queda pendiente. */
    private static final int DIAS_PRESTAMO = 14;

    private final PedidoInterciudadDAO pedidoDAO;
    private final UsuarioDAO usuarioDAO;
    private final LibroDAO libroDAO;
    private final EjemplarDAO ejemplarDAO;
    private final PrestamoDAO prestamoDAO;
    private final SancionDAO sancionDAO;
    private final NotificacionDAO notificacionDAO;
    private final SuscripcionService suscripcionService;

    // ------------------------------------------------------------ RF-26 solicitar

    @Override
    public PedidoInterciudadDTO solicitarPedido(SolicitudPedidoInterciudadDTO solicitud) {
        log.info("Pedido interciudad: usuario {}, libro {}, ciudad de entrega {}",
                solicitud.getUsuarioId(), solicitud.getLibroId(), solicitud.getCiudadEntrega());

        if (!usuarioDAO.existsById(solicitud.getUsuarioId())) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + solicitud.getUsuarioId());
        }
        // RF-26 / RF-35: solo con suscripcion vigente.
        suscripcionService.exigirSuscripcionVigente(solicitud.getUsuarioId());

        if (!libroDAO.existsById(solicitud.getLibroId())) {
            throw new ResourceNotFoundException("Libro no encontrado con ID: " + solicitud.getLibroId());
        }
        if (sancionDAO.existsByUsuarioIdAndTipoIgnoreCaseAndEstadoIgnoreCase(
                solicitud.getUsuarioId(), "BLOQUEO_PRESTAMOS", "ACTIVA")) {
            throw new BusinessValidationException("El usuario tiene un bloqueo de prestamos activo");
        }

        String ciudad = solicitud.getCiudadEntrega().trim();
        String direccion = solicitud.getDireccionEntrega().trim();
        LocalDateTime ahora = LocalDateTime.now();

        // 1) Reservar un ejemplar de otra ciudad (cambio atomico: nadie mas puede tomar el mismo).
        EjemplarDTO ejemplar = reservarEjemplarDeOtraCiudad(solicitud.getLibroId(), ciudad);

        // 2) Prestamo vinculado. fechaVencimiento queda nula hasta registrar la entrega (RF-07).
        PrestamoDTO prestamo = new PrestamoDTO();
        prestamo.setFechaSolicitud(ahora);
        prestamo.setEstado(PRESTAMO_SOLICITADO);
        prestamo.setModalidad(MODALIDAD_INTERCIUDAD);
        prestamo.setDireccionEntrega(direccion);
        prestamo.setVecesRenovado(0);
        prestamo.setUsuarioId(solicitud.getUsuarioId());
        prestamo.setEjemplarId(ejemplar.getId());
        prestamo.setSedeRetiroId(ejemplar.getSedeId());
        PrestamoDTO prestamoGuardado = prestamoDAO.save(prestamo);

        // 3) Pedido.
        PedidoInterciudadDTO pedido = new PedidoInterciudadDTO();
        pedido.setEstado(PEDIDO_SOLICITADO);
        pedido.setCiudadEntrega(ciudad);
        pedido.setDireccionEntrega(direccion);
        pedido.setFechaSolicitud(ahora);
        pedido.setUsuarioId(solicitud.getUsuarioId());
        pedido.setEjemplarId(ejemplar.getId());
        pedido.setSedeOrigenId(ejemplar.getSedeId());
        pedido.setPrestamoId(prestamoGuardado.getId());

        PedidoInterciudadDTO guardado = pedidoDAO.save(pedido);
        log.info("Pedido interciudad creado con ID: {}", guardado.getId());
        return guardado;
    }

    // ------------------------------------------------------------consultar

    @Override
    @Transactional(readOnly = true)
    public PedidoInterciudadDTO obtenerPedidoPorId(Long id) {
        return buscarPedido(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoInterciudadDTO> obtenerPedidosDeUsuario(Long usuarioId, String estado) {
        if (!usuarioDAO.existsById(usuarioId)) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId);
        }
        if (estado == null || estado.isBlank()) {
            return pedidoDAO.findByUsuarioIdOrderByFechaSolicitudDesc(usuarioId);
        }
        return pedidoDAO.findByUsuarioIdAndEstadoIgnoreCaseOrderByFechaSolicitudDesc(usuarioId, normalizarEstado(estado));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoInterciudadDTO> obtenerPedidosPorEstado(String estado) {
        return pedidoDAO.findByEstadoIgnoreCaseOrderByFechaSolicitudAsc(normalizarEstado(estado));
    }

    // ------------------------------------------------------------  cancelar

    /**
     * noRollbackFor: si el pedido esta en transito se guarda la notificacion al usuario y despues
     * se lanza la excepcion; sin esto, el rollback deshace tambien la notificacion.
     */
    @Override
    @Transactional(noRollbackFor = BusinessValidationException.class)
    public PedidoInterciudadDTO cancelarPedido(Long pedidoId, Long usuarioId) {
        PedidoInterciudadDTO pedido = buscarPedido(pedidoId);

        if (!pedido.getUsuarioId().equals(usuarioId)) {
            throw new BusinessValidationException("El pedido no pertenece al usuario " + usuarioId);
        }
        if (PEDIDO_EN_TRANSITO.equalsIgnoreCase(pedido.getEstado())) {
            notificar(usuarioId, "PEDIDO_CANCELACION_BLOQUEADA", "No se puede cancelar tu pedido",
                    "Tu pedido #" + pedidoId + " ya esta en transito y no puede cancelarse.");
            throw new BusinessValidationException("El pedido esta en transito y no puede cancelarse");
        }
        if (!PEDIDO_SOLICITADO.equalsIgnoreCase(pedido.getEstado())) {
            throw new BusinessValidationException("Solo se puede cancelar un pedido en estado "
                    + PEDIDO_SOLICITADO + " (estado actual: " + pedido.getEstado() + ")");
        }

        // El ejemplar vuelve a estar disponible y el prestamo vinculado se cancela.
        cambiarEstadoEjemplar(pedido.getEjemplarId(), EJEMPLAR_RESERVADO, EJEMPLAR_DISPONIBLE);
        PrestamoDTO prestamo = buscarPrestamo(pedido.getPrestamoId());
        prestamo.setEstado(PRESTAMO_CANCELADO);
        prestamoDAO.update(prestamo.getId(), prestamo);

        pedido.setEstado(PEDIDO_CANCELADO);
        pedido.setFechaCancelacion(LocalDateTime.now());
        PedidoInterciudadDTO cancelado = guardar(pedido);
        log.info("Pedido interciudad cancelado ID: {}", pedidoId);
        return cancelado;
    }

    // ------------------------------------------- transiciones del bibliotecario

    @Override
    public PedidoInterciudadDTO marcarEnTransito(Long pedidoId, Long bibliotecarioId) {
        verificarBibliotecario(bibliotecarioId);
        PedidoInterciudadDTO pedido = buscarPedido(pedidoId);
        exigirEstado(pedido, PEDIDO_SOLICITADO, PEDIDO_EN_TRANSITO);

        cambiarEstadoEjemplar(pedido.getEjemplarId(), EJEMPLAR_RESERVADO, EJEMPLAR_EN_TRANSITO);

        pedido.setEstado(PEDIDO_EN_TRANSITO);
        pedido.setFechaTransito(LocalDateTime.now());
        pedido.setBibliotecarioId(bibliotecarioId);
        PedidoInterciudadDTO actualizado = guardar(pedido);

        notificar(pedido.getUsuarioId(), "PEDIDO_EN_TRANSITO", "Tu pedido va en camino",
                "Tu pedido #" + pedidoId + " ya esta en transito hacia " + pedido.getCiudadEntrega() + ".");
        return actualizado;
    }

    @Override
    public PedidoInterciudadDTO marcarDisponibleParaRetiro(Long pedidoId, Long bibliotecarioId) {
        verificarBibliotecario(bibliotecarioId);
        PedidoInterciudadDTO pedido = buscarPedido(pedidoId);
        exigirEstado(pedido, PEDIDO_EN_TRANSITO, PEDIDO_DISPONIBLE);

        // El ejemplar sigue EN_TRANSITO hasta que se registra la entrega.
        pedido.setEstado(PEDIDO_DISPONIBLE);
        pedido.setFechaDisponible(LocalDateTime.now());
        pedido.setBibliotecarioId(bibliotecarioId);
        PedidoInterciudadDTO actualizado = guardar(pedido);

        notificar(pedido.getUsuarioId(), "PEDIDO_DISPONIBLE", "Tu pedido esta disponible",
                "Tu pedido #" + pedidoId + " llego a " + pedido.getCiudadEntrega()
                        + " y esta disponible para su entrega.");
        return actualizado;
    }

    @Override
    public PedidoInterciudadDTO marcarRetirado(Long pedidoId, Long bibliotecarioId) {
        verificarBibliotecario(bibliotecarioId);
        PedidoInterciudadDTO pedido = buscarPedido(pedidoId);
        exigirEstado(pedido, PEDIDO_DISPONIBLE, PEDIDO_RETIRADO);

        cambiarEstadoEjemplar(pedido.getEjemplarId(), EJEMPLAR_EN_TRANSITO, EJEMPLAR_PRESTADO);

        // RF-07: el plazo de devolucion empieza al registrar la entrega.
        LocalDateTime ahora = LocalDateTime.now();
        PrestamoDTO prestamo = buscarPrestamo(pedido.getPrestamoId());
        prestamo.setEstado(PRESTAMO_ACTIVO);
        prestamo.setFechaEntrega(ahora);
        prestamo.setFechaVencimiento(LocalDate.now().plusDays(DIAS_PRESTAMO));
        prestamoDAO.update(prestamo.getId(), prestamo);

        pedido.setEstado(PEDIDO_RETIRADO);
        pedido.setFechaRetiro(ahora);
        pedido.setBibliotecarioId(bibliotecarioId);
        PedidoInterciudadDTO actualizado = guardar(pedido);

        notificar(pedido.getUsuarioId(), "PEDIDO_ENTREGADO", "Tu libro fue entregado",
                "Registramos la entrega de tu pedido #" + pedidoId + ". Fecha limite de devolucion: "
                        + prestamo.getFechaVencimiento() + ".");
        return actualizado;
    }

    // -------------------------------------------------------------- privados

    private EjemplarDTO reservarEjemplarDeOtraCiudad(Long libroId, String ciudadEntrega) {
        for (EjemplarDTO candidato : ejemplarDAO.findDisponiblesEnOtraCiudad(libroId, ciudadEntrega)) {
            if (ejemplarDAO.cambiarEstadoSiEs(candidato.getId(), EJEMPLAR_DISPONIBLE, EJEMPLAR_RESERVADO) == 1) {
                return candidato;
            }
        }
        throw new BusinessValidationException(
                "No hay ejemplares disponibles de este libro en ciudades distintas a " + ciudadEntrega);
    }

    private void cambiarEstadoEjemplar(Long ejemplarId, String esperado, String nuevo) {
        if (ejemplarDAO.cambiarEstadoSiEs(ejemplarId, esperado, nuevo) == 0) {
            throw new BusinessValidationException(
                    "El ejemplar del pedido no esta en el estado esperado (" + esperado + ")");
        }
    }

    private PedidoInterciudadDTO buscarPedido(Long id) {
        return pedidoDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido interciudad no encontrado con ID: " + id));
    }

    private PrestamoDTO buscarPrestamo(Long id) {
        return prestamoDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prestamo no encontrado con ID: " + id));
    }

    private PedidoInterciudadDTO guardar(PedidoInterciudadDTO pedido) {
        return pedidoDAO.update(pedido.getId(), pedido)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pedido interciudad no encontrado con ID: " + pedido.getId()));
    }

    private void verificarBibliotecario(Long bibliotecarioId) {
        // Por ahora solo se comprueba que exista; la validacion del rol llegara con la seguridad.
        if (!usuarioDAO.existsById(bibliotecarioId)) {
            throw new ResourceNotFoundException("Bibliotecario no encontrado con ID: " + bibliotecarioId);
        }
    }

    private void exigirEstado(PedidoInterciudadDTO pedido, String esperado, String destino) {
        if (!esperado.equalsIgnoreCase(pedido.getEstado())) {
            throw new BusinessValidationException("El pedido debe estar en estado " + esperado
                    + " para pasar a " + destino + " (estado actual: " + pedido.getEstado() + ")");
        }
    }

    private String normalizarEstado(String estado) {
        String normalizado = estado == null ? "" : estado.trim().toUpperCase();
        if (!ESTADOS_PEDIDO.contains(normalizado)) {
            throw new BusinessValidationException(
                    "Estado invalido: " + estado + ". Valores permitidos: " + String.join(", ", ESTADOS_PEDIDO));
        }
        return normalizado;
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