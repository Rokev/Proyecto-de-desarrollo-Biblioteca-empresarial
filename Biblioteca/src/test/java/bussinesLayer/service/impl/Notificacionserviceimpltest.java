package bussinesLayer.service.impl;

import bussinesLayer.dto.NotificacionDTO;
import bussinesLayer.dto.UsuarioDTO;
import bussinesLayer.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.NotificacionDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacionServiceImpl")
class NotificacionServiceImplTest {

    private static final Long NOTIFICACION_ID = 1L;
    private static final Long USUARIO_ID = 10L;
    private static final Long SEDE_ID = 7L;

    @Mock private NotificacionDAO notificacionDAO;
    @Mock private UsuarioDAO usuarioDAO;

    @InjectMocks
    private NotificacionServiceImpl service;

    private NotificacionDTO dto;

    @BeforeEach
    void setUp() {
        dto = new NotificacionDTO();
        dto.setTipo("RECORDATORIO");
        dto.setTitulo("Prestamo por vencer");
        dto.setMensaje("Tu prestamo vence manana");
        dto.setCanal("APP");
        dto.setUsuarioId(USUARIO_ID);
    }

    private NotificacionDTO notificacionConId(Long id) {
        NotificacionDTO n = new NotificacionDTO();
        n.setId(id);
        return n;
    }

    private UsuarioDTO bibliotecario(Long id) {
        UsuarioDTO u = new UsuarioDTO();
        u.setId(id);
        return u;
    }

    // ---------------------------------------------------------------- crearNotificacion

    @Test
    @DisplayName("crearNotificacion: asigna leida=false cuando viene vacia")
    void crearNotificacion_valida_aplicaLeidaPorDefecto() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(notificacionDAO.save(dto)).thenAnswer(inv -> {
            NotificacionDTO n = inv.getArgument(0);
            n.setId(NOTIFICACION_ID);
            return n;
        });

        NotificacionDTO resultado = service.crearNotificacion(dto);

        assertEquals(NOTIFICACION_ID, resultado.getId());
        assertFalse(resultado.getLeida());
    }

    @Test
    @DisplayName("crearNotificacion: conserva leida=true si ya viene informada")
    void crearNotificacion_conLeidaTrue_laConserva() {
        dto.setLeida(true);
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(notificacionDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        assertTrue(service.crearNotificacion(dto).getLeida());
    }

    @Test
    @DisplayName("crearNotificacion: usuario inexistente lanza ResourceNotFoundException")
    void crearNotificacion_usuarioInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearNotificacion(dto));

        verify(notificacionDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerNotificacionPorId: devuelve la notificacion cuando existe")
    void obtenerNotificacionPorId_existente_laDevuelve() {
        NotificacionDTO existente = notificacionConId(NOTIFICACION_ID);
        when(notificacionDAO.findById(NOTIFICACION_ID)).thenReturn(Optional.of(existente));

        assertSame(existente, service.obtenerNotificacionPorId(NOTIFICACION_ID));
    }

    @Test
    @DisplayName("obtenerNotificacionPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerNotificacionPorId_inexistente_lanzaNotFound() {
        when(notificacionDAO.findById(NOTIFICACION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerNotificacionPorId(NOTIFICACION_ID));
    }

    @Test
    @DisplayName("obtenerTodasLasNotificaciones: delega en el DAO")
    void obtenerTodasLasNotificaciones_devuelveLista() {
        when(notificacionDAO.findAll()).thenReturn(List.of(notificacionConId(1L), notificacionConId(2L)));

        assertEquals(2, service.obtenerTodasLasNotificaciones().size());
    }

    // ---------------------------------------------------------------- actualizarNotificacion

    @Test
    @DisplayName("actualizarNotificacion: notificacion inexistente lanza ResourceNotFoundException")
    void actualizarNotificacion_inexistente_lanzaNotFound() {
        when(notificacionDAO.existsById(NOTIFICACION_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarNotificacion(NOTIFICACION_ID, dto));

        verify(notificacionDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarNotificacion: con usuario existente actualiza")
    void actualizarNotificacion_valida_actualiza() {
        when(notificacionDAO.existsById(NOTIFICACION_ID)).thenReturn(true);
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(notificacionDAO.update(NOTIFICACION_ID, dto)).thenReturn(Optional.of(dto));

        assertSame(dto, service.actualizarNotificacion(NOTIFICACION_ID, dto));
    }

    @Test
    @DisplayName("actualizarNotificacion: usuario inexistente lanza ResourceNotFoundException")
    void actualizarNotificacion_usuarioInexistente_lanzaNotFound() {
        when(notificacionDAO.existsById(NOTIFICACION_ID)).thenReturn(true);
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarNotificacion(NOTIFICACION_ID, dto));

        verify(notificacionDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarNotificacion: si el DAO no devuelve la notificacion actualizada lanza ResourceNotFoundException")
    void actualizarNotificacion_daoNoActualiza_lanzaNotFound() {
        when(notificacionDAO.existsById(NOTIFICACION_ID)).thenReturn(true);
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(notificacionDAO.update(NOTIFICACION_ID, dto)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarNotificacion(NOTIFICACION_ID, dto));
    }

    // ---------------------------------------------------------------- eliminarNotificacion

    @Test
    @DisplayName("eliminarNotificacion: elimina cuando la notificacion existe")
    void eliminarNotificacion_existente_elimina() {
        when(notificacionDAO.deleteById(NOTIFICACION_ID)).thenReturn(true);

        service.eliminarNotificacion(NOTIFICACION_ID);

        verify(notificacionDAO).deleteById(NOTIFICACION_ID);
    }

    @Test
    @DisplayName("eliminarNotificacion: notificacion inexistente lanza ResourceNotFoundException")
    void eliminarNotificacion_inexistente_lanzaNotFound() {
        when(notificacionDAO.deleteById(NOTIFICACION_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarNotificacion(NOTIFICACION_ID));
    }

    // ---------------------------------------------------------------- notificarSede

    @Test
    @DisplayName("notificarSede: sin bibliotecarios en la sede no guarda notificaciones")
    void notificarSede_sinBibliotecarios_noGuardaNada() {
        when(usuarioDAO.findBySedeIdAndRolNombreIgnoreCase(SEDE_ID, "BIBLIOTECARIO")).thenReturn(List.of());

        service.notificarSede(SEDE_ID, "ALERTA", "Titulo", "Mensaje");

        verify(notificacionDAO, never()).save(any());
    }

    @Test
    @DisplayName("notificarSede: crea una notificacion de APP por cada bibliotecario de la sede")
    void notificarSede_conBibliotecarios_guardaUnaPorCadaUno() {
        when(usuarioDAO.findBySedeIdAndRolNombreIgnoreCase(SEDE_ID, "BIBLIOTECARIO"))
                .thenReturn(List.of(bibliotecario(21L), bibliotecario(22L)));

        service.notificarSede(SEDE_ID, "ALERTA", "Titulo", "Mensaje");

        ArgumentCaptor<NotificacionDTO> captor = ArgumentCaptor.forClass(NotificacionDTO.class);
        verify(notificacionDAO, times(2)).save(captor.capture());

        List<NotificacionDTO> guardadas = captor.getAllValues();
        assertEquals(21L, guardadas.get(0).getUsuarioId());
        assertEquals(22L, guardadas.get(1).getUsuarioId());
        for (NotificacionDTO n : guardadas) {
            assertEquals("ALERTA", n.getTipo());
            assertEquals("Titulo", n.getTitulo());
            assertEquals("Mensaje", n.getMensaje());
            assertEquals("APP", n.getCanal());
            assertFalse(n.getLeida());
            assertNotNull(n.getFechaCreacion());
        }
    }
}
