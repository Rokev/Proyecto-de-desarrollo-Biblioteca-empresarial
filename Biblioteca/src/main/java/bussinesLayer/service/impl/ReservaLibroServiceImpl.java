package bussinesLayer.service.impl;

import bussinesLayer.dto.ReservaLibroDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.ReservaLibroService;
import bussinesLayer.service.ValidadorUsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.EjemplarDAO;
import persistanceLayer.dao.LibroDAO;
import persistanceLayer.dao.ReservaLibroDAO;
import persistanceLayer.dao.SedeDAO;
import persistanceLayer.dao.UsuarioDAO;
import bussinesLayer.dto.EjemplarDTO;
import bussinesLayer.dto.LibroDTO;
import bussinesLayer.dto.NotificacionDTO;
import persistanceLayer.dao.NotificacionDAO;
import java.util.Optional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ReservaLibroServiceImpl implements ReservaLibroService {

    private static final String ESTADO_EN_ESPERA = "EN_ESPERA";
    private static final String ESTADO_NOTIFICADA = "NOTIFICADA";
    private static final String ESTADO_CANCELADA = "CANCELADA";
    /** Dias que tiene el usuario para retirar el libro una vez notificado. */
    private static final int DIAS_RETIRO_RESERVA = 2;
    private final NotificacionDAO notificacionDAO;

    private final ReservaLibroDAO reservaLibroDAO;
    private final UsuarioDAO usuarioDAO;
    private final LibroDAO libroDAO;
    private final SedeDAO sedeDAO;
    private final EjemplarDAO ejemplarDAO;
    private final ValidadorUsuarioService validadorUsuarioService;

    @Override
    public ReservaLibroDTO crearReservaLibro(ReservaLibroDTO dto) {
        log.info("Creando reserva del libro {} para el usuario {}", dto.getLibroId(), dto.getUsuarioId());

        // Validar multas y sanciones
        validadorUsuarioService.validarUsuarioHabilitadoParaOperaciones(dto.getUsuarioId());

        validarReferencias(dto);

        if (reservaLibroDAO.existsByUsuarioIdAndLibroIdAndEstadoIn(
                dto.getUsuarioId(), dto.getLibroId(), List.of(ESTADO_EN_ESPERA, ESTADO_NOTIFICADA))) {
            throw new BusinessValidationException("El usuario ya tiene una reserva vigente para este libro");
        }
        if (dto.getEstado() == null || dto.getEstado().isBlank()) {
            dto.setEstado(ESTADO_EN_ESPERA);
        }

        ReservaLibroDTO guardada = reservaLibroDAO.save(dto);
        log.info("Reserva de libro creada con ID: {}", guardada.getId());
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaLibroDTO obtenerReservaLibroPorId(Long id) {
        return reservaLibroDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva de libro no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaLibroDTO> obtenerTodasLasReservasLibro() {
        return reservaLibroDAO.findAll();
    }

    @Override
    public ReservaLibroDTO actualizarReservaLibro(Long id, ReservaLibroDTO dto) {
        ReservaLibroDTO actual = reservaLibroDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva de libro no encontrada con ID: " + id));

        if (!actual.getUsuarioId().equals(dto.getUsuarioId()) || !actual.getLibroId().equals(dto.getLibroId())) {
            throw new BusinessValidationException("No se puede cambiar el usuario ni el libro de una reserva");
        }
        validarReferencias(dto);

        if (dto.getEstado() != null && ESTADO_CANCELADA.equalsIgnoreCase(dto.getEstado())
                && !ESTADO_CANCELADA.equalsIgnoreCase(actual.getEstado())
                && dto.getFechaCancelacion() == null) {
            dto.setFechaCancelacion(LocalDateTime.now());
        }

        ReservaLibroDTO actualizada = reservaLibroDAO.update(id, dto)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva de libro no encontrada con ID: " + id));
        log.info("Reserva de libro actualizada ID: {}", id);
        return actualizada;
    }

    @Override
    public void eliminarReservaLibro(Long id) {
        if (!reservaLibroDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Reserva de libro no encontrada con ID: " + id);
        }
        log.info("Reserva de libro eliminada ID: {}", id);
    }

    @Override
    public void avisarDevolucionEnCamino(Long libroId, Long sedeOrigenId, String nombreSedeActual) {
        reservaLibroDAO.findFirstByLibroIdAndSedeIdAndEstadoIgnoreCaseOrderByFechaReservaAsc(
                libroId, sedeOrigenId, ESTADO_EN_ESPERA).ifPresent(reserva ->
                notificar(reserva.getUsuarioId(), "LIBRO_PRONTO_DISPONIBLE", "Tu libro ya fue devuelto",
                        "El libro \"" + tituloDe(libroId) + "\" que reservaste fue devuelto en " + nombreSedeActual
                                + " y va de regreso a la sede donde lo reservaste. Te avisaremos cuando este disponible para retiro."));
    }

    @Override
    public boolean asignarEjemplarAReservaEnEspera(Long ejemplarId) {
        EjemplarDTO ejemplar = ejemplarDAO.findById(ejemplarId)
                .orElseThrow(() -> new ResourceNotFoundException("Ejemplar no encontrado con ID: " + ejemplarId));

        Optional<ReservaLibroDTO> siguiente = reservaLibroDAO
                .findFirstByLibroIdAndSedeIdAndEstadoIgnoreCaseOrderByFechaReservaAsc(
                        ejemplar.getLibroId(), ejemplar.getSedeId(), ESTADO_EN_ESPERA);
        if (siguiente.isEmpty()) {
            return false;
        }
        // Se aparta el ejemplar de forma atomica para que nadie mas lo tome.
        if (ejemplarDAO.cambiarEstadoSiEs(ejemplarId, "DISPONIBLE", "RESERVADO") == 0) {
            return false;
        }

        ReservaLibroDTO reserva = siguiente.get();
        LocalDateTime ahora = LocalDateTime.now();
        reserva.setEstado(ESTADO_NOTIFICADA);
        reserva.setEjemplarId(ejemplarId);
        reserva.setFechaNotificacion(ahora);
        reserva.setFechaLimiteRetiro(ahora.plusDays(DIAS_RETIRO_RESERVA));
        reservaLibroDAO.update(reserva.getId(), reserva);

        notificar(reserva.getUsuarioId(), "LIBRO_DISPONIBLE", "Tu libro esta disponible",
                "El libro \"" + tituloDe(ejemplar.getLibroId()) + "\" que reservaste ya esta disponible. Tienes hasta el "
                        + reserva.getFechaLimiteRetiro() + " para retirarlo.");
        log.info("Ejemplar {} asignado a la reserva {}", ejemplarId, reserva.getId());
        return true;
    }

    private String tituloDe(Long libroId) {
        return libroDAO.findById(libroId).map(LibroDTO::getTitulo).orElse("(libro " + libroId + ")");
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

    private void validarReferencias(ReservaLibroDTO dto) {
        if (!usuarioDAO.existsById(dto.getUsuarioId())) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + dto.getUsuarioId());
        }
        if (!libroDAO.existsById(dto.getLibroId())) {
            throw new ResourceNotFoundException("Libro no encontrado con ID: " + dto.getLibroId());
        }
        if (!sedeDAO.existsById(dto.getSedeId())) {
            throw new ResourceNotFoundException("Sede no encontrada con ID: " + dto.getSedeId());
        }
        if (dto.getEjemplarId() != null && !ejemplarDAO.existsById(dto.getEjemplarId())) {
            throw new ResourceNotFoundException("Ejemplar no encontrado con ID: " + dto.getEjemplarId());
        }
    }

    
}
