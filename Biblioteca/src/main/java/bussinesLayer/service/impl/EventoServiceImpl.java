package bussinesLayer.service.impl;

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

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class EventoServiceImpl implements EventoService {

    private static final String ESTADO_INICIAL = "PROGRAMADO";

    private final EventoDAO eventoDAO;
    private final SedeDAO sedeDAO;

    @Override
    public EventoDTO crearEvento(EventoDTO eventoDTO) {
        log.info("Creando evento: {}", eventoDTO.getTitulo());

        validarEvento(eventoDTO);

        if (eventoDTO.getEstado() == null || eventoDTO.getEstado().isBlank()) {
            eventoDTO.setEstado(ESTADO_INICIAL);
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
        if (!eventoDAO.existsById(id)) {
            throw new ResourceNotFoundException("Evento no encontrado con ID: " + id);
        }

        validarEvento(eventoDTO);

        EventoDTO actualizado = eventoDAO.update(id, eventoDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con ID: " + id));
        log.info("Evento actualizado ID: {}", id);
        return actualizado;
    }

    @Override
    public void eliminarEvento(Long id) {
        if (!eventoDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Evento no encontrado con ID: " + id);
        }
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
}
