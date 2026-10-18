package bussinesLayer.service.impl;

import bussinesLayer.dto.SesionDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.SesionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.SesionDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SesionServiceImpl implements SesionService {

    private final SesionDAO sesionDAO;
    private final UsuarioDAO usuarioDAO;

    @Override
    public SesionDTO crearSesion(SesionDTO dto) {
        log.info("Creando sesion para el usuario: {}", dto.getUsuarioId());

        if (sesionDAO.findByToken(dto.getToken()).isPresent()) {
            throw new BusinessValidationException("Ya existe una sesion con el token indicado");
        }
        validar(dto);

        if (dto.getActiva() == null) {
            dto.setActiva(true);
        }

        SesionDTO guardada = sesionDAO.save(dto);
        log.info("Sesion creada con ID: {}", guardada.getId());
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public SesionDTO obtenerSesionPorId(Long id) {
        return sesionDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sesion no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SesionDTO> obtenerTodasLasSesiones() {
        return sesionDAO.findAll();
    }

    @Override
    public SesionDTO actualizarSesion(Long id, SesionDTO dto) {
        if (!sesionDAO.existsById(id)) {
            throw new ResourceNotFoundException("Sesion no encontrada con ID: " + id);
        }

        sesionDAO.findByToken(dto.getToken())
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new BusinessValidationException("Ya existe otra sesion con el token indicado");
                });
        validar(dto);

        SesionDTO actualizada = sesionDAO.update(id, dto)
                .orElseThrow(() -> new ResourceNotFoundException("Sesion no encontrada con ID: " + id));
        log.info("Sesion actualizada ID: {}", id);
        return actualizada;
    }

    @Override
    public void eliminarSesion(Long id) {
        if (!sesionDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Sesion no encontrada con ID: " + id);
        }
        log.info("Sesion eliminada ID: {}", id);
    }

    private void validar(SesionDTO dto) {
        if (!usuarioDAO.existsById(dto.getUsuarioId())) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + dto.getUsuarioId());
        }
        if (!dto.getFechaExpiracion().isAfter(dto.getFechaInicio())) {
            throw new BusinessValidationException("La fechaExpiracion debe ser posterior a la fechaInicio");
        }
    }
}
