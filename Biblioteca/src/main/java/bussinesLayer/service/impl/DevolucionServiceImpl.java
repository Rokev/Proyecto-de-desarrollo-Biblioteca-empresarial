package bussinesLayer.service.impl;

import bussinesLayer.dto.DevolucionDTO;
import bussinesLayer.dto.EjemplarDTO;
import bussinesLayer.dto.PrestamoDTO;
import bussinesLayer.dto.SedeDTO;
import bussinesLayer.dto.SolicitudDevolucionDTO;
import bussinesLayer.dto.TrasladoRetornoDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.DevolucionService;
import bussinesLayer.service.ReservaLibroService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.DevolucionDAO;
import persistanceLayer.dao.EjemplarDAO;
import persistanceLayer.dao.PrestamoDAO;
import persistanceLayer.dao.SedeDAO;
import persistanceLayer.dao.TrasladoRetornoDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.time.LocalDateTime;
import java.util.List;
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

    private final DevolucionDAO devolucionDAO;
    private final PrestamoDAO prestamoDAO;
    private final SedeDAO sedeDAO;
    private final UsuarioDAO usuarioDAO;
    private final EjemplarDAO ejemplarDAO;
    private final TrasladoRetornoDAO trasladoRetornoDAO;
    private final ReservaLibroService reservaLibroService;

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

    // ------------------------------------------------ RF-08 devolver en cualquier sede

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
            // Reintento despues de una devolucion rechazada: se reutiliza el registro con un QR nuevo.
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
        // Por ahora solo se comprueba que el bibliotecario exista; el rol se validara con la seguridad.
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
        EjemplarDTO ejemplar = ejemplarDAO.findById(prestamo.getEjemplarId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ejemplar no encontrado con ID: " + prestamo.getEjemplarId()));

        LocalDateTime ahora = LocalDateTime.now();

        // 1) Devolucion confirmada y prestamo cerrado.
        devolucion.setEstado(ESTADO_CONFIRMADA);
        devolucion.setFechaConfirmacion(ahora);
        devolucion.setBibliotecarioId(bibliotecarioId);
        DevolucionDTO confirmada = devolucionDAO.update(devolucion.getId(), devolucion)
                .orElseThrow(() -> new ResourceNotFoundException("Devolucion no encontrada con ID: " + devolucion.getId()));

        prestamo.setEstado(PRESTAMO_DEVUELTO);
        prestamo.setFechaDevolucion(ahora);
        prestamoDAO.update(prestamo.getId(), prestamo);

        // 2) Ejemplar: el origen es la sede propietaria (ejemplar.sede), que no cambia al prestarse.
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

            // 3) Aviso a quien tiene reservado este libro en la sede de origen.
            String nombreSedeActual = sedeDAO.findById(devolucion.getSedeDevolucionId())
                    .map(SedeDTO::getNombre).orElse("otra sede");
            reservaLibroService.avisarDevolucionEnCamino(ejemplar.getLibroId(), ejemplar.getSedeId(), nombreSedeActual);
        }

        log.info("Devolucion {} confirmada por el bibliotecario {} (en origen: {})",
                devolucion.getId(), bibliotecarioId, devueltoEnOrigen);
        return confirmada;
    }

    // -------------------------------------------------------------- privados

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