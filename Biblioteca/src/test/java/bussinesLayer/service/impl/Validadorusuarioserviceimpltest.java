package bussinesLayer.service.impl;

import bussinesLayer.dto.UsuarioDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.MultaDAO;
import persistanceLayer.dao.SancionDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidadorUsuarioServiceImpl")
class ValidadorUsuarioServiceImplTest {

    private static final Long USUARIO_ID = 10L;

    @Mock private UsuarioDAO usuarioDAO;
    @Mock private SancionDAO sancionDAO;
    @Mock private MultaDAO multaDAO;

    @InjectMocks
    private ValidadorUsuarioServiceImpl service;

    private UsuarioDTO usuario(Boolean activo) {
        UsuarioDTO u = new UsuarioDTO();
        u.setId(USUARIO_ID);
        u.setActivo(activo);
        return u;
    }

    @Test
    @DisplayName("usuario inexistente lanza ResourceNotFoundException")
    void validar_usuarioInexistente_lanzaNotFound() {
        when(usuarioDAO.findById(USUARIO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.validarUsuarioHabilitadoParaOperaciones(USUARIO_ID));

        verifyNoInteractions(sancionDAO, multaDAO);
    }

    @Test
    @DisplayName("cuenta suspendida o desactivada (activo=false) lanza BusinessValidationException")
    void validar_cuentaInactiva_lanzaBusinessValidation() {
        when(usuarioDAO.findById(USUARIO_ID)).thenReturn(Optional.of(usuario(false)));

        assertThrows(BusinessValidationException.class,
                () -> service.validarUsuarioHabilitadoParaOperaciones(USUARIO_ID));

        verifyNoInteractions(sancionDAO, multaDAO);
    }

    @Test
    @DisplayName("cuenta con activo nulo se trata como inactiva y lanza BusinessValidationException")
    void validar_activoNulo_lanzaBusinessValidation() {
        when(usuarioDAO.findById(USUARIO_ID)).thenReturn(Optional.of(usuario(null)));

        assertThrows(BusinessValidationException.class,
                () -> service.validarUsuarioHabilitadoParaOperaciones(USUARIO_ID));

        verifyNoInteractions(sancionDAO, multaDAO);
    }

    @Test
    @DisplayName("sancion activa BLOQUEO_PRESTAMOS lanza BusinessValidationException")
    void validar_bloqueoPrestamos_lanzaBusinessValidation() {
        when(usuarioDAO.findById(USUARIO_ID)).thenReturn(Optional.of(usuario(true)));
        when(sancionDAO.existsByUsuarioIdAndTipoIgnoreCaseAndEstadoIgnoreCase(
                USUARIO_ID, "BLOQUEO_PRESTAMOS", "ACTIVA")).thenReturn(true);

        assertThrows(BusinessValidationException.class,
                () -> service.validarUsuarioHabilitadoParaOperaciones(USUARIO_ID));

        verify(multaDAO, never()).totalPendientePorUsuario(any());
    }

    @Test
    @DisplayName("sancion activa SUSPENSION_CUENTA lanza BusinessValidationException")
    void validar_suspensionCuenta_lanzaBusinessValidation() {
        when(usuarioDAO.findById(USUARIO_ID)).thenReturn(Optional.of(usuario(true)));
        when(sancionDAO.existsByUsuarioIdAndTipoIgnoreCaseAndEstadoIgnoreCase(
                USUARIO_ID, "BLOQUEO_PRESTAMOS", "ACTIVA")).thenReturn(false);
        when(sancionDAO.existsByUsuarioIdAndTipoIgnoreCaseAndEstadoIgnoreCase(
                USUARIO_ID, "SUSPENSION_CUENTA", "ACTIVA")).thenReturn(true);

        assertThrows(BusinessValidationException.class,
                () -> service.validarUsuarioHabilitadoParaOperaciones(USUARIO_ID));

        verify(multaDAO, never()).totalPendientePorUsuario(any());
    }

    @Test
    @DisplayName("multas pendientes mayores a cero lanza BusinessValidationException")
    void validar_multasPendientes_lanzaBusinessValidation() {
        when(usuarioDAO.findById(USUARIO_ID)).thenReturn(Optional.of(usuario(true)));
        when(multaDAO.totalPendientePorUsuario(USUARIO_ID)).thenReturn(new BigDecimal("5000"));

        assertThrows(BusinessValidationException.class,
                () -> service.validarUsuarioHabilitadoParaOperaciones(USUARIO_ID));
    }

    @Test
    @DisplayName("cuenta activa, sin sanciones activas y sin multas pendientes no lanza excepcion")
    void validar_usuarioHabilitado_noLanza() {
        when(usuarioDAO.findById(USUARIO_ID)).thenReturn(Optional.of(usuario(true)));
        when(multaDAO.totalPendientePorUsuario(USUARIO_ID)).thenReturn(BigDecimal.ZERO);

        assertDoesNotThrow(() -> service.validarUsuarioHabilitadoParaOperaciones(USUARIO_ID));

        verify(multaDAO).totalPendientePorUsuario(USUARIO_ID);
    }
}