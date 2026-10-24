package bussinesLayer.service;

public interface ValidadorUsuarioService {
    /**
     * Verifica que el usuario no tenga bloqueos por sanciones ni multas pendientes.
     * Dispara BusinessValidationException si existe algún impedimento.
     */
    void validarUsuarioHabilitadoParaOperaciones(Long usuarioId);
}