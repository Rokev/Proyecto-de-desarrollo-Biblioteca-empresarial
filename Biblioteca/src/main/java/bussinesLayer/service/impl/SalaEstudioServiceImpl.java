package bussinesLayer.service.impl;

import bussinesLayer.dto.SalaEstudioDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.SalaEstudioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.SalaEstudioDAO;
import persistanceLayer.dao.SedeDAO;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SalaEstudioServiceImpl implements SalaEstudioService {

    private final SalaEstudioDAO salaEstudioDAO;
    private final SedeDAO sedeDAO;

    @Override
    public SalaEstudioDTO crearSalaEstudio(SalaEstudioDTO dto) {
        log.info("Creando sala de estudio: {}", dto.getNombre());

        validarSede(dto);
        if (salaEstudioDAO.existsBySedeIdAndNombreIgnoreCase(dto.getSedeId(), dto.getNombre())) {
            throw new BusinessValidationException(
                    "Ya existe una sala con el nombre '" + dto.getNombre() + "' en la sede " + dto.getSedeId());
        }
        if (dto.getActiva() == null) {
            dto.setActiva(true);
        }

        SalaEstudioDTO guardada = salaEstudioDAO.save(dto);
        log.info("Sala de estudio creada con ID: {}", guardada.getId());
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public SalaEstudioDTO obtenerSalaEstudioPorId(Long id) {
        return salaEstudioDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sala de estudio no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalaEstudioDTO> obtenerTodasLasSalasEstudio() {
        return salaEstudioDAO.findAll();
    }

    @Override
    public SalaEstudioDTO actualizarSalaEstudio(Long id, SalaEstudioDTO dto) {
        SalaEstudioDTO actual = salaEstudioDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sala de estudio no encontrada con ID: " + id));

        validarSede(dto);
        boolean cambioClave = !actual.getSedeId().equals(dto.getSedeId())
                || !actual.getNombre().equalsIgnoreCase(dto.getNombre());
        if (cambioClave && salaEstudioDAO.existsBySedeIdAndNombreIgnoreCase(dto.getSedeId(), dto.getNombre())) {
            throw new BusinessValidationException(
                    "Ya existe una sala con el nombre '" + dto.getNombre() + "' en la sede " + dto.getSedeId());
        }

        SalaEstudioDTO actualizada = salaEstudioDAO.update(id, dto)
                .orElseThrow(() -> new ResourceNotFoundException("Sala de estudio no encontrada con ID: " + id));
        log.info("Sala de estudio actualizada ID: {}", id);
        return actualizada;
    }

    @Override
    public void eliminarSalaEstudio(Long id) {
        if (!salaEstudioDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Sala de estudio no encontrada con ID: " + id);
        }
        log.info("Sala de estudio eliminada ID: {}", id);
    }

    private void validarSede(SalaEstudioDTO dto) {
        if (!sedeDAO.existsById(dto.getSedeId())) {
            throw new ResourceNotFoundException("Sede no encontrada con ID: " + dto.getSedeId());
        }
        if (dto.getCapacidad() < 1) {
            throw new BusinessValidationException("La capacidad debe ser al menos 1");
        }
    }
}
