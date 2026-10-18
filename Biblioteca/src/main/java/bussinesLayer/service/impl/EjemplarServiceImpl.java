package bussinesLayer.service.impl;

import bussinesLayer.dto.EjemplarDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.EjemplarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.EjemplarDAO;
import persistanceLayer.dao.LibroDAO;
import persistanceLayer.dao.SedeDAO;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class EjemplarServiceImpl implements EjemplarService {

    private static final String ESTADO_INICIAL = "DISPONIBLE";

    private final EjemplarDAO ejemplarDAO;
    private final LibroDAO libroDAO;
    private final SedeDAO sedeDAO;

    @Override
    public EjemplarDTO crearEjemplar(EjemplarDTO ejemplarDTO) {
        log.info("Creando ejemplar con codigo de barras: {}", ejemplarDTO.getCodigoBarras());

        if (ejemplarDAO.existsByCodigoBarras(ejemplarDTO.getCodigoBarras())) {
            throw new BusinessValidationException(
                    "Ya existe un ejemplar registrado con el codigo de barras: " + ejemplarDTO.getCodigoBarras());
        }
        validarReferencias(ejemplarDTO);

        if (ejemplarDTO.getEstado() == null || ejemplarDTO.getEstado().isBlank()) {
            ejemplarDTO.setEstado(ESTADO_INICIAL);
        }

        EjemplarDTO guardado = ejemplarDAO.save(ejemplarDTO);
        log.info("Ejemplar creado con ID: {}", guardado.getId());
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public EjemplarDTO obtenerEjemplarPorId(Long id) {
        return ejemplarDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ejemplar no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EjemplarDTO> obtenerTodosLosEjemplares() {
        return ejemplarDAO.findAll();
    }

    @Override
    public EjemplarDTO actualizarEjemplar(Long id, EjemplarDTO ejemplarDTO) {
        if (!ejemplarDAO.existsById(id)) {
            throw new ResourceNotFoundException("Ejemplar no encontrado con ID: " + id);
        }

        ejemplarDAO.findByCodigoBarras(ejemplarDTO.getCodigoBarras())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new BusinessValidationException(
                            "Ya existe otro ejemplar registrado con el codigo de barras: " + ejemplarDTO.getCodigoBarras());
                });
        validarReferencias(ejemplarDTO);

        EjemplarDTO actualizado = ejemplarDAO.update(id, ejemplarDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Ejemplar no encontrado con ID: " + id));
        log.info("Ejemplar actualizado ID: {}", id);
        return actualizado;
    }

    @Override
    public void eliminarEjemplar(Long id) {
        if (!ejemplarDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Ejemplar no encontrado con ID: " + id);
        }
        log.info("Ejemplar eliminado ID: {}", id);
    }

    private void validarReferencias(EjemplarDTO dto) {
        if (!libroDAO.existsById(dto.getLibroId())) {
            throw new ResourceNotFoundException("Libro no encontrado con ID: " + dto.getLibroId());
        }
        if (!sedeDAO.existsById(dto.getSedeId())) {
            throw new ResourceNotFoundException("Sede no encontrada con ID: " + dto.getSedeId());
        }
    }
}
