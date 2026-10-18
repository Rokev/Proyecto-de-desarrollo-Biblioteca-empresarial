package bussinesLayer.service.impl;

import bussinesLayer.dto.RolDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.RolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.RolDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RolServiceImpl implements RolService {

    private final RolDAO rolDAO;
    private final UsuarioDAO usuarioDAO;

    @Override
    public RolDTO crearRol(RolDTO rolDTO) {
        log.info("Creando rol: {}", rolDTO.getNombre());

        if (rolDAO.existsByNombreIgnoreCase(rolDTO.getNombre())) {
            throw new BusinessValidationException("Ya existe un rol registrado con el nombre: " + rolDTO.getNombre());
        }

        RolDTO guardado = rolDAO.save(rolDTO);
        log.info("Rol creado con ID: {}", guardado.getId());
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public RolDTO obtenerRolPorId(Long id) {
        return rolDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolDTO> obtenerTodosLosRoles() {
        return rolDAO.findAll();
    }

    @Override
    public RolDTO actualizarRol(Long id, RolDTO rolDTO) {
        if (!rolDAO.existsById(id)) {
            throw new ResourceNotFoundException("Rol no encontrado con ID: " + id);
        }

        rolDAO.findByNombreIgnoreCase(rolDTO.getNombre())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new BusinessValidationException(
                            "Ya existe otro rol registrado con el nombre: " + rolDTO.getNombre());
                });

        RolDTO actualizado = rolDAO.update(id, rolDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con ID: " + id));
        log.info("Rol actualizado ID: {}", id);
        return actualizado;
    }

    @Override
    public void eliminarRol(Long id) {
        if (!rolDAO.existsById(id)) {
            throw new ResourceNotFoundException("Rol no encontrado con ID: " + id);
        }
        if (!usuarioDAO.findByRolId(id).isEmpty()) {
            throw new BusinessValidationException("No se puede eliminar el rol porque tiene usuarios asociados");
        }
        rolDAO.deleteById(id);
        log.info("Rol eliminado ID: {}", id);
    }
}
