package bussinesLayer.service.impl;
import java.util.Optional;
import persistanceLayer.dao.NotificacionDAO;
import bussinesLayer.dto.NotificacionDTO;
import bussinesLayer.dto.InscripcionEventoDTO;
import java.time.LocalDateTime;
import java.time.LocalDate;

import bussinesLayer.dto.EventoDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.EventoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.EventoDAO;
import persistanceLayer.dao.SedeDAO;
import persistanceLayer.dao.InscripcionEventoDAO;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class EventoServiceImpl implements EventoService {

    private static final String ESTADO_INICIAL = "PROGRAMADO";

    private final EventoDAO eventoDAO;
    private final SedeDAO sedeDAO;
    private final InscripcionEventoDAO inscripcionEventoDAO;
    private final NotificacionDAO notificacionDAO;

    @Override
    public EventoDTO crearEvento(EventoDTO eventoDTO) {
        log.info("Creando evento: {}", eventoDTO.getTitulo());

        validarEvento(eventoDTO);

        if (eventoDTO.getEstado() == null || eventoDTO.getEstado().isBlank()) {
            eventoDTO.setEstado(ESTADO_INICIAL);
        }
        if (eventoDTO.getExclusivo() == null) {
            eventoDTO.setExclusivo(false);
        }

        EventoDTO guardado = eventoDAO.save(eventoDTO);
        log.info("Evento creado con ID: {}", guardado.getId());
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public EventoDTO obtenerEventoPorId(Long id) {
        return eventoDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoDTO> obtenerTodosLosEventos() {
        return eventoDAO.findAll();
    }

    @Override
    public EventoDTO actualizarEvento(Long id, EventoDTO eventoDTO) {
        EventoDTO actual = eventoDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + id));

        validarEvento(eventoDTO);
        //  45: el cupo maximo no puede quedar por debajo de los cupos ya reservados (inscripciones activas).
        int reservados = inscripcionEventoDAO.findByEventoIdAndEstadoIgnoreCase(id, "ACTIVA").stream()
                .mapToInt(i -> i.getCantidadCupos() == null ? 1 : i.getCantidadCupos())
                .sum();
        if (eventoDTO.getCupoMaximo() < reservados) {
            throw new BusinessValidationException("No se pueden reducir los cupos a " + eventoDTO.getCupoMaximo()
                    + " porque el evento tiene " + reservados + " cupos reservados");
        }
        // Los cupos disponibles se recalculan; no se aceptan los enviados por el cliente.
        eventoDTO.setCuposDisponibles(eventoDTO.getCupoMaximo() - reservados);

        if (eventoDTO.getExclusivo() != null
                && !eventoDTO.getExclusivo().equals(actual.getExclusivo())
                && !inscripcionEventoDAO.findByEventoIdAndEstadoIgnoreCase(id, "ACTIVA").isEmpty()) {
            throw new BusinessValidationException(
                    "No se puede cambiar la exclusividad de un evento con inscripciones activas");
        }
        EventoDTO actualizado = eventoDAO.update(id, eventoDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + id));
        log.info("Evento actualizado ID: {}", id);
        return actualizado;
    }

    @Override
    public void eliminarEvento(Long id) {
        //  47: eliminar y cancelar siguen la misma regla.
        cancelarEvento(id);
        log.info("Evento eliminado ID: {}", id);
    }

    private void validarEvento(EventoDTO dto) {
        if (!dto.getFechaFin().isAfter(dto.getFechaInicio())) {
            throw new BusinessValidationException("La fechaFin debe ser posterior a la fechaInicio");
        }
        if (dto.getCuposDisponibles() > dto.getCupoMaximo()) {
            throw new BusinessValidationException("Los cuposDisponibles no pueden superar el cupoMaximo");
        }
        if (!sedeDAO.existsById(dto.getSedeId())) {
            throw new ResourceNotFoundException("Sede no encontrada con ID: " + dto.getSedeId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventoDTO> buscarEventosProximos(Long sedeId, LocalDate fecha, Boolean exclusivo) {
        return eventoDAO.findByFechaInicioGreaterThanEqualAndEstadoIgnoreCaseOrderByFechaInicioAsc(
                        LocalDateTime.now(), ESTADO_INICIAL).stream()
                .filter(e -> sedeId == null || sedeId.equals(e.getSedeId()))
                .filter(e -> fecha == null || fecha.equals(e.getFechaInicio().toLocalDate()))
                .filter(e -> exclusivo == null || exclusivo.equals(Boolean.TRUE.equals(e.getExclusivo())))
                .toList();
    }

    @Override
    public Optional<EventoDTO> cancelarEvento(Long id) {
        EventoDTO evento = eventoDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + id));
        if (!ESTADO_INICIAL.equalsIgnoreCase(evento.getEstado())) {
            throw new BusinessValidationException(
                    "Solo se pueden cancelar eventos programados (estado actual: " + evento.getEstado() + ")");
        }
        List<InscripcionEventoDTO> inscripciones = inscripcionEventoDAO.findByEventoId(id);
        if (inscripciones.isEmpty()) {
            eventoDAO.deleteById(id);
            log.info("Evento eliminado ID: {} (sin reservas)", id);
            return Optional.empty();
        }
        LocalDateTime ahora = LocalDateTime.now();
        for (InscripcionEventoDTO inscripcion : inscripciones) {
            if (!"ACTIVA".equalsIgnoreCase(inscripcion.getEstado())) {
                continue;
            }
            inscripcion.setEstado("CANCELADA");
            inscripcion.setFechaCancelacion(ahora);
            inscripcionEventoDAO.update(inscripcion.getId(), inscripcion);
            NotificacionDTO n = new NotificacionDTO();
            n.setUsuarioId(inscripcion.getUsuarioId());
            n.setTipo("EVENTO_CANCELADO");
            n.setTitulo("Evento cancelado");
            n.setMensaje("El evento \"" + evento.getTitulo() + "\" programado para el " + evento.getFechaInicio()
                    + " fue cancelado. Se liberaron tus " + inscripcion.getCantidadCupos() + " cupo(s).");
            n.setCanal("APP");
            n.setFechaCreacion(ahora);
            n.setLeida(false);
            notificacionDAO.save(n);
        }
        evento.setEstado("CANCELADO");
        evento.setCuposDisponibles(evento.getCupoMaximo());
        EventoDTO cancelado = eventoDAO.update(id, evento)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + id));
        log.info("Evento cancelado ID: {}", id);
        return Optional.of(cancelado);
    }
}
