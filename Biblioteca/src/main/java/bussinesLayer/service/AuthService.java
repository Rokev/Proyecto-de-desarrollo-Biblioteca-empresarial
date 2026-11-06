package bussinesLayer.service;

import bussinesLayer.dto.AuthResponseDTO;
import bussinesLayer.dto.LoginRequestDTO;
import bussinesLayer.dto.RegistroDTO;
import bussinesLayer.dto.RestablecerContrasenaDTO;
import bussinesLayer.dto.UsuarioDTO;

/** Inicio de sesion, registro de clientes, cierre de sesion y recuperacion de contrasena. */
public interface AuthService {

    AuthResponseDTO login(LoginRequestDTO request);

    UsuarioDTO registrar(RegistroDTO registro);

    void logout(String jti);

    void solicitarRecuperacion(String correo);

    void restablecerContrasena(RestablecerContrasenaDTO dto);
}