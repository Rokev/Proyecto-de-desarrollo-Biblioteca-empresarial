package bussinesLayer.service.impl;
import persistanceLayer.dao.LibroDAO;
import bussinesLayer.dto.PrestamoDetalleDTO;

import bussinesLayer.dto.EjemplarDTO;
import bussinesLayer.dto.PrestamoDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.ConfiguracionSistemaService;
import bussinesLayer.service.PrestamoService;
import bussinesLayer.service.ValidadorUsuarioService;
import bussinesLayer.util.ClavePlazo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.EjemplarDAO;
import persistanceLayer.dao.PrestamoDAO;
import persistanceLayer.dao.SancionDAO;
import persistanceLayer.dao.SedeDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PrestamoServiceImpl implements PrestamoService {

    private static final int MAX_PRESTAMOS_VIGENTES = 3;

    private static final String ESTADO_SOLICITADO = "SOLICITADO";
    private static final String ESTADO_ACTIVO = "ACTIVO";
    private static final String ESTADO_VENCIDO = "VENCIDO";
    private static final String ESTADO_DEVUELTO = "DEVUELTO";
    private static final String ESTADO_CANCELADO = "CANCELADO";
    private static final List<String> ESTADOS_VIGENTES = List.of(ESTADO_SOLICITADO, ESTADO_ACTIVO, ESTADO_VENCIDO);

    private static final String EJEMPLAR_DISPONIBLE = "DISPONIBLE";
    private static final String EJEMPLAR_PRESTADO = "PRESTADO";

    private static final String MODALIDAD_SEDE = "SEDE";
    private static final String MODALIDAD_DOMICILIO = "DOMICILIO";
    private static final String MODALIDAD_INTERCIUDAD = "INTERCIUDAD";

    private final PrestamoDAO prestamoDAO;
    private final UsuarioDAO usuarioDAO;
    private final EjemplarDAO ejemplarDAO;
    private final SedeDAO sedeDAO;
    private final LibroDAO libroDAO;
    private final SancionDAO sancionDAO;
    private final ValidadorUsuarioService validadorUsuarioService;
    private final ConfiguracionSistemaService configuracionService;

    @Override
    public PrestamoDTO crearPrestamo(PrestamoDTO dto) {
        log.info("Creando prestamo del ejemplar {} para el usuario {}", dto.getEjemplarId(), dto.getUsuarioId());

        // 1. Validar que el usuario no tenga sanciones activas ni multas pendientes 
        validadorUsuarioService.validarUsuarioHabilitadoParaOperaciones(dto.getUsuarioId());

        if (!usuarioDAO.existsById(dto.getUsuarioId())) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + dto.getUsuarioId());
        }
        if (!sedeDAO.existsById(dto.getSedeRetiroId())) {
            throw new ResourceNotFoundException("Sede no encontrada con ID: " + dto.getSedeRetiroId());
        }
        EjemplarDTO ejemplar = ejemplarDAO.findById(dto.getEjemplarId())
                .orElseThrow(() -> new ResourceNotFoundException("Ejemplar no encontrado con ID: " + dto.getEjemplarId()));

        if (prestamoDAO.countByUsuarioIdAndEstadoIn(dto.getUsuarioId(), ESTADOS_VIGENTES) >= MAX_PRESTAMOS_VIGENTES) {
            throw new BusinessValidationException(
                    "El usuario alcanzo el maximo de " + MAX_PRESTAMOS_VIGENTES + " prestamos vigentes");
        }
        if (!EJEMPLAR_DISPONIBLE.equalsIgnoreCase(ejemplar.getEstado())
                || prestamoDAO.existsByEjemplarIdAndEstadoIn(dto.getEjemplarId(), ESTADOS_VIGENTES)) {
            throw new BusinessValidationException("El ejemplar no esta disponible para prestamo");
        }

        normalizarModalidad(dto);
        dto.setEstado(ESTADO_SOLICITADO);
        dto.setVecesRenovado(0);
        dto.setFechaDevolucion(null);
        if (dto.getFechaVencimiento() == null) {
            dto.setFechaVencimiento(LocalDate.now().plusDays(configuracionService.plazo(ClavePlazo.DIAS_PRESTAMO)));
        } else if (dto.getFechaVencimiento().isBefore(dto.getFechaSolicitud().toLocalDate())) {
            throw new BusinessValidationException("La fechaVencimiento no puede ser anterior a la fechaSolicitud");
        }

        cambiarEstadoEjemplar(ejemplar, EJEMPLAR_PRESTADO);

        PrestamoDTO guardado = prestamoDAO.save(dto);
        log.info("Prestamo creado con ID: {}", guardado.getId());
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public PrestamoDTO obtenerPrestamoPorId(Long id) {
        return prestamoDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prestamo no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrestamoDTO> obtenerTodosLosPrestamos() {
        return prestamoDAO.findAll();
    }

    @Override
    public PrestamoDTO actualizarPrestamo(Long id, PrestamoDTO dto) {
        PrestamoDTO actual = prestamoDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prestamo no encontrado con ID: " + id));

        if (!actual.getUsuarioId().equals(dto.getUsuarioId()) || !actual.getEjemplarId().equals(dto.getEjemplarId())) {
            throw new BusinessValidationException("No se puede cambiar el usuario ni el ejemplar de un prestamo");
        }
        if (!sedeDAO.existsById(dto.getSedeRetiroId())) {
            throw new ResourceNotFoundException("Sede no encontrada con ID: " + dto.getSedeRetiroId());
        }
        normalizarModalidad(dto);

        if (dto.getEstado() == null || dto.getEstado().isBlank()) {
            dto.setEstado(actual.getEstado());
        }
        if (dto.getVecesRenovado() == null) {
            dto.setVecesRenovado(actual.getVecesRenovado());
        }

        boolean estabaVigente = ESTADOS_VIGENTES.contains(actual.getEstado().toUpperCase());
        boolean cierra = ESTADO_DEVUELTO.equalsIgnoreCase(dto.getEstado())
                || ESTADO_CANCELADO.equalsIgnoreCase(dto.getEstado());

        if (ESTADO_DEVUELTO.equalsIgnoreCase(dto.getEstado()) && dto.getFechaDevolucion() == null) {
            dto.setFechaDevolucion(LocalDateTime.now());
        }
        if (ESTADO_ACTIVO.equalsIgnoreCase(dto.getEstado()) && dto.getFechaEntrega() == null
                && actual.getFechaEntrega() == null) {
            dto.setFechaEntrega(LocalDateTime.now());
        }
        if (estabaVigente && cierra) {
            ejemplarDAO.findById(actual.getEjemplarId())
                    .ifPresent(ej -> cambiarEstadoEjemplar(ej, EJEMPLAR_DISPONIBLE));
        }

        PrestamoDTO actualizado = prestamoDAO.update(id, dto)
                .orElseThrow(() -> new ResourceNotFoundException("Prestamo no encontrado con ID: " + id));
        log.info("Prestamo actualizado ID: {}", id);
        return actualizado;
    }

    @Override
    public void eliminarPrestamo(Long id) {
        PrestamoDTO actual = prestamoDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prestamo no encontrado con ID: " + id));

        if (ESTADOS_VIGENTES.contains(actual.getEstado().toUpperCase())) {
            ejemplarDAO.findById(actual.getEjemplarId())
                    .ifPresent(ej -> cambiarEstadoEjemplar(ej, EJEMPLAR_DISPONIBLE));
        }
        prestamoDAO.deleteById(id);
        log.info("Prestamo eliminado ID: {}", id);
    }

    private void normalizarModalidad(PrestamoDTO dto) {
        if (dto.getModalidad() == null || dto.getModalidad().isBlank()) {
            dto.setModalidad(MODALIDAD_SEDE);
        }
        String modalidad = dto.getModalidad().toUpperCase();
        if (!List.of(MODALIDAD_SEDE, MODALIDAD_DOMICILIO, MODALIDAD_INTERCIUDAD).contains(modalidad)) {
            throw new BusinessValidationException(
                    "Modalidad invalida: " + dto.getModalidad() + ". Valores permitidos: SEDE, DOMICILIO, INTERCIUDAD");
        }
        if (MODALIDAD_DOMICILIO.equals(modalidad)
                && (dto.getDireccionEntrega() == null || dto.getDireccionEntrega().isBlank())) {
            throw new BusinessValidationException("La direccionEntrega es obligatoria para la modalidad DOMICILIO");
        }
        dto.setModalidad(modalidad);
    }

    private void cambiarEstadoEjemplar(EjemplarDTO ejemplar, String nuevoEstado) {
        ejemplar.setEstado(nuevoEstado);
        ejemplarDAO.update(ejemplar.getId(), ejemplar);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrestamoDetalleDTO> obtenerPrestamosDeUsuario(Long usuarioId, String estado) {
        if (!usuarioDAO.existsById(usuarioId)) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId);
        }
        List<PrestamoDTO> prestamos = (estado == null || estado.isBlank())
                ? prestamoDAO.findByUsuarioIdOrderByFechaSolicitudDesc(usuarioId)
                : prestamoDAO.findByUsuarioIdAndEstadoIgnoreCase(usuarioId, estado);
        return prestamos.stream().map(this::aDetalle).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrestamoDetalleDTO> obtenerPrestamosDeSede(Long sedeId, String estado, Long usuarioId, Long libroId) {
        if (!sedeDAO.existsById(sedeId)) {
            throw new ResourceNotFoundException("Sede no encontrada con ID: " + sedeId);
        }
        return prestamoDAO.findBySedeRetiroId(sedeId).stream()
                .filter(p -> estado == null || estado.isBlank() || estado.equalsIgnoreCase(p.getEstado()))
                .filter(p -> usuarioId == null || usuarioId.equals(p.getUsuarioId()))
                .map(this::aDetalle)
                .filter(d -> libroId == null || libroId.equals(d.getLibroId()))
                .sorted(java.util.Comparator.comparing(PrestamoDetalleDTO::getFechaSolicitud,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .toList();
    }

    private PrestamoDetalleDTO aDetalle(PrestamoDTO p) {
        EjemplarDTO ejemplar = ejemplarDAO.findById(p.getEjemplarId()).orElse(null);
        Long libroId = ejemplar == null ? null : ejemplar.getLibroId();
        String titulo = libroId == null ? null : libroDAO.findById(libroId).map(l -> l.getTitulo()).orElse(null);
        String sede = p.getSedeRetiroId() == null ? null
                : sedeDAO.findById(p.getSedeRetiroId()).map(s -> s.getNombre()).orElse(null);
        return new PrestamoDetalleDTO(p.getId(), p.getEstado(), p.getModalidad(), p.getUsuarioId(), libroId, titulo,
                p.getEjemplarId(), p.getSedeRetiroId(), sede, p.getFechaSolicitud(), p.getFechaEntrega(),
                p.getFechaVencimiento(), p.getFechaDevolucion());
    }
}