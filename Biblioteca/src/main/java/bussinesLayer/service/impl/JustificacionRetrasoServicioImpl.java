package bussinesLayer.service.impl;

import bussinesLayer.dto.JustificacionRetrasoDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.JustificacionRetrasoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.JustificacionRetrasoDAO;
import persistanceLayer.dao.PrestamoDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
class JustificacionRetrasoServiceImpl implements JustificacionRetrasoService {

    private static final String ESTADO_INICIAL = "PENDIENTE";

    private final JustificacionRetrasoDAO justificacionRetrasoDAO;
    private final PrestamoDAO prestamoDAO;
    private final UsuarioDAO usuarioDAO;

    @Override
    public JustificacionRetrasoDTO crearJustificacionRetraso(JustificacionRetrasoDTO dto) {
        log.info("Creando justificacion de retraso para el prestamo: {}", dto.getPrestamoId());

        validarReferencias(dto);

        if (dto.getEstado() == null || dto.getEstado().isBlank()) {
            dto.setEstado(ESTADO_INICIAL);
        }

        JustificacionRetrasoDTO guardada = justificacionRetrasoDAO.save(dto);
        log.info("Justificacion creada con ID: {}", guardada.getId());
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public JustificacionRetrasoDTO obtenerJustificacionRetrasoPorId(Long id) {
        return justificacionRetrasoDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Justificacion no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JustificacionRetrasoDTO> obtenerTodasLasJustificacionesRetraso() {
        return justificacionRetrasoDAO.findAll();
    }

    @Override
    public JustificacionRetrasoDTO actualizarJustificacionRetraso(Long id, JustificacionRetrasoDTO dto) {
        if (!justificacionRetrasoDAO.existsById(id)) {
            throw new ResourceNotFoundException("Justificacion no encontrada con ID: " + id);
        }

        validarReferencias(dto);

        JustificacionRetrasoDTO actualizada = justificacionRetrasoDAO.update(id, dto)
                .orElseThrow(() -> new ResourceNotFoundException("Justificacion no encontrada con ID: " + id));
        log.info("Justificacion actualizada ID: {}", id);
        return actualizada;
    }

    @Override
    public void eliminarJustificacionRetraso(Long id) {
        if (!justificacionRetrasoDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Justificacion no encontrada con ID: " + id);
        }
        log.info("Justificacion eliminada ID: {}", id);
    }

    private void validarReferencias(JustificacionRetrasoDTO dto) {
        if (!prestamoDAO.existsById(dto.getPrestamoId())) {
            throw new ResourceNotFoundException("Prestamo no encontrado con ID: " + dto.getPrestamoId());
        }
        if (!usuarioDAO.existsById(dto.getUsuarioId())) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + dto.getUsuarioId());
        }
        if (dto.getBibliotecarioId() != null && !usuarioDAO.existsById(dto.getBibliotecarioId())) {
            throw new ResourceNotFoundException("Bibliotecario no encontrado con ID: " + dto.getBibliotecarioId());
        }
    }
}
