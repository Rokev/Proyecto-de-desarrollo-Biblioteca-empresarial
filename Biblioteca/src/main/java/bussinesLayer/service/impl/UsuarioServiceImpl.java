package bussinesLayer.service.impl;
import java.util.Set;
import persistanceLayer.dao.NotificacionDAO;
import bussinesLayer.dto.NotificacionDTO;
import bussinesLayer.service.SuscripcionService;

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
    private final SuscripcionService suscripcionService;
    private final NotificacionDAO notificacionDAO;

    private static final Set<String> ROLES_PERSONAL = Set.of("BIBLIOTECARIO", "REPARTIDOR");
    private static final Set<String> ROLES_INTERNOS = Set.of("BIBLIOTECARIO", "REPARTIDOR", "ADMINISTRADOR", "ADMIN");

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

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioDTO> filtrarUsuarios(String rol, Long sedeId, Boolean activo, Boolean conSuscripcion) {
        String rolFiltro = (rol == null || rol.isBlank()) ? null : rol.trim();
        return usuarioDAO.filtrar(rolFiltro, sedeId, activo).stream()
                .filter(u -> conSuscripcion == null
                        || suscripcionService.tieneSuscripcionVigente(u.getId()) == conSuscripcion)
                .toList();
    }

    @Override
    public UsuarioDTO desactivarPersonal(Long id) {
        UsuarioDTO usuario = buscarUsuario(id);
        if (!ROLES_PERSONAL.contains(nombreRol(usuario))) {
            throw new BusinessValidationException("Solo se pueden desactivar cuentas de bibliotecarios y repartidores");
        }
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BusinessValidationException("La cuenta ya esta desactivada");
        }
        return cambiarEstadoCuenta(usuario, false);
    }

    @Override
    public UsuarioDTO suspenderUsuario(Long id) {
        UsuarioDTO usuario = buscarUsuario(id);
        if (ROLES_INTERNOS.contains(nombreRol(usuario))) {
            throw new BusinessValidationException(
                    "Solo se pueden suspender cuentas de usuarios; para bibliotecarios y repartidores use desactivar");
        }
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BusinessValidationException("La cuenta ya esta suspendida");
        }
        UsuarioDTO suspendido = cambiarEstadoCuenta(usuario, false);
        notificar(id, "CUENTA_SUSPENDIDA", "Tu cuenta fue suspendida",
                "Tu cuenta fue suspendida por la administracion. Comunicate con la biblioteca para mas informacion.");
        return suspendido;
    }

    @Override
    public UsuarioDTO reactivarCuenta(Long id) {
        UsuarioDTO usuario = buscarUsuario(id);
        if (Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BusinessValidationException("La cuenta ya esta activa");
        }
        UsuarioDTO reactivado = cambiarEstadoCuenta(usuario, true);
        notificar(id, "CUENTA_REACTIVADA", "Tu cuenta fue reactivada", "Tu cuenta fue reactivada y ya puedes usarla de nuevo.");
        return reactivado;
    }

    private UsuarioDTO buscarUsuario(Long id) {
        return usuarioDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
    }

    private String nombreRol(UsuarioDTO usuario) {
        return rolDAO.findById(usuario.getRolId()).map(r -> r.getNombre()).orElse("").trim().toUpperCase();
    }

    private UsuarioDTO cambiarEstadoCuenta(UsuarioDTO usuario, boolean activo) {
        usuario.setActivo(activo);
        return usuarioDAO.update(usuario.getId(), usuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuario.getId()));
    }

    private void notificar(Long usuarioId, String tipo, String titulo, String mensaje) {
        NotificacionDTO n = new NotificacionDTO();
        n.setUsuarioId(usuarioId);
        n.setTipo(tipo);
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setCanal("APP");
        n.setFechaCreacion(LocalDateTime.now());
        n.setLeida(false);
        notificacionDAO.save(n);
    }
}
