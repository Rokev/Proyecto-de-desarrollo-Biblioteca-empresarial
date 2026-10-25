package bussinesLayer.service.impl;

import bussinesLayer.dto.SancionDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.SancionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.PrestamoDAO;
import persistanceLayer.dao.SancionDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SancionServiceImpl implements SancionService {

    private static final String ESTADO_ACTIVA = "ACTIVA";

    private final SancionDAO sancionDAO;
    private final UsuarioDAO usuarioDAO;
    private final PrestamoDAO prestamoDAO;

    @Override
    public SancionDTO crearSancion(SancionDTO dto) {
        log.info("Creando sancion {} para el usuario {}", dto.getTipo(), dto.getUsuarioId());

        validar(dto);

        if (sancionDAO.existsByUsuarioIdAndTipoIgnoreCaseAndEstadoIgnoreCase(
                dto.getUsuarioId(), dto.getTipo(), ESTADO_ACTIVA)) {
            throw new BusinessValidationException(
                    "El usuario ya tiene una sancion activa de tipo: " + dto.getTipo());
        }
        if (dto.getEstado() == null || dto.getEstado().isBlank()) {
            dto.setEstado(ESTADO_ACTIVA);
        }

        SancionDTO guardada = sancionDAO.save(dto);
        log.info("Sancion creada con ID: {}", guardada.getId());
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public SancionDTO obtenerSancionPorId(Long id) {
        return sancionDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sancion no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SancionDTO> obtenerTodasLasSanciones() {
        return sancionDAO.findAll();
    }

    @Override
    public SancionDTO actualizarSancion(Long id, SancionDTO dto) {
        if (!sancionDAO.existsById(id)) {
            throw new ResourceNotFoundException("Sancion no encontrada con ID: " + id);
        }

        validar(dto);

        SancionDTO actualizada = sancionDAO.update(id, dto)
                .orElseThrow(() -> new ResourceNotFoundException("Sancion no encontrada con ID: " + id));
        log.info("Sancion actualizada ID: {}", id);
        return actualizada;
    }

    @Override
    public void eliminarSancion(Long id) {
        if (!sancionDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Sancion no encontrada con ID: " + id);
        }
        log.info("Sancion eliminada ID: {}", id);
    }

    private void validar(SancionDTO dto) {
        if (!usuarioDAO.existsById(dto.getUsuarioId())) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + dto.getUsuarioId());
        }
        if (dto.getPrestamoId() != null && !prestamoDAO.existsById(dto.getPrestamoId())) {
            throw new ResourceNotFoundException("Prestamo no encontrado con ID: " + dto.getPrestamoId());
        }
        if (dto.getFechaFin() != null && !dto.getFechaFin().isAfter(dto.getFechaAplicacion())) {
            throw new BusinessValidationException("La fechaFin debe ser posterior a la fechaAplicacion");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SancionDTO> obtenerSancionesDeUsuario(Long usuarioId, String estado) {
        if (!usuarioDAO.existsById(usuarioId)) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId);
        }
        if (estado == null || estado.isBlank() || "TODAS".equalsIgnoreCase(estado)) {
            return sancionDAO.findByUsuarioId(usuarioId);
        }
        return sancionDAO.findByUsuarioIdAndEstadoIgnoreCase(usuarioId, estado);
    }
}
