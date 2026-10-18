package bussinesLayer.service;

import bussinesLayer.dto.TokenRecuperacionDTO;

import java.util.List;

/**
 * Logica de negocio para la gestion de tokens de recuperacion de contrasena.
 */
public interface TokenRecuperacionService {

    TokenRecuperacionDTO crearTokenRecuperacion(TokenRecuperacionDTO tokenRecuperacionDTO);

    TokenRecuperacionDTO obtenerTokenRecuperacionPorId(Long id);

    List<TokenRecuperacionDTO> obtenerTodosLosTokensRecuperacion();

    TokenRecuperacionDTO actualizarTokenRecuperacion(Long id, TokenRecuperacionDTO tokenRecuperacionDTO);

    void eliminarTokenRecuperacion(Long id);
}
