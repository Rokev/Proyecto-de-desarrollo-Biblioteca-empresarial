package bussinesLayer.service.impl;

import bussinesLayer.dto.SesionDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.SesionDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SesionServiceImpl")
class SesionServiceImplTest {

    private static final Long SESION_ID = 1L;
    private static final Long OTRA_ID = 2L;
    private static final Long USUARIO_ID = 10L;
    private static final String TOKEN = "token-abc-123";

    @Mock private SesionDAO sesionDAO;
    @Mock private UsuarioDAO usuarioDAO;

    @InjectMocks
    private SesionServiceImpl service;

    private SesionDTO dto;

    @BeforeEach
    void setUp() {
        LocalDateTime inicio = LocalDateTime.of(2026, 1, 15, 10, 0);
        dto = new SesionDTO();
        dto.setToken(TOKEN);
        dto.setFechaInicio(inicio);
        dto.setFechaExpiracion(inicio.plusHours(2));
        dto.setUsuarioId(USUARIO_ID);
    }

    private SesionDTO sesionConId(Long id) {
        SesionDTO s = new SesionDTO();
        s.setId(id);
        return s;
    }

    // ---------------------------------------------------------------- crearSesion

    @Test
    @DisplayName("crearSesion: asigna activa=true cuando viene vacia")
    void crearSesion_valida_aplicaActivaPorDefecto() {
        when(sesionDAO.findByToken(TOKEN)).thenReturn(Optional.empty());
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(sesionDAO.save(dto)).thenAnswer(inv -> {
            SesionDTO s = inv.getArgument(0);
            s.setId(SESION_ID);
            return s;
        });

        SesionDTO resultado = service.crearSesion(dto);

        assertEquals(SESION_ID, resultado.getId());
        assertTrue(resultado.getActiva());
    }

    @Test
    @DisplayName("crearSesion: conserva activa=false si ya viene informada")
    void crearSesion_conActivaFalse_laConserva() {
        dto.setActiva(false);
        when(sesionDAO.findByToken(TOKEN)).thenReturn(Optional.empty());
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(sesionDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        assertFalse(service.crearSesion(dto).getActiva());
    }

    @Test
    @DisplayName("crearSesion: token duplicado lanza BusinessValidationException")
    void crearSesion_tokenDuplicado_lanzaBusinessValidation() {
        when(sesionDAO.findByToken(TOKEN)).thenReturn(Optional.of(sesionConId(OTRA_ID)));

        assertThrows(BusinessValidationException.class, () -> service.crearSesion(dto));

        verify(sesionDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearSesion: usuario inexistente lanza ResourceNotFoundException")
    void crearSesion_usuarioInexistente_lanzaNotFound() {
        when(sesionDAO.findByToken(TOKEN)).thenReturn(Optional.empty());
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearSesion(dto));

        verify(sesionDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearSesion: fechaExpiracion anterior a fechaInicio lanza BusinessValidationException")
    void crearSesion_expiracionAnterior_lanzaBusinessValidation() {
        dto.setFechaExpiracion(dto.getFechaInicio().minusHours(1));
        when(sesionDAO.findByToken(TOKEN)).thenReturn(Optional.empty());
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearSesion(dto));

        verify(sesionDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearSesion: fechaExpiracion igual a fechaInicio lanza BusinessValidationException")
    void crearSesion_expiracionIgual_lanzaBusinessValidation() {
        dto.setFechaExpiracion(dto.getFechaInicio());
        when(sesionDAO.findByToken(TOKEN)).thenReturn(Optional.empty());
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearSesion(dto));

        verify(sesionDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerSesionPorId: devuelve la sesion cuando existe")
    void obtenerSesionPorId_existente_laDevuelve() {
        SesionDTO existente = sesionConId(SESION_ID);
        when(sesionDAO.findById(SESION_ID)).thenReturn(Optional.of(existente));

        assertSame(existente, service.obtenerSesionPorId(SESION_ID));
    }

    @Test
    @DisplayName("obtenerSesionPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerSesionPorId_inexistente_lanzaNotFound() {
        when(sesionDAO.findById(SESION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerSesionPorId(SESION_ID));
    }

    @Test
    @DisplayName("obtenerTodasLasSesiones: delega en el DAO")
    void obtenerTodasLasSesiones_devuelveLista() {
        when(sesionDAO.findAll()).thenReturn(List.of(sesionConId(1L), sesionConId(2L)));

        assertEquals(2, service.obtenerTodasLasSesiones().size());
    }

    // ---------------------------------------------------------------- actualizarSesion

    @Test
    @DisplayName("actualizarSesion: sesion inexistente lanza ResourceNotFoundException")
    void actualizarSesion_inexistente_lanzaNotFound() {
        when(sesionDAO.existsById(SESION_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarSesion(SESION_ID, dto));

        verify(sesionDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarSesion: token propio o libre actualiza sin conflicto")
    void actualizarSesion_valida_actualiza() {
        when(sesionDAO.existsById(SESION_ID)).thenReturn(true);
        // La misma sesion aparece como "dueña" del token: no es conflicto.
        when(sesionDAO.findByToken(TOKEN)).thenReturn(Optional.of(sesionConId(SESION_ID)));
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(sesionDAO.update(SESION_ID, dto)).thenReturn(Optional.of(dto));

        assertSame(dto, service.actualizarSesion(SESION_ID, dto));
    }

    @Test
    @DisplayName("actualizarSesion: token de otra sesion lanza BusinessValidationException")
    void actualizarSesion_tokenDeOtra_lanzaBusinessValidation() {
        when(sesionDAO.existsById(SESION_ID)).thenReturn(true);
        when(sesionDAO.findByToken(TOKEN)).thenReturn(Optional.of(sesionConId(OTRA_ID)));

        assertThrows(BusinessValidationException.class, () -> service.actualizarSesion(SESION_ID, dto));

        verify(sesionDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarSesion: usuario inexistente lanza ResourceNotFoundException")
    void actualizarSesion_usuarioInexistente_lanzaNotFound() {
        when(sesionDAO.existsById(SESION_ID)).thenReturn(true);
        when(sesionDAO.findByToken(TOKEN)).thenReturn(Optional.empty());
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarSesion(SESION_ID, dto));

        verify(sesionDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarSesion: fechaExpiracion no posterior a fechaInicio lanza BusinessValidationException")
    void actualizarSesion_fechasInvalidas_lanzaBusinessValidation() {
        dto.setFechaExpiracion(dto.getFechaInicio());
        when(sesionDAO.existsById(SESION_ID)).thenReturn(true);
        when(sesionDAO.findByToken(TOKEN)).thenReturn(Optional.empty());
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.actualizarSesion(SESION_ID, dto));

        verify(sesionDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarSesion: si el DAO no devuelve la sesion actualizada lanza ResourceNotFoundException")
    void actualizarSesion_daoNoActualiza_lanzaNotFound() {
        when(sesionDAO.existsById(SESION_ID)).thenReturn(true);
        when(sesionDAO.findByToken(TOKEN)).thenReturn(Optional.empty());
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(sesionDAO.update(SESION_ID, dto)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarSesion(SESION_ID, dto));
    }

    // ---------------------------------------------------------------- eliminarSesion

    @Test
    @DisplayName("eliminarSesion: elimina cuando la sesion existe")
    void eliminarSesion_existente_elimina() {
        when(sesionDAO.deleteById(SESION_ID)).thenReturn(true);

        service.eliminarSesion(SESION_ID);

        verify(sesionDAO).deleteById(SESION_ID);
    }

    @Test
    @DisplayName("eliminarSesion: sesion inexistente lanza ResourceNotFoundException")
    void eliminarSesion_inexistente_lanzaNotFound() {
        when(sesionDAO.deleteById(SESION_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarSesion(SESION_ID));
    }
}