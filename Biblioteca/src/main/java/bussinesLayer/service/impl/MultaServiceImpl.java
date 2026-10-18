package bussinesLayer.service.impl;

import bussinesLayer.dto.MultaDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.MultaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.MultaDAO;
import persistanceLayer.dao.PrestamoDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MultaServiceImpl implements MultaService {

    private static final String ESTADO_INICIAL = "PENDIENTE";

    private final MultaDAO multaDAO;
    private final UsuarioDAO usuarioDAO;
    private final PrestamoDAO prestamoDAO;

    @Override
    public MultaDTO crearMulta(MultaDTO multaDTO) {
        log.info("Creando multa para el usuario: {}", multaDTO.getUsuarioId());

        validarReferencias(multaDTO);

        if (multaDTO.getEstado() == null || multaDTO.getEstado().isBlank()) {
            multaDTO.setEstado(ESTADO_INICIAL);
        }

        MultaDTO guardada = multaDAO.save(multaDTO);
        log.info("Multa creada con ID: {}", guardada.getId());
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public MultaDTO obtenerMultaPorId(Long id) {
        return multaDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Multa no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MultaDTO> obtenerTodasLasMultas() {
        return multaDAO.findAll();
    }

    @Override
    public MultaDTO actualizarMulta(Long id, MultaDTO multaDTO) {
        if (!multaDAO.existsById(id)) {
            throw new ResourceNotFoundException("Multa no encontrada con ID: " + id);
        }

        validarReferencias(multaDTO);

        MultaDTO actualizada = multaDAO.update(id, multaDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Multa no encontrada con ID: " + id));
        log.info("Multa actualizada ID: {}", id);
        return actualizada;
    }

    @Override
    public void eliminarMulta(Long id) {
        if (!multaDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Multa no encontrada con ID: " + id);
        }
        log.info("Multa eliminada ID: {}", id);
    }

    private void validarReferencias(MultaDTO dto) {
        if (!usuarioDAO.existsById(dto.getUsuarioId())) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + dto.getUsuarioId());
        }
        if (dto.getPrestamoId() != null && !prestamoDAO.existsById(dto.getPrestamoId())) {
            throw new ResourceNotFoundException("Prestamo no encontrado con ID: " + dto.getPrestamoId());
        }
    }
}
