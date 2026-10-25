package bussinesLayer.service.impl;

import bussinesLayer.dto.EventoDTO;
import bussinesLayer.dto.InscripcionEventoDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.SuscripcionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.EventoDAO;
import persistanceLayer.dao.InscripcionEventoDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InscripcionEventoServiceImpl")
class InscripcionEventoServiceImplTest {

    private static final Long INSCRIPCION_ID = 1L;
    private static final Long EVENTO_ID = 5L;
    private static final Long OTRO_EVENTO_ID = 6L;
    private static final Long USUARIO_ID = 10L;
    private static final Long OTRO_USUARIO_ID = 11L;

    @Mock private InscripcionEventoDAO inscripcionEventoDAO;
    @Mock private EventoDAO eventoDAO;
    @Mock private UsuarioDAO usuarioDAO;
    @Mock private SuscripcionService suscripcionService;

    @InjectMocks
    private InscripcionEventoServiceImpl service;

    private InscripcionEventoDTO dto;

    @BeforeEach
    void setUp() {
        dto = new InscripcionEventoDTO();
        dto.setEventoId(EVENTO_ID);
        dto.setUsuarioId(USUARIO_ID);
    }

    private EventoDTO evento(Boolean exclusivo) {
        EventoDTO e = new EventoDTO();
        e.setId(EVENTO_ID);
        e.setExclusivo(exclusivo);
        return e;
    }

    private InscripcionEventoDTO inscripcion(Long id, Long eventoId, Long usuarioId, String estado, int cupos) {
        InscripcionEventoDTO i = new InscripcionEventoDTO();
        i.setId(id);
        i.setEventoId(eventoId);
        i.setUsuarioId(usuarioId);
        i.setEstado(estado);
        i.setCantidadCupos(cupos);
        return i;
    }

    // ---------------------------------------------------------------- crearInscripcionEvento

    @Test
    @DisplayName("crearInscripcionEvento: asigna 1 cupo, estado ACTIVA y limpia fechaCancelacion")
    void crearInscripcion_valida_aplicaDefaults() {
        dto.setEstado("CANCELADA");
        dto.setFechaCancelacion(LocalDateTime.of(2026, 1, 1, 0, 0));
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(evento(false)));
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(eventoDAO.descontarCupos(EVENTO_ID, 1)).thenReturn(1);
        when(inscripcionEventoDAO.save(dto)).thenAnswer(inv -> {
            InscripcionEventoDTO i = inv.getArgument(0);
            i.setId(INSCRIPCION_ID);
            return i;
        });

        InscripcionEventoDTO resultado = service.crearInscripcionEvento(dto);

        assertEquals(INSCRIPCION_ID, resultado.getId());
        assertEquals(1, resultado.getCantidadCupos());
        assertEquals("ACTIVA", resultado.getEstado());
        assertNull(resultado.getFechaCancelacion());
        verify(suscripcionService, never()).tieneSuscripcionVigente(any());
    }

    @Test
    @DisplayName("crearInscripcionEvento: conserva la cantidad de cupos informada y descuenta esa cantidad")
    void crearInscripcion_conCantidad_descuentaEsaCantidad() {
        dto.setCantidadCupos(3);
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(evento(false)));
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(eventoDAO.descontarCupos(EVENTO_ID, 3)).thenReturn(1);
        when(inscripcionEventoDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(3, service.crearInscripcionEvento(dto).getCantidadCupos());
    }

    @Test
    @DisplayName("crearInscripcionEvento: evento inexistente lanza ResourceNotFoundException")
    void crearInscripcion_eventoInexistente_lanzaNotFound() {
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.crearInscripcionEvento(dto));

        verify(inscripcionEventoDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearInscripcionEvento: usuario inexistente lanza ResourceNotFoundException")
    void crearInscripcion_usuarioInexistente_lanzaNotFound() {
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(evento(false)));
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearInscripcionEvento(dto));

        verify(inscripcionEventoDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearInscripcionEvento: evento exclusivo sin suscripcion vigente lanza BusinessValidationException")
    void crearInscripcion_exclusivoSinSuscripcion_lanzaBusinessValidation() {
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(evento(true)));
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(suscripcionService.tieneSuscripcionVigente(USUARIO_ID)).thenReturn(false);

        assertThrows(BusinessValidationException.class, () -> service.crearInscripcionEvento(dto));

        verify(inscripcionEventoDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearInscripcionEvento: evento exclusivo con suscripcion vigente permite inscribirse")
    void crearInscripcion_exclusivoConSuscripcion_crea() {
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(evento(true)));
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(suscripcionService.tieneSuscripcionVigente(USUARIO_ID)).thenReturn(true);
        when(eventoDAO.descontarCupos(EVENTO_ID, 1)).thenReturn(1);
        when(inscripcionEventoDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        assertSame(dto, service.crearInscripcionEvento(dto));
    }

    @Test
    @DisplayName("crearInscripcionEvento: usuario ya inscrito lanza BusinessValidationException")
    void crearInscripcion_yaInscrito_lanzaBusinessValidation() {
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(evento(false)));
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(inscripcionEventoDAO.existsByEventoIdAndUsuarioId(EVENTO_ID, USUARIO_ID)).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearInscripcionEvento(dto));

        verify(eventoDAO, never()).descontarCupos(any(), anyInt());
        verify(inscripcionEventoDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearInscripcionEvento: sin cupos suficientes lanza BusinessValidationException")
    void crearInscripcion_sinCupos_lanzaBusinessValidation() {
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(evento(false)));
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(eventoDAO.descontarCupos(EVENTO_ID, 1)).thenReturn(0);

        assertThrows(BusinessValidationException.class, () -> service.crearInscripcionEvento(dto));

        verify(inscripcionEventoDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerInscripcionEventoPorId: devuelve la inscripcion cuando existe")
    void obtenerInscripcionPorId_existente_laDevuelve() {
        InscripcionEventoDTO existente = inscripcion(INSCRIPCION_ID, EVENTO_ID, USUARIO_ID, "ACTIVA", 1);
        when(inscripcionEventoDAO.findById(INSCRIPCION_ID)).thenReturn(Optional.of(existente));

        assertSame(existente, service.obtenerInscripcionEventoPorId(INSCRIPCION_ID));
    }

    @Test
    @DisplayName("obtenerInscripcionEventoPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerInscripcionPorId_inexistente_lanzaNotFound() {
        when(inscripcionEventoDAO.findById(INSCRIPCION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerInscripcionEventoPorId(INSCRIPCION_ID));
    }

    @Test
    @DisplayName("obtenerTodasLasInscripcionesEvento: delega en el DAO")
    void obtenerTodasLasInscripciones_devuelveLista() {
        when(inscripcionEventoDAO.findAll()).thenReturn(List.of(
                inscripcion(1L, EVENTO_ID, USUARIO_ID, "ACTIVA", 1),
                inscripcion(2L, EVENTO_ID, OTRO_USUARIO_ID, "ACTIVA", 1)));

        assertEquals(2, service.obtenerTodasLasInscripcionesEvento().size());
    }

    // ---------------------------------------------------------------- actualizarInscripcionEvento

    @Test
    @DisplayName("actualizarInscripcionEvento: inscripcion inexistente lanza ResourceNotFoundException")
    void actualizarInscripcion_inexistente_lanzaNotFound() {
        when(inscripcionEventoDAO.findById(INSCRIPCION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarInscripcionEvento(INSCRIPCION_ID, dto));

        verify(inscripcionEventoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarInscripcionEvento: cambiar el evento lanza BusinessValidationException")
    void actualizarInscripcion_cambiaEvento_lanzaBusinessValidation() {
        dto.setEventoId(OTRO_EVENTO_ID);
        when(inscripcionEventoDAO.findById(INSCRIPCION_ID))
                .thenReturn(Optional.of(inscripcion(INSCRIPCION_ID, EVENTO_ID, USUARIO_ID, "ACTIVA", 2)));

        assertThrows(BusinessValidationException.class, () -> service.actualizarInscripcionEvento(INSCRIPCION_ID, dto));

        verify(inscripcionEventoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarInscripcionEvento: cambiar el usuario lanza BusinessValidationException")
    void actualizarInscripcion_cambiaUsuario_lanzaBusinessValidation() {
        dto.setUsuarioId(OTRO_USUARIO_ID);
        when(inscripcionEventoDAO.findById(INSCRIPCION_ID))
                .thenReturn(Optional.of(inscripcion(INSCRIPCION_ID, EVENTO_ID, USUARIO_ID, "ACTIVA", 2)));

        assertThrows(BusinessValidationException.class, () -> service.actualizarInscripcionEvento(INSCRIPCION_ID, dto));

        verify(inscripcionEventoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarInscripcionEvento: cancelar una inscripcion activa libera sus cupos y fija fechaCancelacion")
    void actualizarInscripcion_cancelarActiva_liberaCupos() {
        dto.setEstado("cancelada");
        dto.setCantidadCupos(99);
        when(inscripcionEventoDAO.findById(INSCRIPCION_ID))
                .thenReturn(Optional.of(inscripcion(INSCRIPCION_ID, EVENTO_ID, USUARIO_ID, "ACTIVA", 2)));
        when(inscripcionEventoDAO.update(INSCRIPCION_ID, dto)).thenReturn(Optional.of(dto));

        InscripcionEventoDTO resultado = service.actualizarInscripcionEvento(INSCRIPCION_ID, dto);

        verify(eventoDAO).liberarCupos(EVENTO_ID, 2);
        assertNotNull(resultado.getFechaCancelacion());
        assertEquals(2, resultado.getCantidadCupos());
    }

    @Test
    @DisplayName("actualizarInscripcionEvento: cancelar conserva la fechaCancelacion si ya viene informada")
    void actualizarInscripcion_cancelarConFecha_laConserva() {
        LocalDateTime fecha = LocalDateTime.of(2026, 5, 5, 12, 0);
        dto.setEstado("CANCELADA");
        dto.setFechaCancelacion(fecha);
        when(inscripcionEventoDAO.findById(INSCRIPCION_ID))
                .thenReturn(Optional.of(inscripcion(INSCRIPCION_ID, EVENTO_ID, USUARIO_ID, "ACTIVA", 1)));
        when(inscripcionEventoDAO.update(INSCRIPCION_ID, dto)).thenReturn(Optional.of(dto));

        assertEquals(fecha, service.actualizarInscripcionEvento(INSCRIPCION_ID, dto).getFechaCancelacion());
    }

    @Test
    @DisplayName("actualizarInscripcionEvento: reactivar una cancelada descuenta los cupos y limpia fechaCancelacion")
    void actualizarInscripcion_reactivar_descuentaCupos() {
        dto.setEstado("ACTIVA");
        dto.setFechaCancelacion(LocalDateTime.of(2026, 5, 5, 12, 0));
        when(inscripcionEventoDAO.findById(INSCRIPCION_ID))
                .thenReturn(Optional.of(inscripcion(INSCRIPCION_ID, EVENTO_ID, USUARIO_ID, "CANCELADA", 2)));
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(evento(false)));
        when(eventoDAO.descontarCupos(EVENTO_ID, 2)).thenReturn(1);
        when(inscripcionEventoDAO.update(INSCRIPCION_ID, dto)).thenReturn(Optional.of(dto));

        InscripcionEventoDTO resultado = service.actualizarInscripcionEvento(INSCRIPCION_ID, dto);

        assertNull(resultado.getFechaCancelacion());
        assertEquals(2, resultado.getCantidadCupos());
        verify(eventoDAO, never()).liberarCupos(any(), anyInt());
    }

    @Test
    @DisplayName("actualizarInscripcionEvento: reactivar con evento inexistente lanza ResourceNotFoundException")
    void actualizarInscripcion_reactivarEventoInexistente_lanzaNotFound() {
        dto.setEstado("ACTIVA");
        when(inscripcionEventoDAO.findById(INSCRIPCION_ID))
                .thenReturn(Optional.of(inscripcion(INSCRIPCION_ID, EVENTO_ID, USUARIO_ID, "CANCELADA", 1)));
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarInscripcionEvento(INSCRIPCION_ID, dto));

        verify(inscripcionEventoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarInscripcionEvento: reactivar en evento exclusivo sin suscripcion lanza BusinessValidationException")
    void actualizarInscripcion_reactivarExclusivoSinSuscripcion_lanzaBusinessValidation() {
        dto.setEstado("ACTIVA");
        when(inscripcionEventoDAO.findById(INSCRIPCION_ID))
                .thenReturn(Optional.of(inscripcion(INSCRIPCION_ID, EVENTO_ID, USUARIO_ID, "CANCELADA", 1)));
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(evento(true)));
        when(suscripcionService.tieneSuscripcionVigente(USUARIO_ID)).thenReturn(false);

        assertThrows(BusinessValidationException.class, () -> service.actualizarInscripcionEvento(INSCRIPCION_ID, dto));

        verify(eventoDAO, never()).descontarCupos(any(), anyInt());
        verify(inscripcionEventoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarInscripcionEvento: reactivar sin cupos suficientes lanza BusinessValidationException")
    void actualizarInscripcion_reactivarSinCupos_lanzaBusinessValidation() {
        dto.setEstado("ACTIVA");
        when(inscripcionEventoDAO.findById(INSCRIPCION_ID))
                .thenReturn(Optional.of(inscripcion(INSCRIPCION_ID, EVENTO_ID, USUARIO_ID, "CANCELADA", 3)));
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(evento(false)));
        when(eventoDAO.descontarCupos(EVENTO_ID, 3)).thenReturn(0);

        assertThrows(BusinessValidationException.class, () -> service.actualizarInscripcionEvento(INSCRIPCION_ID, dto));

        verify(inscripcionEventoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarInscripcionEvento: sin cambio de estado no libera ni descuenta cupos")
    void actualizarInscripcion_sinCambioDeEstado_noTocaCupos() {
        dto.setEstado("ACTIVA");
        when(inscripcionEventoDAO.findById(INSCRIPCION_ID))
                .thenReturn(Optional.of(inscripcion(INSCRIPCION_ID, EVENTO_ID, USUARIO_ID, "ACTIVA", 2)));
        when(inscripcionEventoDAO.update(INSCRIPCION_ID, dto)).thenReturn(Optional.of(dto));

        service.actualizarInscripcionEvento(INSCRIPCION_ID, dto);

        verify(eventoDAO, never()).liberarCupos(any(), anyInt());
        verify(eventoDAO, never()).descontarCupos(any(), anyInt());
    }

    @Test
    @DisplayName("actualizarInscripcionEvento: sin estado en el DTO no libera ni descuenta cupos")
    void actualizarInscripcion_sinEstado_noTocaCupos() {
        dto.setEstado(null);
        when(inscripcionEventoDAO.findById(INSCRIPCION_ID))
                .thenReturn(Optional.of(inscripcion(INSCRIPCION_ID, EVENTO_ID, USUARIO_ID, "ACTIVA", 2)));
        when(inscripcionEventoDAO.update(INSCRIPCION_ID, dto)).thenReturn(Optional.of(dto));

        service.actualizarInscripcionEvento(INSCRIPCION_ID, dto);

        verify(eventoDAO, never()).liberarCupos(any(), anyInt());
        verify(eventoDAO, never()).descontarCupos(any(), anyInt());
    }

    @Test
    @DisplayName("actualizarInscripcionEvento: si el DAO no devuelve la inscripcion actualizada lanza ResourceNotFoundException")
    void actualizarInscripcion_daoNoActualiza_lanzaNotFound() {
        dto.setEstado("ACTIVA");
        when(inscripcionEventoDAO.findById(INSCRIPCION_ID))
                .thenReturn(Optional.of(inscripcion(INSCRIPCION_ID, EVENTO_ID, USUARIO_ID, "ACTIVA", 1)));
        when(inscripcionEventoDAO.update(INSCRIPCION_ID, dto)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarInscripcionEvento(INSCRIPCION_ID, dto));
    }

    // ---------------------------------------------------------------- eliminarInscripcionEvento

    @Test
    @DisplayName("eliminarInscripcionEvento: inscripcion inexistente lanza ResourceNotFoundException")
    void eliminarInscripcion_inexistente_lanzaNotFound() {
        when(inscripcionEventoDAO.findById(INSCRIPCION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarInscripcionEvento(INSCRIPCION_ID));

        verify(inscripcionEventoDAO, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminarInscripcionEvento: una inscripcion activa libera sus cupos al eliminarse")
    void eliminarInscripcion_activa_liberaCuposYElimina() {
        when(inscripcionEventoDAO.findById(INSCRIPCION_ID))
                .thenReturn(Optional.of(inscripcion(INSCRIPCION_ID, EVENTO_ID, USUARIO_ID, "ACTIVA", 4)));

        service.eliminarInscripcionEvento(INSCRIPCION_ID);

        verify(eventoDAO).liberarCupos(EVENTO_ID, 4);
        verify(inscripcionEventoDAO).deleteById(INSCRIPCION_ID);
    }

    @Test
    @DisplayName("eliminarInscripcionEvento: una inscripcion cancelada se elimina sin liberar cupos")
    void eliminarInscripcion_cancelada_eliminaSinLiberar() {
        when(inscripcionEventoDAO.findById(INSCRIPCION_ID))
                .thenReturn(Optional.of(inscripcion(INSCRIPCION_ID, EVENTO_ID, USUARIO_ID, "CANCELADA", 4)));

        service.eliminarInscripcionEvento(INSCRIPCION_ID);

        verify(eventoDAO, never()).liberarCupos(any(), anyInt());
        verify(inscripcionEventoDAO).deleteById(INSCRIPCION_ID);
    }

    // ---------------------------------------------------------------- cancelarInscripcionesExclusivasSinSuscripcion

    @Test
    @DisplayName("cancelarInscripcionesExclusivasSinSuscripcion: sin candidatas devuelve 0")
    void cancelarExclusivas_sinCandidatas_devuelveCero() {
        when(inscripcionEventoDAO.findActivasEnEventosExclusivosFuturos(any(LocalDateTime.class)))
                .thenReturn(List.of());

        assertEquals(0, service.cancelarInscripcionesExclusivasSinSuscripcion());

        verify(eventoDAO, never()).liberarCupos(any(), anyInt());
    }

    @Test
    @DisplayName("cancelarInscripcionesExclusivasSinSuscripcion: usuario con suscripcion vigente conserva su inscripcion")
    void cancelarExclusivas_conSuscripcion_noCancela() {
        when(inscripcionEventoDAO.findActivasEnEventosExclusivosFuturos(any(LocalDateTime.class)))
                .thenReturn(List.of(inscripcion(1L, EVENTO_ID, USUARIO_ID, "ACTIVA", 1)));
        when(suscripcionService.tieneSuscripcionVigente(USUARIO_ID)).thenReturn(true);

        assertEquals(0, service.cancelarInscripcionesExclusivasSinSuscripcion());

        verify(eventoDAO, never()).liberarCupos(any(), anyInt());
        verify(inscripcionEventoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("cancelarInscripcionesExclusivasSinSuscripcion: cancela y libera cupos de quien no tiene suscripcion")
    void cancelarExclusivas_sinSuscripcion_cancelaYLiberaCupos() {
        InscripcionEventoDTO candidata = inscripcion(1L, EVENTO_ID, USUARIO_ID, "ACTIVA", 2);
        when(inscripcionEventoDAO.findActivasEnEventosExclusivosFuturos(any(LocalDateTime.class)))
                .thenReturn(List.of(candidata));
        when(suscripcionService.tieneSuscripcionVigente(USUARIO_ID)).thenReturn(false);

        int canceladas = service.cancelarInscripcionesExclusivasSinSuscripcion();

        assertEquals(1, canceladas);
        assertEquals("CANCELADA", candidata.getEstado());
        assertNotNull(candidata.getFechaCancelacion());
        verify(eventoDAO).liberarCupos(EVENTO_ID, 2);
        verify(inscripcionEventoDAO).update(1L, candidata);
    }

    @Test
    @DisplayName("cancelarInscripcionesExclusivasSinSuscripcion: consulta la suscripcion una sola vez por usuario")
    void cancelarExclusivas_variasDelMismoUsuario_consultaSuscripcionUnaVez() {
        when(inscripcionEventoDAO.findActivasEnEventosExclusivosFuturos(any(LocalDateTime.class)))
                .thenReturn(List.of(
                        inscripcion(1L, EVENTO_ID, USUARIO_ID, "ACTIVA", 1),
                        inscripcion(2L, OTRO_EVENTO_ID, USUARIO_ID, "ACTIVA", 1)));
        when(suscripcionService.tieneSuscripcionVigente(USUARIO_ID)).thenReturn(false);

        assertEquals(2, service.cancelarInscripcionesExclusivasSinSuscripcion());

        verify(suscripcionService, times(1)).tieneSuscripcionVigente(USUARIO_ID);
    }

    @Test
    @DisplayName("cancelarInscripcionesExclusivasSinSuscripcion: solo cancela las de usuarios sin suscripcion")
    void cancelarExclusivas_usuariosMixtos_cancelaSoloSinSuscripcion() {
        when(inscripcionEventoDAO.findActivasEnEventosExclusivosFuturos(any(LocalDateTime.class)))
                .thenReturn(List.of(
                        inscripcion(1L, EVENTO_ID, USUARIO_ID, "ACTIVA", 1),
                        inscripcion(2L, EVENTO_ID, OTRO_USUARIO_ID, "ACTIVA", 1)));
        when(suscripcionService.tieneSuscripcionVigente(USUARIO_ID)).thenReturn(true);
        when(suscripcionService.tieneSuscripcionVigente(OTRO_USUARIO_ID)).thenReturn(false);

        assertEquals(1, service.cancelarInscripcionesExclusivasSinSuscripcion());

        verify(eventoDAO, times(1)).liberarCupos(any(), anyInt());
    }
}
