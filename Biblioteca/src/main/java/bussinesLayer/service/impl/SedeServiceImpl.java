package bussinesLayer.service.impl;

import bussinesLayer.dto.SedeDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.SedeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.EjemplarDAO;
import persistanceLayer.dao.SedeDAO;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SedeServiceImpl implements SedeService {

    private final SedeDAO sedeDAO;
    private final EjemplarDAO ejemplarDAO;

    @Override
    public SedeDTO crearSede(SedeDTO sedeDTO) {
        log.info("Creando sede: {}", sedeDTO.getNombre());

        if (sedeDAO.existsByNombreIgnoreCase(sedeDTO.getNombre())) {
            throw new BusinessValidationException("Ya existe una sede registrada con el nombre: " + sedeDTO.getNombre());
        }
        validarSede(sedeDTO);

        if (sedeDTO.getAliada() == null) {
            sedeDTO.setAliada(false);
        }
        if (sedeDTO.getEnOperacion() == null) {
            sedeDTO.setEnOperacion(true);
        }

        SedeDTO guardada = sedeDAO.save(sedeDTO);
        log.info("Sede creada con ID: {}", guardada.getId());
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public SedeDTO obtenerSedePorId(Long id) {
        return sedeDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SedeDTO> obtenerTodasLasSedes() {
        return sedeDAO.findAll();
    }

    @Override
    public SedeDTO actualizarSede(Long id, SedeDTO sedeDTO) {
        if (!sedeDAO.existsById(id)) {
            throw new ResourceNotFoundException("Sede no encontrada con ID: " + id);
        }

        sedeDAO.findByNombreIgnoreCase(sedeDTO.getNombre())
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new BusinessValidationException(
                            "Ya existe otra sede registrada con el nombre: " + sedeDTO.getNombre());
                });
        validarSede(sedeDTO);

        SedeDTO actualizada = sedeDAO.update(id, sedeDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada con ID: " + id));
        log.info("Sede actualizada ID: {}", id);
        return actualizada;
    }

    @Override
    public void eliminarSede(Long id) {
        if (!sedeDAO.existsById(id)) {
            throw new ResourceNotFoundException("Sede no encontrada con ID: " + id);
        }
        if (ejemplarDAO.existsBySedeId(id)) {
            throw new BusinessValidationException("No se puede eliminar la sede porque tiene ejemplares asociados");
        }
        sedeDAO.deleteById(id);
        log.info("Sede eliminada ID: {}", id);
    }

    private void validarSede(SedeDTO dto) {
        if (dto.getHoraApertura() != null && dto.getHoraCierre() != null
                && !dto.getHoraCierre().isAfter(dto.getHoraApertura())) {
            throw new BusinessValidationException("La horaCierre debe ser posterior a la horaApertura");
        }
        if ((dto.getLatitud() == null) != (dto.getLongitud() == null)) {
            throw new BusinessValidationException("Debe indicar latitud y longitud juntas o ninguna");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SedeDTO> obtenerSedesActivas() {
        return sedeDAO.findByEnOperacionTrue();
    }
}
