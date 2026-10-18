package bussinesLayer.service.impl;

import bussinesLayer.dto.InscripcionEventoDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.InscripcionEventoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.EventoDAO;
import persistanceLayer.dao.InscripcionEventoDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class InscripcionEventoServiceImpl implements InscripcionEventoService {

    private static final String ESTADO_ACTIVA = "ACTIVA";
    private static final String ESTADO_CANCELADA = "CANCELADA";

    private final InscripcionEventoDAO inscripcionEventoDAO;
    private final EventoDAO eventoDAO;
    private final UsuarioDAO usuarioDAO;

    @Override
    public InscripcionEventoDTO crearInscripcionEvento(InscripcionEventoDTO dto) {
        log.info("Creando inscripcion del usuario {} al evento {}", dto.getUsuarioId(), dto.getEventoId());

        validarReferencias(dto);

        if (inscripcionEventoDAO.existsByEventoIdAndUsuarioId(dto.getEventoId(), dto.getUsuarioId())) {
            throw new BusinessValidationException("El usuario ya esta inscrito en este evento");
        }

        if (dto.getCantidadCupos() == null) {
            dto.setCantidadCupos(1);
        }
        dto.setEstado(ESTADO_ACTIVA);
        dto.setFechaCancelacion(null);

        if (eventoDAO.descontarCupos(dto.getEventoId(), dto.getCantidadCupos()) == 0) {
            throw new BusinessValidationException(
                    "El evento no esta programado o no tiene cupos suficientes");
        }

        InscripcionEventoDTO guardada = inscripcionEventoDAO.save(dto);
        log.info("Inscripcion creada con ID: {}", guardada.getId());
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public InscripcionEventoDTO obtenerInscripcionEventoPorId(Long id) {
        return inscripcionEventoDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripcion no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InscripcionEventoDTO> obtenerTodasLasInscripcionesEvento() {
        return inscripcionEventoDAO.findAll();
    }

    @Override
    public InscripcionEventoDTO actualizarInscripcionEvento(Long id, InscripcionEventoDTO dto) {
        InscripcionEventoDTO actual = inscripcionEventoDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripcion no encontrada con ID: " + id));

        if (!actual.getEventoId().equals(dto.getEventoId()) || !actual.getUsuarioId().equals(dto.getUsuarioId())) {
            throw new BusinessValidationException("No se puede cambiar el evento ni el usuario de una inscripcion");
        }

        boolean estabaActiva = ESTADO_ACTIVA.equalsIgnoreCase(actual.getEstado());
        boolean cancelar = dto.getEstado() != null && ESTADO_CANCELADA.equalsIgnoreCase(dto.getEstado());
        boolean reactivar = dto.getEstado() != null && ESTADO_ACTIVA.equalsIgnoreCase(dto.getEstado());

        if (estabaActiva && cancelar) {
            eventoDAO.liberarCupos(actual.getEventoId(), actual.getCantidadCupos());
            if (dto.getFechaCancelacion() == null) {
                dto.setFechaCancelacion(LocalDateTime.now());
            }
        } else if (!estabaActiva && reactivar) {
            if (eventoDAO.descontarCupos(actual.getEventoId(), actual.getCantidadCupos()) == 0) {
                throw new BusinessValidationException(
                        "El evento no esta programado o no tiene cupos suficientes");
            }
            dto.setFechaCancelacion(null);
        }
        // La cantidad de cupos no se modifica una vez reservada, para mantener consistente el cupo del evento.
        dto.setCantidadCupos(actual.getCantidadCupos());

        InscripcionEventoDTO actualizada = inscripcionEventoDAO.update(id, dto)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripcion no encontrada con ID: " + id));
        log.info("Inscripcion actualizada ID: {}", id);
        return actualizada;
    }

    @Override
    public void eliminarInscripcionEvento(Long id) {
        InscripcionEventoDTO actual = inscripcionEventoDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripcion no encontrada con ID: " + id));

        if (ESTADO_ACTIVA.equalsIgnoreCase(actual.getEstado())) {
            eventoDAO.liberarCupos(actual.getEventoId(), actual.getCantidadCupos());
        }

        inscripcionEventoDAO.deleteById(id);
        log.info("Inscripcion eliminada ID: {}", id);
    }

    private void validarReferencias(InscripcionEventoDTO dto) {
        if (!eventoDAO.existsById(dto.getEventoId())) {
            throw new ResourceNotFoundException("Evento no encontrado con ID: " + dto.getEventoId());
        }
        if (!usuarioDAO.existsById(dto.getUsuarioId())) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + dto.getUsuarioId());
        }
    }
}
