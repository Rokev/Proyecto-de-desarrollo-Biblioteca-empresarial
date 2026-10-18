package bussinesLayer.service.impl;

import bussinesLayer.dto.AutorDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.AutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.AutorDAO;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AutorServiceImpl implements AutorService {

    private final AutorDAO autorDAO;

    @Override
    public AutorDTO crearAutor(AutorDTO autorDTO) {
        log.info("Creando autor: {}", autorDTO.getNombre());

        if (autorDAO.existsByNombreIgnoreCase(autorDTO.getNombre())) {
            throw new BusinessValidationException(
                    "Ya existe un autor registrado con el nombre: " + autorDTO.getNombre());
        }

        AutorDTO guardado = autorDAO.save(autorDTO);
        log.info("Autor creado con ID: {}", guardado.getId());
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public AutorDTO obtenerAutorPorId(Long id) {
        return autorDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Autor no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutorDTO> obtenerTodosLosAutores() {
        return autorDAO.findAll();
    }

    @Override
    public AutorDTO actualizarAutor(Long id, AutorDTO autorDTO) {
        AutorDTO actual = autorDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Autor no encontrado con ID: " + id));

        boolean cambioNombre = !actual.getNombre().equalsIgnoreCase(autorDTO.getNombre());
        if (cambioNombre && autorDAO.existsByNombreIgnoreCase(autorDTO.getNombre())) {
            throw new BusinessValidationException(
                    "Ya existe otro autor registrado con el nombre: " + autorDTO.getNombre());
        }

        AutorDTO actualizado = autorDAO.update(id, autorDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Autor no encontrado con ID: " + id));
        log.info("Autor actualizado ID: {}", id);
        return actualizado;
    }

    @Override
    public void eliminarAutor(Long id) {
        if (!autorDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Autor no encontrado con ID: " + id);
        }
        log.info("Autor eliminado ID: {}", id);
    }
}
