package bussinesLayer.service.impl;

import bussinesLayer.dto.AuthResponseDTO;
import bussinesLayer.dto.LoginRequestDTO;
import bussinesLayer.dto.RegistroDTO;
import bussinesLayer.dto.RestablecerContrasenaDTO;
import bussinesLayer.dto.SesionDTO;
import bussinesLayer.dto.TokenRecuperacionDTO;
import bussinesLayer.dto.UsuarioDTO;
import bussinesLayer.dto.CambiarContrasenaDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.security.JwtService;
import bussinesLayer.service.AuthService;
import bussinesLayer.service.UsuarioService;
import bussinesLayer.service.CorreoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.RolDAO;
import persistanceLayer.dao.SesionDAO;
import persistanceLayer.dao.TokenRecuperacionDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UsuarioDAO usuarioDAO;
    private final RolDAO rolDAO;
    private final SesionDAO sesionDAO;
    private final TokenRecuperacionDAO tokenRecuperacionDAO;
    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final CorreoService correoService;

    @Override
    public AuthResponseDTO login(LoginRequestDTO request) {
        UsuarioDTO usuario = usuarioDAO.findByCorreoIgnoreCase(request.getCorreo().trim())
                .orElseThrow(() -> new BadCredentialsException("Correo o contrasena incorrectos"));
        String hash = usuarioDAO.findContrasenaHashById(usuario.getId())
                .orElseThrow(() -> new BadCredentialsException("Correo o contrasena incorrectos"));

        if (!passwordEncoder.matches(request.getContrasena(), hash)) {
            throw new BadCredentialsException("Correo o contrasena incorrectos");
        }
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new DisabledException("La cuenta esta suspendida o desactivada");
        }

        String rol = nombreRol(usuario);
        JwtService.TokenEmitido emitido = jwtService.generar(usuario, rol);

        SesionDTO sesion = new SesionDTO();
        sesion.setToken(emitido.jti());
        sesion.setFechaInicio(emitido.inicio());
        sesion.setFechaExpiracion(emitido.expiracion());
        sesion.setActiva(true);
        sesion.setUsuarioId(usuario.getId());
        sesionDAO.save(sesion);

        log.info("Inicio de sesion del usuario ID: {}", usuario.getId());
        return new AuthResponseDTO(emitido.token(), "Bearer", emitido.expiracion(),
                usuario.getId(), usuario.getNombre(), rol);
    }

    @Override
    public UsuarioDTO registrar(RegistroDTO registro) {
        var rolCliente = rolDAO.findByNombreIgnoreCase("CLIENTE")
                .orElseThrow(() -> new ResourceNotFoundException("El rol CLIENTE no existe"));

        UsuarioDTO dto = new UsuarioDTO();
        dto.setNombre(registro.getNombre());
        dto.setApellido(registro.getApellido());
        dto.setDocumento(registro.getDocumento());
        dto.setNombreUsuario(registro.getNombreUsuario());
        dto.setCorreo(registro.getCorreo());
        dto.setContrasena(registro.getContrasena());
        dto.setTelefono(registro.getTelefono());
        dto.setDireccion(registro.getDireccion());
        dto.setRolId(rolCliente.getId());
        dto.setActivo(true);
        return usuarioService.crearUsuario(dto); // valida duplicados y cifra la contrasena
    }

    @Override
    public void logout(String jti) {
        sesionDAO.cerrarSesion(jti, LocalDateTime.now());
    }

    @Override
    public void solicitarRecuperacion(String correo) {
        // Siempre responde igual exista o no el correo (no revela que correos estan registrados)
        usuarioDAO.findByCorreoIgnoreCase(correo.trim()).ifPresent(usuario -> {
            LocalDateTime ahora = LocalDateTime.now();
            TokenRecuperacionDTO token = new TokenRecuperacionDTO();
            token.setToken(UUID.randomUUID().toString());
            token.setFechaCreacion(ahora);
            token.setFechaExpiracion(ahora.plusHours(24));
            token.setUsado(false);
            token.setUsuarioId(usuario.getId());

            tokenRecuperacionDAO.invalidarTokensDeUsuario(usuario.getId()); // solo uno vigente
            tokenRecuperacionDAO.save(token);
            correoService.enviarRecuperacionContrasena(usuario.getCorreo(), token.getToken());
        });
    }

    @Override
    public void restablecerContrasena(RestablecerContrasenaDTO dto) {
        TokenRecuperacionDTO token = tokenRecuperacionDAO.findByTokenAndUsadoFalse(dto.getToken())
                .orElseThrow(() -> new BusinessValidationException("El token de recuperacion no es valido o ya fue usado"));

        // Marca el token como usado solo si sigue vigente (operacion atomica)
        if (tokenRecuperacionDAO.consumirToken(dto.getToken(), LocalDateTime.now()) == 0) {
            throw new BusinessValidationException("El token de recuperacion expiro");
        }

        usuarioDAO.updateContrasena(token.getUsuarioId(), passwordEncoder.encode(dto.getNuevaContrasena()));
        sesionDAO.cerrarSesionesDeUsuario(token.getUsuarioId(), LocalDateTime.now());
        log.info("Contrasena restablecida para el usuario ID: {}", token.getUsuarioId());
    }

    @Override
    public void cambiarContrasena(Long usuarioId, CambiarContrasenaDTO dto) {
        String hash = usuarioDAO.findContrasenaHashById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        if (!passwordEncoder.matches(dto.getContrasenaActual(), hash)) {
            throw new BusinessValidationException("La contrasena actual es incorrecta");
        }
        if (dto.getContrasenaActual().equals(dto.getNuevaContrasena())) {
            throw new BusinessValidationException("La nueva contrasena debe ser diferente a la actual");
        }

        usuarioDAO.updateContrasena(usuarioId, passwordEncoder.encode(dto.getNuevaContrasena()));
        sesionDAO.cerrarSesionesDeUsuario(usuarioId, LocalDateTime.now()); // obliga a iniciar sesion de nuevo
        log.info("Contrasena cambiada por el usuario ID: {}", usuarioId);
    }    

    private String nombreRol(UsuarioDTO usuario) {
        return rolDAO.findById(usuario.getRolId())
                .map(r -> r.getNombre().trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con ID: " + usuario.getRolId()));
    }
}