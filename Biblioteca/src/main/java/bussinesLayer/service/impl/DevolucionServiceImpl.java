package bussinesLayer.service.impl;

import bussinesLayer.dto.DevolucionDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.DevolucionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.DevolucionDAO;
import persistanceLayer.dao.PrestamoDAO;
import persistanceLayer.dao.SedeDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DevolucionServiceImpl implements DevolucionService {

    private static final String ESTADO_INICIAL = "PENDIENTE";

    private final DevolucionDAO devolucionDAO;
    private final PrestamoDAO prestamoDAO;
    private final SedeDAO sedeDAO;
    private final UsuarioDAO usuarioDAO;

    @Override
    public DevolucionDTO crearDevolucion(DevolucionDTO devolucionDTO) {
        log.info("Creando devolucion para el prestamo: {}", devolucionDTO.getPrestamoId());

        validarReferencias(devolucionDTO);

        if (devolucionDAO.existsByPrestamoId(devolucionDTO.getPrestamoId())) {
            throw new BusinessValidationException(
                    "Ya existe una devolucion registrada para el prestamo: " + devolucionDTO.getPrestamoId());
        }
        if (devolucionDAO.findByCodigoQr(devolucionDTO.getCodigoQr()).isPresent()) {
            throw new BusinessValidationException(
                    "Ya existe una devolucion con el codigo QR: " + devolucionDTO.getCodigoQr());
        }

        if (devolucionDTO.getEstado() == null || devolucionDTO.getEstado().isBlank()) {
            devolucionDTO.setEstado(ESTADO_INICIAL);
        }

        DevolucionDTO guardada = devolucionDAO.save(devolucionDTO);
        log.info("Devolucion creada con ID: {}", guardada.getId());
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public DevolucionDTO obtenerDevolucionPorId(Long id) {
        return devolucionDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Devolucion no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DevolucionDTO> obtenerTodasLasDevoluciones() {
        return devolucionDAO.findAll();
    }

    @Override
    public DevolucionDTO actualizarDevolucion(Long id, DevolucionDTO devolucionDTO) {
        if (!devolucionDAO.existsById(id)) {
            throw new ResourceNotFoundException("Devolucion no encontrada con ID: " + id);
        }

        validarReferencias(devolucionDTO);

        devolucionDAO.findByPrestamoId(devolucionDTO.getPrestamoId())
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new BusinessValidationException(
                            "Ya existe otra devolucion registrada para el prestamo: " + devolucionDTO.getPrestamoId());
                });
        devolucionDAO.findByCodigoQr(devolucionDTO.getCodigoQr())
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new BusinessValidationException(
                            "Ya existe otra devolucion con el codigo QR: " + devolucionDTO.getCodigoQr());
                });

        DevolucionDTO actualizada = devolucionDAO.update(id, devolucionDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Devolucion no encontrada con ID: " + id));
        log.info("Devolucion actualizada ID: {}", id);
        return actualizada;
    }

    @Override
    public void eliminarDevolucion(Long id) {
        if (!devolucionDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Devolucion no encontrada con ID: " + id);
        }
        log.info("Devolucion eliminada ID: {}", id);
    }

    private void validarReferencias(DevolucionDTO dto) {
        if (!prestamoDAO.existsById(dto.getPrestamoId())) {
            throw new ResourceNotFoundException("Prestamo no encontrado con ID: " + dto.getPrestamoId());
        }
        if (!sedeDAO.existsById(dto.getSedeDevolucionId())) {
            throw new ResourceNotFoundException("Sede no encontrada con ID: " + dto.getSedeDevolucionId());
        }
        if (dto.getBibliotecarioId() != null && !usuarioDAO.existsById(dto.getBibliotecarioId())) {
            throw new ResourceNotFoundException("Bibliotecario no encontrado con ID: " + dto.getBibliotecarioId());
        }
    }
}
