package bussinesLayer.service.impl;

import bussinesLayer.dto.DevolucionDTO;
import bussinesLayer.dto.EjemplarDTO;
import bussinesLayer.dto.PedidoInterciudadDTO;
import bussinesLayer.dto.PrestamoDTO;
import bussinesLayer.dto.MultaDTO;
import bussinesLayer.dto.ResultadoDanioPerdidaDTO;
import bussinesLayer.dto.SedeDTO;
import bussinesLayer.dto.SolicitudDevolucionDTO;
import bussinesLayer.dto.TrasladoRetornoDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.DevolucionService;
import bussinesLayer.service.ReservaLibroService;
import bussinesLayer.service.NotificacionService;
import bussinesLayer.service.AutomatizacionService;
import bussinesLayer.service.MultaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.DevolucionDAO;
import persistanceLayer.dao.EjemplarDAO;
import persistanceLayer.dao.PedidoInterciudadDAO;
import persistanceLayer.dao.PrestamoDAO;
import persistanceLayer.dao.SedeDAO;
import persistanceLayer.dao.TrasladoRetornoDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DevolucionServiceImpl implements DevolucionService {

    private static final String ESTADO_INICIAL = "PENDIENTE";
    private static final String ESTADO_CONFIRMADA = "CONFIRMADA";
    private static final String ESTADO_RECHAZADA = "RECHAZADA";

    private static final String PRESTAMO_ACTIVO = "ACTIVO";
    private static final String PRESTAMO_VENCIDO = "VENCIDO";
    private static final String PRESTAMO_DEVUELTO = "DEVUELTO";
    private static final List<String> PRESTAMOS_DEVOLVIBLES = List.of(PRESTAMO_ACTIVO, PRESTAMO_VENCIDO);

    private static final String EJEMPLAR_PRESTADO = "PRESTADO";
    private static final String EJEMPLAR_DISPONIBLE = "DISPONIBLE";
    private static final String EJEMPLAR_EN_TRANSITO = "EN_TRANSITO";
    private static final String EJEMPLAR_DANADO = "DANADO";
    private static final String EJEMPLAR_PERDIDO = "PERDIDO";

    private final DevolucionDAO devolucionDAO;
    private final PrestamoDAO prestamoDAO;
    private final SedeDAO sedeDAO;
    private final UsuarioDAO usuarioDAO;
    private final EjemplarDAO ejemplarDAO;
    private final TrasladoRetornoDAO trasladoRetornoDAO;
    private final PedidoInterciudadDAO pedidoInterciudadDAO;
    private final ReservaLibroService reservaLibroService;
    private final NotificacionService notificacionService;
    private final AutomatizacionService automatizacionService;
    private final MultaService multaService;

    // ------------------------------------------------------------------ CRUD

    @Override
    public DevolucionDTO crearDevolucion(DevolucionDTO devolucionDTO) {
        log.info("Creando devolucion para el prestamo: {}", devolucionDTO.getPrestamoId());

        validarReferencias(devolucionDTO);

        if (devolucionDAO.existsByPrestamoId(devolucionDTO.getPrestamoId())) {
            throw new BusinessValidationException(
                    "Ya existe una devolucion registrada para el prestamo: " + devolucionDTO.getPrestamoId());
        }
        if (devolucionDAO.findByCodigoQr(devolucionDTO.getCodigoQr()).isPresent()) {
            throw new BusinessValidationException(
                    "Ya existe una devolucion con el codigo QR: " + devolucionDTO.getCodigoQr());
        }

        if (devolucionDTO.getEstado() == null || devolucionDTO.getEstado().isBlank()) {
            devolucionDTO.setEstado(ESTADO_INICIAL);
        }

        DevolucionDTO guardada = devolucionDAO.save(devolucionDTO);
        log.info("Devolucion creada con ID: {}", guardada.getId());
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public DevolucionDTO obtenerDevolucionPorId(Long id) {
        return devolucionDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Devolucion no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DevolucionDTO> obtenerTodasLasDevoluciones() {
        return devolucionDAO.findAll();
    }

    @Override
    public DevolucionDTO actualizarDevolucion(Long id, DevolucionDTO devolucionDTO) {
        if (!devolucionDAO.existsById(id)) {
            throw new ResourceNotFoundException("Devolucion no encontrada con ID: " + id);
        }

        validarReferencias(devolucionDTO);

        devolucionDAO.findByPrestamoId(devolucionDTO.getPrestamoId())
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new BusinessValidationException(
                            "Ya existe otra devolucion registrada para el prestamo: " + devolucionDTO.getPrestamoId());
                });
        devolucionDAO.findByCodigoQr(devolucionDTO.getCodigoQr())
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new BusinessValidationException(
                            "Ya existe otra devolucion con el codigo QR: " + devolucionDTO.getCodigoQr());
                });

        DevolucionDTO actualizada = devolucionDAO.update(id, devolucionDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Devolucion no encontrada con ID: " + id));
        log.info("Devolucion actualizada ID: {}", id);
        return actualizada;
    }

    @Override
    public void eliminarDevolucion(Long id) {
        if (!devolucionDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Devolucion no encontrada con ID: " + id);
        }
        log.info("Devolucion eliminada ID: {}", id);
    }

    // ------------------------------------------------  Devolver en Sede

    @Override
    @Transactional(readOnly = true)
    public SedeDTO sugerirSedeCercana(double latitud, double longitud) {
        return sedeDAO.findMasCercana(latitud, longitud)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay sedes en operacion con coordenadas registradas"));
    }

    @Override
    public DevolucionDTO solicitarDevolucion(SolicitudDevolucionDTO solicitud) {
        PrestamoDTO prestamo = prestamoDAO.findById(solicitud.getPrestamoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Prestamo no encontrado con ID: " + solicitud.getPrestamoId()));

        if (!prestamo.getUsuarioId().equals(solicitud.getUsuarioId())) {
            throw new BusinessValidationException("El prestamo no pertenece al usuario " + solicitud.getUsuarioId());
        }
        if (!PRESTAMOS_DEVOLVIBLES.contains(prestamo.getEstado().toUpperCase())) {
            throw new BusinessValidationException("Solo se puede devolver un prestamo ACTIVO o VENCIDO (estado actual: "
                    + prestamo.getEstado() + ")");
        }

        // Validar si el préstamo pertenece a un pedido interciudad
        Optional<PedidoInterciudadDTO> pedidoOpt = pedidoInterciudadDAO.findByPrestamoId(prestamo.getId());
        if (pedidoOpt.isPresent()) {
            PedidoInterciudadDTO pedido = pedidoOpt.get();
            if (solicitud.getSedeDevolucionId() != null && !solicitud.getSedeDevolucionId().equals(pedido.getSedeDevolucionId())) {
                throw new BusinessValidationException(
                        "Los pedidos interciudad sólo se pueden devolver en la sede de devolución seleccionada al solicitar el pedido: "
                                + pedido.getSedeDevolucionNombre() );
            }
            // Forzar la sede de devolución registrada en el pedido interciudad
            solicitud.setSedeDevolucionId(pedido.getSedeDevolucionId());
        }

        SedeDTO sede = resolverSedeDeDevolucion(solicitud);
        if (!Boolean.TRUE.equals(sede.getEnOperacion())) {
            throw new BusinessValidationException("La sede " + sede.getNombre() + " no esta en operacion");
        }

        DevolucionDTO existente = devolucionDAO.findByPrestamoId(prestamo.getId()).orElse(null);
        if (existente != null && !ESTADO_RECHAZADA.equalsIgnoreCase(existente.getEstado())) {
            throw new BusinessValidationException("Ya existe una devolucion para el prestamo "
                    + prestamo.getId() + " (estado: " + existente.getEstado() + ")");
        }

        LocalDateTime ahora = LocalDateTime.now();
        String codigoQr = "DEV-" + UUID.randomUUID();

        if (existente != null) {
            existente.setCodigoQr(codigoQr);
            existente.setFechaSolicitud(ahora);
            existente.setFechaConfirmacion(null);
            existente.setEstado(ESTADO_INICIAL);
            existente.setObservaciones(null);
            existente.setSedeDevolucionId(sede.getId());
            existente.setBibliotecarioId(null);
            return devolucionDAO.update(existente.getId(), existente)
                    .orElseThrow(() -> new ResourceNotFoundException("Devolucion no encontrada con ID: " + existente.getId()));
        }

        DevolucionDTO nueva = new DevolucionDTO();
        nueva.setCodigoQr(codigoQr);
        nueva.setFechaSolicitud(ahora);
        nueva.setEstado(ESTADO_INICIAL);
        nueva.setPrestamoId(prestamo.getId());
        nueva.setSedeDevolucionId(sede.getId());
        DevolucionDTO guardada = devolucionDAO.save(nueva);
        log.info("Devolucion solicitada ID: {} para el prestamo {} en la sede {}",
                guardada.getId(), prestamo.getId(), sede.getNombre());
        return guardada;
    }

    @Override
    public DevolucionDTO confirmarDevolucion(String codigoQr, Long bibliotecarioId) {
        if (!usuarioDAO.existsById(bibliotecarioId)) {
            throw new ResourceNotFoundException("Bibliotecario no encontrado con ID: " + bibliotecarioId);
        }
        DevolucionDTO devolucion = devolucionDAO.findByCodigoQr(codigoQr)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una devolucion con el codigo QR: " + codigoQr));
        if (!ESTADO_INICIAL.equalsIgnoreCase(devolucion.getEstado())) {
            throw new BusinessValidationException("La devolucion no esta PENDIENTE (estado actual: "
                    + devolucion.getEstado() + ")");
        }

        PrestamoDTO prestamo = prestamoDAO.findById(devolucion.getPrestamoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Prestamo no encontrado con ID: " + devolucion.getPrestamoId()));
        if (!PRESTAMOS_DEVOLVIBLES.contains(prestamo.getEstado().toUpperCase())) {
            throw new BusinessValidationException("El prestamo ya no esta ACTIVO ni VENCIDO (estado actual: "
                    + prestamo.getEstado() + ")");
        }

        // Si es un pedido interciudad, validar sede de recepción y actualizar pedido
        Optional<PedidoInterciudadDTO> pedidoOpt = pedidoInterciudadDAO.findByPrestamoId(prestamo.getId());
        if (pedidoOpt.isPresent()) {
            PedidoInterciudadDTO pedido = pedidoOpt.get();
            if (!devolucion.getSedeDevolucionId().equals(pedido.getSedeDevolucionId())) {
                throw new BusinessValidationException(
                        "Este libro pertenece a un pedido interciudad y sólo puede devolverse en la sede de devolución seleccionada: "
                                + pedido.getSedeDevolucionNombre() );
            }
            pedido.setEstado("DEVUELTO_PENDIENTE_TRASLADO");
            pedido.setBibliotecarioId(bibliotecarioId);
            pedidoInterciudadDAO.update(pedido.getId(), pedido);
        }

        EjemplarDTO ejemplar = ejemplarDAO.findById(prestamo.getEjemplarId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ejemplar no encontrado con ID: " + prestamo.getEjemplarId()));

        LocalDateTime ahora = LocalDateTime.now();

        // Devolución confirmada y préstamo cerrado
        devolucion.setEstado(ESTADO_CONFIRMADA);
        devolucion.setFechaConfirmacion(ahora);
        devolucion.setBibliotecarioId(bibliotecarioId);
        DevolucionDTO confirmada = devolucionDAO.update(devolucion.getId(), devolucion)
                .orElseThrow(() -> new ResourceNotFoundException("Devolucion no encontrada con ID: " + devolucion.getId()));

        automatizacionService.evaluarRetrasoAlDevolver(prestamo.getId());
        prestamo.setEstado(PRESTAMO_DEVUELTO);
        prestamo.setFechaDevolucion(ahora);
        prestamoDAO.update(prestamo.getId(), prestamo);

        // Ejemplar
        boolean devueltoEnOrigen = devolucion.getSedeDevolucionId().equals(ejemplar.getSedeId());
        if (devueltoEnOrigen) {
            cambiarEstadoEjemplar(ejemplar.getId(), EJEMPLAR_PRESTADO, EJEMPLAR_DISPONIBLE);
            reservaLibroService.asignarEjemplarAReservaEnEspera(ejemplar.getId());
        } else {
            cambiarEstadoEjemplar(ejemplar.getId(), EJEMPLAR_PRESTADO, EJEMPLAR_EN_TRANSITO);

            TrasladoRetornoDTO traslado = new TrasladoRetornoDTO();
            traslado.setEstado("EN_TRANSITO");
            traslado.setFechaCreacion(ahora);
            traslado.setEjemplarId(ejemplar.getId());
            traslado.setSedeActualId(devolucion.getSedeDevolucionId());
            traslado.setSedeDestinoId(ejemplar.getSedeId());
            traslado.setDevolucionId(devolucion.getId());
            trasladoRetornoDAO.save(traslado);

            String nombreSedeActual = sedeDAO.findById(devolucion.getSedeDevolucionId())
                    .map(SedeDTO::getNombre).orElse("otra sede");
            reservaLibroService.avisarDevolucionEnCamino(ejemplar.getLibroId(), ejemplar.getSedeId(), nombreSedeActual);
            notificacionService.notificarSede(ejemplar.getSedeId(), "DEVOLUCION_EN_OTRA_SEDE",
                    "Libro devuelto en otra sede",
                    "El ejemplar #" + ejemplar.getId() + " fue devuelto en la sede " + nombreSedeActual
                            + " y va en camino de regreso a tu sede.");
        }

        log.info("Devolucion {} confirmada por el bibliotecario {} (en origen: {})",
                devolucion.getId(), bibliotecarioId, devueltoEnOrigen);
        return confirmada;
    }

    // ------------------------------------------------ RF-41: libro dañado o perdido

    @Override
    public ResultadoDanioPerdidaDTO registrarDanioOPerdida(Long prestamoId, String tipo, Long bibliotecarioId,
                                                        BigDecimal monto, String observaciones) {
        boolean perdido = "PERDIDO".equalsIgnoreCase(tipo);
        if (!perdido && !"DANADO".equalsIgnoreCase(tipo)) {
            throw new BusinessValidationException("El tipo debe ser DANADO o PERDIDO");
        }
        if (!usuarioDAO.existsById(bibliotecarioId)) {
            throw new ResourceNotFoundException("Bibliotecario no encontrado con ID: " + bibliotecarioId);
        }
        PrestamoDTO prestamo = prestamoDAO.findById(prestamoId)
                .orElseThrow(() -> new ResourceNotFoundException("Prestamo no encontrado con ID: " + prestamoId));
        if (!PRESTAMOS_DEVOLVIBLES.contains(prestamo.getEstado().toUpperCase())) {
            throw new BusinessValidationException("Solo se puede registrar dano o perdida de un prestamo ACTIVO o VENCIDO"
                    + " (estado actual: " + prestamo.getEstado() + ")");
        }
        EjemplarDTO ejemplar = ejemplarDAO.findById(prestamo.getEjemplarId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ejemplar no encontrado con ID: " + prestamo.getEjemplarId()));
        String estadoNuevo = perdido ? EJEMPLAR_PERDIDO : EJEMPLAR_DANADO;

        // 1) Multa (monto configurado si no se indica) y sancion segun politica. Valida usuario y monto.
        String motivo = (perdido ? "Libro perdido" : "Libro devuelto danado") + " - ejemplar #" + ejemplar.getId()
                + " (prestamo #" + prestamo.getId() + ")"
                + (observaciones != null && !observaciones.isBlank() ? ": " + observaciones.trim() : "");
        if (motivo.length() > 200) {
            motivo = motivo.substring(0, 200);
        }

        // 2) Mora acumulada hasta hoy (si el prestamo estaba vencido) y cierre del prestamo.
        LocalDateTime ahora = LocalDateTime.now();
        automatizacionService.evaluarRetrasoAlDevolver(prestamo.getId());
        MultaDTO multa = multaService.generarMultaPorDanioOPerdida(
                prestamo.getUsuarioId(), prestamo.getId(), tipo, monto, motivo);
        prestamo.setEstado(PRESTAMO_DEVUELTO);
        prestamo.setFechaDevolucion(ahora);
        prestamoDAO.update(prestamo.getId(), prestamo);

        // 3) El ejemplar sale del stock: el stock disponible se calcula con los ejemplares DISPONIBLE.
        cambiarEstadoEjemplar(ejemplar.getId(), EJEMPLAR_PRESTADO, estadoNuevo);

        // 4) Si el usuario ya habia solicitado la devolucion (QR pendiente), se confirma con la observacion.
        devolucionDAO.findByPrestamoId(prestamo.getId())
                .filter(d -> ESTADO_INICIAL.equalsIgnoreCase(d.getEstado()))
                .ifPresent(d -> {
                    d.setEstado(ESTADO_CONFIRMADA);
                    d.setFechaConfirmacion(ahora);
                    d.setBibliotecarioId(bibliotecarioId);
                    d.setObservaciones(perdido ? "Libro perdido" : "Libro devuelto danado");
                    devolucionDAO.update(d.getId(), d);
                });

        // 5) Pedido interciudad: el libro no volvera a la sede de origen, el pedido queda cerrado.
        pedidoInterciudadDAO.findByPrestamoId(prestamo.getId()).ifPresent(pedido -> {
            pedido.setEstado("CERRADO");
            pedido.setBibliotecarioId(bibliotecarioId);
            pedidoInterciudadDAO.update(pedido.getId(), pedido);
        });

        log.info("Prestamo {} cerrado: ejemplar {} registrado como {} por el bibliotecario {} (multa {})",
                prestamo.getId(), ejemplar.getId(), estadoNuevo, bibliotecarioId, multa.getId());
        return new ResultadoDanioPerdidaDTO(prestamo.getId(), ejemplar.getId(), estadoNuevo, multa);
    }

    // -------------------------------------------------------------- Privados

    private SedeDTO resolverSedeDeDevolucion(SolicitudDevolucionDTO solicitud) {
        if (solicitud.getSedeDevolucionId() != null) {
            return sedeDAO.findById(solicitud.getSedeDevolucionId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Sede no encontrada con ID: " + solicitud.getSedeDevolucionId()));
        }
        if (solicitud.getLatitud() != null && solicitud.getLongitud() != null) {
            return sedeDAO.findMasCercana(solicitud.getLatitud(), solicitud.getLongitud())
                    .orElseThrow(() -> new BusinessValidationException(
                            "No hay sedes en operacion con coordenadas registradas"));
        }
        throw new BusinessValidationException(
                "Indique sedeDevolucionId o, para que el sistema elija la sede mas cercana, latitud y longitud");
    }

    private void cambiarEstadoEjemplar(Long ejemplarId, String esperado, String nuevo) {
        if (ejemplarDAO.cambiarEstadoSiEs(ejemplarId, esperado, nuevo) == 0) {
            throw new BusinessValidationException(
                    "El ejemplar del prestamo no esta en el estado esperado (" + esperado + ")");
        }
    }

    private void validarReferencias(DevolucionDTO dto) {
        if (!prestamoDAO.existsById(dto.getPrestamoId())) {
            throw new ResourceNotFoundException("Prestamo no encontrado con ID: " + dto.getPrestamoId());
        }
        if (!sedeDAO.existsById(dto.getSedeDevolucionId())) {
            throw new ResourceNotFoundException("Sede no encontrada con ID: " + dto.getSedeDevolucionId());
        }
        if (dto.getBibliotecarioId() != null && !usuarioDAO.existsById(dto.getBibliotecarioId())) {
            throw new ResourceNotFoundException("Bibliotecario no encontrado con ID: " + dto.getBibliotecarioId());
        }
    }
}