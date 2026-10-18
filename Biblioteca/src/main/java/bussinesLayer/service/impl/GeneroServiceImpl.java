package bussinesLayer.service.impl;

import bussinesLayer.dto.GeneroDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.GeneroService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.GeneroDAO;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GeneroServiceImpl implements GeneroService {

    private final GeneroDAO generoDAO;

    @Override
    public GeneroDTO crearGenero(GeneroDTO generoDTO) {
        log.info("Creando genero: {}", generoDTO.getNombre());

        if (generoDAO.existsByNombreIgnoreCase(generoDTO.getNombre())) {
            throw new BusinessValidationException(
                    "Ya existe un genero registrado con el nombre: " + generoDTO.getNombre());
        }

        GeneroDTO guardado = generoDAO.save(generoDTO);
        log.info("Genero creado con ID: {}", guardado.getId());
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public GeneroDTO obtenerGeneroPorId(Long id) {
        return generoDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Genero no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeneroDTO> obtenerTodosLosGeneros() {
        return generoDAO.findAll();
    }

    @Override
    public GeneroDTO actualizarGenero(Long id, GeneroDTO generoDTO) {
        if (!generoDAO.existsById(id)) {
            throw new ResourceNotFoundException("Genero no encontrado con ID: " + id);
        }

        generoDAO.findByNombreIgnoreCase(generoDTO.getNombre())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new BusinessValidationException(
                            "Ya existe otro genero registrado con el nombre: " + generoDTO.getNombre());
                });

        GeneroDTO actualizado = generoDAO.update(id, generoDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Genero no encontrado con ID: " + id));
        log.info("Genero actualizado ID: {}", id);
        return actualizado;
    }

    @Override
    public void eliminarGenero(Long id) {
        if (!generoDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Genero no encontrado con ID: " + id);
        }
        log.info("Genero eliminado ID: {}", id);
    }
}
