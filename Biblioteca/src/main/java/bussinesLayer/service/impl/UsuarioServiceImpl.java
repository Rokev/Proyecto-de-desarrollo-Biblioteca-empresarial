package bussinesLayer.service.impl;

import bussinesLayer.dto.UsuarioDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.RolDAO;
import persistanceLayer.dao.SedeDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioDAO usuarioDAO;
    private final RolDAO rolDAO;
    private final SedeDAO sedeDAO;

    @Override
    public UsuarioDTO crearUsuario(UsuarioDTO dto) {
        log.info("Creando usuario con correo: {}", dto.getCorreo());

        if (usuarioDAO.existsByNombreUsuarioIgnoreCase(dto.getNombreUsuario())) {
            throw new BusinessValidationException(
                    "Ya existe un usuario registrado con el nombre de usuario: " + dto.getNombreUsuario());
        }
        if (usuarioDAO.existsByCorreoIgnoreCase(dto.getCorreo())) {
            throw new BusinessValidationException(
                    "Ya existe un usuario registrado con el correo: " + dto.getCorreo());
        }
        if (usuarioDAO.existsByDocumento(dto.getDocumento())) {
            throw new BusinessValidationException(
                    "Ya existe un usuario registrado con el documento: " + dto.getDocumento());
        }
        validarReferencias(dto);

        if (dto.getActivo() == null) {
            dto.setActivo(true);
        }
        if (dto.getFechaRegistro() == null) {
            dto.setFechaRegistro(LocalDateTime.now());
        }
        // TODO: cifrar dto.getContrasena() (p. ej. BCrypt con PasswordEncoder) antes de persistir.

        UsuarioDTO guardado = usuarioDAO.save(dto);
        log.info("Usuario creado con ID: {}", guardado.getId());
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO obtenerUsuarioPorId(Long id) {
        return usuarioDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioDTO> obtenerTodosLosUsuarios() {
        return usuarioDAO.findAll();
    }

    @Override
    public UsuarioDTO actualizarUsuario(Long id, UsuarioDTO dto) {
        if (!usuarioDAO.existsById(id)) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + id);
        }

        usuarioDAO.findByNombreUsuarioIgnoreCase(dto.getNombreUsuario())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new BusinessValidationException(
                            "Ya existe otro usuario con el nombre de usuario: " + dto.getNombreUsuario());
                });
        usuarioDAO.findByCorreoIgnoreCase(dto.getCorreo())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new BusinessValidationException("Ya existe otro usuario con el correo: " + dto.getCorreo());
                });
        usuarioDAO.findByDocumento(dto.getDocumento())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new BusinessValidationException("Ya existe otro usuario con el documento: " + dto.getDocumento());
                });
        validarReferencias(dto);

        UsuarioDTO actualizado = usuarioDAO.update(id, dto)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        log.info("Usuario actualizado ID: {}", id);
        return actualizado;
    }

    @Override
    public void eliminarUsuario(Long id) {
        if (!usuarioDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + id);
        }
        log.info("Usuario eliminado ID: {}", id);
    }

    private void validarReferencias(UsuarioDTO dto) {
        if (!rolDAO.existsById(dto.getRolId())) {
            throw new ResourceNotFoundException("Rol no encontrado con ID: " + dto.getRolId());
        }
        if (dto.getSedeId() != null && !sedeDAO.existsById(dto.getSedeId())) {
            throw new ResourceNotFoundException("Sede no encontrada con ID: " + dto.getSedeId());
        }
    }
}
