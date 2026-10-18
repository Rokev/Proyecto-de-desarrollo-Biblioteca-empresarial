package bussinesLayer.service.impl;

import bussinesLayer.dto.PreguntaFrecuenteDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.PreguntaFrecuenteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.PreguntaFrecuenteDAO;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PreguntaFrecuenteServiceImpl implements PreguntaFrecuenteService {

    private final PreguntaFrecuenteDAO preguntaFrecuenteDAO;

    @Override
    public PreguntaFrecuenteDTO crearPreguntaFrecuente(PreguntaFrecuenteDTO dto) {
        log.info("Creando pregunta frecuente: {}", dto.getPregunta());

        if (dto.getActiva() == null) {
            dto.setActiva(true);
        }

        PreguntaFrecuenteDTO guardada = preguntaFrecuenteDAO.save(dto);
        log.info("Pregunta frecuente creada con ID: {}", guardada.getId());
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public PreguntaFrecuenteDTO obtenerPreguntaFrecuentePorId(Long id) {
        return preguntaFrecuenteDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pregunta frecuente no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PreguntaFrecuenteDTO> obtenerTodasLasPreguntasFrecuentes() {
        return preguntaFrecuenteDAO.findAll();
    }

    @Override
    public PreguntaFrecuenteDTO actualizarPreguntaFrecuente(Long id, PreguntaFrecuenteDTO dto) {
        PreguntaFrecuenteDTO actualizada = preguntaFrecuenteDAO.update(id, dto)
                .orElseThrow(() -> new ResourceNotFoundException("Pregunta frecuente no encontrada con ID: " + id));
        log.info("Pregunta frecuente actualizada ID: {}", id);
        return actualizada;
    }

    @Override
    public void eliminarPreguntaFrecuente(Long id) {
        if (!preguntaFrecuenteDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Pregunta frecuente no encontrada con ID: " + id);
        }
        log.info("Pregunta frecuente eliminada ID: {}", id);
    }
}
