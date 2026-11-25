package bussinesLayer.service;

import bussinesLayer.dto.AuthResponseDTO;
import bussinesLayer.dto.LoginRequestDTO;
import bussinesLayer.dto.RegistroDTO;
import bussinesLayer.dto.RestablecerContrasenaDTO;
import bussinesLayer.dto.UsuarioDTO;
import bussinesLayer.dto.CambiarContrasenaDTO;

/** Inicio de sesion, registro de clientes, cierre de sesion y recuperacion de contrasena. */
public interface AuthService {

    AuthResponseDTO login(LoginRequestDTO request);

    UsuarioDTO registrar(RegistroDTO registro);

    void logout(String jti);

    /** Genera el token de recuperación y lo devuelve (para mostrarlo en modo simulación). */
    String solicitarRecuperacion(String correo);

    void restablecerContrasena(RestablecerContrasenaDTO dto);

    void cambiarContrasena(Long usuarioId, CambiarContrasenaDTO dto);
}