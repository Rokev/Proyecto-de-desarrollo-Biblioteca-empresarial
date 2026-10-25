package bussinesLayer.service.impl;

import bussinesLayer.dto.TokenRecuperacionDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.TokenRecuperacionDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenRecuperacionServiceImpl")
class TokenRecuperacionServiceImplTest {

    private static final Long TOKEN_ID = 1L;
    private static final Long OTRO_ID = 2L;
    private static final Long USUARIO_ID = 10L;
    private static final String TOKEN = "tok-xyz-789";

    @Mock private TokenRecuperacionDAO tokenRecuperacionDAO;
    @Mock private UsuarioDAO usuarioDAO;

    @InjectMocks
    private TokenRecuperacionServiceImpl service;

    private TokenRecuperacionDTO dto;

    @BeforeEach
    void setUp() {
        LocalDateTime creacion = LocalDateTime.of(2026, 2, 10, 9, 0);
        dto = new TokenRecuperacionDTO();
        dto.setToken(TOKEN);
        dto.setFechaCreacion(creacion);
        dto.setFechaExpiracion(creacion.plusMinutes(30));
        dto.setUsuarioId(USUARIO_ID);
    }

    private TokenRecuperacionDTO tokenConId(Long id) {
        TokenRecuperacionDTO t = new TokenRecuperacionDTO();
        t.setId(id);
        return t;
    }

    // ---------------------------------------------------------------- crearTokenRecuperacion

    @Test
    @DisplayName("crearTokenRecuperacion: invalida los tokens anteriores, guarda con usado=false y devuelve el token")
    void crearTokenRecuperacion_valido_invalidaAnterioresYGuarda() {
        dto.setUsado(true);
        when(tokenRecuperacionDAO.findByToken(TOKEN)).thenReturn(Optional.empty());
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(tokenRecuperacionDAO.save(dto)).thenAnswer(inv -> {
            TokenRecuperacionDTO t = inv.getArgument(0);
            t.setId(TOKEN_ID);
            return t;
        });

        TokenRecuperacionDTO resultado = service.crearTokenRecuperacion(dto);

        assertEquals(TOKEN_ID, resultado.getId());
        assertFalse(resultado.getUsado());
        InOrder orden = inOrder(tokenRecuperacionDAO);
        orden.verify(tokenRecuperacionDAO).invalidarTokensDeUsuario(USUARIO_ID);
        orden.verify(tokenRecuperacionDAO).save(dto);
    }

    @Test
    @DisplayName("crearTokenRecuperacion: token duplicado lanza BusinessValidationException")
    void crearTokenRecuperacion_tokenDuplicado_lanzaBusinessValidation() {
        when(tokenRecuperacionDAO.findByToken(TOKEN)).thenReturn(Optional.of(tokenConId(OTRO_ID)));

        assertThrows(BusinessValidationException.class, () -> service.crearTokenRecuperacion(dto));

        verify(tokenRecuperacionDAO, never()).save(any());
        verify(tokenRecuperacionDAO, never()).invalidarTokensDeUsuario(any());
    }

    @Test
    @DisplayName("crearTokenRecuperacion: usuario inexistente lanza ResourceNotFoundException")
    void crearTokenRecuperacion_usuarioInexistente_lanzaNotFound() {
        when(tokenRecuperacionDAO.findByToken(TOKEN)).thenReturn(Optional.empty());
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearTokenRecuperacion(dto));

        verify(tokenRecuperacionDAO, never()).save(any());
        verify(tokenRecuperacionDAO, never()).invalidarTokensDeUsuario(any());
    }

    @Test
    @DisplayName("crearTokenRecuperacion: fechaExpiracion igual a fechaCreacion lanza BusinessValidationException")
    void crearTokenRecuperacion_expiracionIgual_lanzaBusinessValidation() {
        dto.setFechaExpiracion(dto.getFechaCreacion());
        when(tokenRecuperacionDAO.findByToken(TOKEN)).thenReturn(Optional.empty());
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearTokenRecuperacion(dto));

        verify(tokenRecuperacionDAO, never()).save(any());
        verify(tokenRecuperacionDAO, never()).invalidarTokensDeUsuario(any());
    }

    @Test
    @DisplayName("crearTokenRecuperacion: fechaExpiracion anterior a fechaCreacion lanza BusinessValidationException")
    void crearTokenRecuperacion_expiracionAnterior_lanzaBusinessValidation() {
        dto.setFechaExpiracion(dto.getFechaCreacion().minusMinutes(5));
        when(tokenRecuperacionDAO.findByToken(TOKEN)).thenReturn(Optional.empty());
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearTokenRecuperacion(dto));

        verify(tokenRecuperacionDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerTokenRecuperacionPorId: devuelve el token cuando existe")
    void obtenerTokenRecuperacionPorId_existente_loDevuelve() {
        TokenRecuperacionDTO existente = tokenConId(TOKEN_ID);
        when(tokenRecuperacionDAO.findById(TOKEN_ID)).thenReturn(Optional.of(existente));

        assertSame(existente, service.obtenerTokenRecuperacionPorId(TOKEN_ID));
    }

    @Test
    @DisplayName("obtenerTokenRecuperacionPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerTokenRecuperacionPorId_inexistente_lanzaNotFound() {
        when(tokenRecuperacionDAO.findById(TOKEN_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerTokenRecuperacionPorId(TOKEN_ID));
    }

    @Test
    @DisplayName("obtenerTodosLosTokensRecuperacion: delega en el DAO")
    void obtenerTodosLosTokensRecuperacion_devuelveLista() {
        when(tokenRecuperacionDAO.findAll()).thenReturn(List.of(tokenConId(1L), tokenConId(2L)));

        assertEquals(2, service.obtenerTodosLosTokensRecuperacion().size());
    }

    // ---------------------------------------------------------------- actualizarTokenRecuperacion

    @Test
    @DisplayName("actualizarTokenRecuperacion: token inexistente lanza ResourceNotFoundException")
    void actualizarTokenRecuperacion_inexistente_lanzaNotFound() {
        when(tokenRecuperacionDAO.existsById(TOKEN_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarTokenRecuperacion(TOKEN_ID, dto));

        verify(tokenRecuperacionDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarTokenRecuperacion: valor propio o libre actualiza sin conflicto")
    void actualizarTokenRecuperacion_valido_actualiza() {
        when(tokenRecuperacionDAO.existsById(TOKEN_ID)).thenReturn(true);
        // El mismo registro aparece como "dueño" del valor: no es conflicto.
        when(tokenRecuperacionDAO.findByToken(TOKEN)).thenReturn(Optional.of(tokenConId(TOKEN_ID)));
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(tokenRecuperacionDAO.update(TOKEN_ID, dto)).thenReturn(Optional.of(dto));

        assertSame(dto, service.actualizarTokenRecuperacion(TOKEN_ID, dto));
    }

    @Test
    @DisplayName("actualizarTokenRecuperacion: valor de otro token lanza BusinessValidationException")
    void actualizarTokenRecuperacion_valorDeOtro_lanzaBusinessValidation() {
        when(tokenRecuperacionDAO.existsById(TOKEN_ID)).thenReturn(true);
        when(tokenRecuperacionDAO.findByToken(TOKEN)).thenReturn(Optional.of(tokenConId(OTRO_ID)));

        assertThrows(BusinessValidationException.class, () -> service.actualizarTokenRecuperacion(TOKEN_ID, dto));

        verify(tokenRecuperacionDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarTokenRecuperacion: usuario inexistente lanza ResourceNotFoundException")
    void actualizarTokenRecuperacion_usuarioInexistente_lanzaNotFound() {
        when(tokenRecuperacionDAO.existsById(TOKEN_ID)).thenReturn(true);
        when(tokenRecuperacionDAO.findByToken(TOKEN)).thenReturn(Optional.empty());
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarTokenRecuperacion(TOKEN_ID, dto));

        verify(tokenRecuperacionDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarTokenRecuperacion: fechas invalidas lanzan BusinessValidationException")
    void actualizarTokenRecuperacion_fechasInvalidas_lanzaBusinessValidation() {
        dto.setFechaExpiracion(dto.getFechaCreacion());
        when(tokenRecuperacionDAO.existsById(TOKEN_ID)).thenReturn(true);
        when(tokenRecuperacionDAO.findByToken(TOKEN)).thenReturn(Optional.empty());
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.actualizarTokenRecuperacion(TOKEN_ID, dto));

        verify(tokenRecuperacionDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarTokenRecuperacion: si el DAO no devuelve el token actualizado lanza ResourceNotFoundException")
    void actualizarTokenRecuperacion_daoNoActualiza_lanzaNotFound() {
        when(tokenRecuperacionDAO.existsById(TOKEN_ID)).thenReturn(true);
        when(tokenRecuperacionDAO.findByToken(TOKEN)).thenReturn(Optional.empty());
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(tokenRecuperacionDAO.update(TOKEN_ID, dto)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarTokenRecuperacion(TOKEN_ID, dto));
    }

    // ---------------------------------------------------------------- eliminarTokenRecuperacion

    @Test
    @DisplayName("eliminarTokenRecuperacion: elimina cuando el token existe")
    void eliminarTokenRecuperacion_existente_elimina() {
        when(tokenRecuperacionDAO.deleteById(TOKEN_ID)).thenReturn(true);

        service.eliminarTokenRecuperacion(TOKEN_ID);

        verify(tokenRecuperacionDAO).deleteById(TOKEN_ID);
    }

    @Test
    @DisplayName("eliminarTokenRecuperacion: token inexistente lanza ResourceNotFoundException")
    void eliminarTokenRecuperacion_inexistente_lanzaNotFound() {
        when(tokenRecuperacionDAO.deleteById(TOKEN_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarTokenRecuperacion(TOKEN_ID));
    }
}
