package bussinesLayer.service.impl;

import bussinesLayer.dto.EventoDTO;
import bussinesLayer.dto.InscripcionEventoDTO;
import bussinesLayer.dto.NotificacionDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.EventoDAO;
import persistanceLayer.dao.InscripcionEventoDAO;
import persistanceLayer.dao.NotificacionDAO;
import persistanceLayer.dao.SedeDAO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventoServiceImpl")
class EventoServiceImplTest {

    private static final Long EVENTO_ID = 1L;
    private static final Long SEDE_ID = 7L;

    @Mock private EventoDAO eventoDAO;
    @Mock private SedeDAO sedeDAO;
    @Mock private InscripcionEventoDAO inscripcionEventoDAO;
    @Mock private NotificacionDAO notificacionDAO;

    @InjectMocks
    private EventoServiceImpl service;

    private EventoDTO dto;

    @BeforeEach
    void setUp() {
        LocalDateTime inicio = LocalDateTime.of(2026, 6, 1, 9, 0);
        dto = new EventoDTO();
        dto.setTitulo("Taller de lectura");
        dto.setDescripcion("Taller para jovenes");
        dto.setFechaInicio(inicio);
        dto.setFechaFin(inicio.plusHours(3));
        dto.setCupoMaximo(30);
        dto.setCuposDisponibles(30);
        dto.setSedeId(SEDE_ID);
    }

    private EventoDTO eventoActual(Boolean exclusivo) {
        EventoDTO e = new EventoDTO();
        e.setId(EVENTO_ID);
        e.setExclusivo(exclusivo);
        return e;
    }

    private EventoDTO eventoProgramado() {
        EventoDTO e = eventoActual(false);
        e.setTitulo("Taller de lectura");
        e.setEstado("PROGRAMADO");
        e.setFechaInicio(LocalDateTime.of(2026, 12, 1, 9, 0));
        e.setCupoMaximo(30);
        e.setCuposDisponibles(20);
        return e;
    }

    private InscripcionEventoDTO inscripcion(Long id, Long usuarioId, Integer cupos, String estado) {
        InscripcionEventoDTO i = new InscripcionEventoDTO();
        i.setId(id);
        i.setUsuarioId(usuarioId);
        i.setCantidadCupos(cupos);
        i.setEstado(estado);
        return i;
    }

    private EventoDTO proximo(Long id, Long sedeId, Boolean exclusivo, LocalDateTime inicio) {
        EventoDTO e = new EventoDTO();
        e.setId(id);
        e.setSedeId(sedeId);
        e.setExclusivo(exclusivo);
        e.setFechaInicio(inicio);
        e.setEstado("PROGRAMADO");
        return e;
    }

    private void stubProximos(List<EventoDTO> eventos) {
        when(eventoDAO.findByFechaInicioGreaterThanEqualAndEstadoIgnoreCaseOrderByFechaInicioAsc(
                any(LocalDateTime.class), eq("PROGRAMADO"))).thenReturn(eventos);
    }

    // ---------------------------------------------------------------- crearEvento

    @Test
    @DisplayName("crearEvento: asigna estado PROGRAMADO y exclusivo=false cuando vienen vacios")
    void crearEvento_valido_aplicaDefaults() {
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(eventoDAO.save(dto)).thenAnswer(inv -> {
            EventoDTO e = inv.getArgument(0);
            e.setId(EVENTO_ID);
            return e;
        });

        EventoDTO resultado = service.crearEvento(dto);

        assertEquals(EVENTO_ID, resultado.getId());
        assertEquals("PROGRAMADO", resultado.getEstado());
        assertFalse(resultado.getExclusivo());
    }

    @Test
    @DisplayName("crearEvento: estado en blanco asigna PROGRAMADO")
    void crearEvento_estadoEnBlanco_asignaProgramado() {
        dto.setEstado(" ");
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(eventoDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("PROGRAMADO", service.crearEvento(dto).getEstado());
    }

    @Test
    @DisplayName("crearEvento: conserva estado y exclusivo si ya vienen informados")
    void crearEvento_conEstadoYExclusivo_losConserva() {
        dto.setEstado("CANCELADO");
        dto.setExclusivo(true);
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(eventoDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        EventoDTO resultado = service.crearEvento(dto);

        assertEquals("CANCELADO", resultado.getEstado());
        assertTrue(resultado.getExclusivo());
    }

    @Test
    @DisplayName("crearEvento: fechaFin anterior a fechaInicio lanza BusinessValidationException")
    void crearEvento_finAnterior_lanzaBusinessValidation() {
        dto.setFechaFin(dto.getFechaInicio().minusHours(1));

        assertThrows(BusinessValidationException.class, () -> service.crearEvento(dto));

        verify(eventoDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearEvento: fechaFin igual a fechaInicio lanza BusinessValidationException")
    void crearEvento_finIgual_lanzaBusinessValidation() {
        dto.setFechaFin(dto.getFechaInicio());

        assertThrows(BusinessValidationException.class, () -> service.crearEvento(dto));

        verify(eventoDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearEvento: cuposDisponibles mayor al cupoMaximo lanza BusinessValidationException")
    void crearEvento_cuposExcedidos_lanzaBusinessValidation() {
        dto.setCuposDisponibles(31);

        assertThrows(BusinessValidationException.class, () -> service.crearEvento(dto));

        verify(eventoDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearEvento: sede inexistente lanza ResourceNotFoundException")
    void crearEvento_sedeInexistente_lanzaNotFound() {
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearEvento(dto));

        verify(eventoDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerEventoPorId: devuelve el evento cuando existe")
    void obtenerEventoPorId_existente_loDevuelve() {
        EventoDTO existente = eventoActual(false);
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(existente));

        assertSame(existente, service.obtenerEventoPorId(EVENTO_ID));
    }

    @Test
    @DisplayName("obtenerEventoPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerEventoPorId_inexistente_lanzaNotFound() {
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerEventoPorId(EVENTO_ID));
    }

    @Test
    @DisplayName("obtenerTodosLosEventos: delega en el DAO")
    void obtenerTodosLosEventos_devuelveLista() {
        when(eventoDAO.findAll()).thenReturn(List.of(eventoActual(false), eventoActual(true)));

        assertEquals(2, service.obtenerTodosLosEventos().size());
    }

    // ---------------------------------------------------------------- actualizarEvento

    @Test
    @DisplayName("actualizarEvento: evento inexistente lanza ResourceNotFoundException")
    void actualizarEvento_inexistente_lanzaNotFound() {
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarEvento(EVENTO_ID, dto));

        verify(eventoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarEvento: datos invalidos lanzan BusinessValidationException")
    void actualizarEvento_fechasInvalidas_lanzaBusinessValidation() {
        dto.setFechaFin(dto.getFechaInicio().minusHours(1));
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(eventoActual(false)));

        assertThrows(BusinessValidationException.class, () -> service.actualizarEvento(EVENTO_ID, dto));

        verify(eventoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarEvento: sede inexistente lanza ResourceNotFoundException")
    void actualizarEvento_sedeInexistente_lanzaNotFound() {
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(eventoActual(false)));
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarEvento(EVENTO_ID, dto));

        verify(eventoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarEvento: sin indicar exclusivo actualiza y consulta las inscripciones solo una vez (cupos)")
    void actualizarEvento_sinExclusivo_actualizaSinConsultarInscripciones() {
        dto.setExclusivo(null);
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(eventoActual(false)));
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(eventoDAO.update(EVENTO_ID, dto)).thenReturn(Optional.of(dto));

        assertSame(dto, service.actualizarEvento(EVENTO_ID, dto));

        verify(inscripcionEventoDAO, times(1)).findByEventoIdAndEstadoIgnoreCase(EVENTO_ID, "ACTIVA");
    }

    @Test
    @DisplayName("actualizarEvento: mismo valor de exclusivo actualiza y consulta las inscripciones solo una vez (cupos)")
    void actualizarEvento_mismoExclusivo_actualizaSinConsultarInscripciones() {
        dto.setExclusivo(true);
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(eventoActual(true)));
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(eventoDAO.update(EVENTO_ID, dto)).thenReturn(Optional.of(dto));

        assertSame(dto, service.actualizarEvento(EVENTO_ID, dto));

        verify(inscripcionEventoDAO, times(1)).findByEventoIdAndEstadoIgnoreCase(EVENTO_ID, "ACTIVA");
    }

    @Test
    @DisplayName("actualizarEvento: cambiar exclusivo sin inscripciones activas actualiza")
    void actualizarEvento_cambiaExclusivoSinInscripciones_actualiza() {
        dto.setExclusivo(true);
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(eventoActual(false)));
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(inscripcionEventoDAO.findByEventoIdAndEstadoIgnoreCase(EVENTO_ID, "ACTIVA")).thenReturn(List.of());
        when(eventoDAO.update(EVENTO_ID, dto)).thenReturn(Optional.of(dto));

        assertSame(dto, service.actualizarEvento(EVENTO_ID, dto));
    }

    @Test
    @DisplayName("actualizarEvento: cambiar exclusivo con inscripciones activas lanza BusinessValidationException")
    void actualizarEvento_cambiaExclusivoConInscripciones_lanzaBusinessValidation() {
        dto.setExclusivo(true);
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(eventoActual(false)));
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(inscripcionEventoDAO.findByEventoIdAndEstadoIgnoreCase(EVENTO_ID, "ACTIVA"))
                .thenReturn(List.of(new InscripcionEventoDTO()));

        assertThrows(BusinessValidationException.class, () -> service.actualizarEvento(EVENTO_ID, dto));

        verify(eventoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarEvento: si el DAO no devuelve el evento actualizado lanza ResourceNotFoundException")
    void actualizarEvento_daoNoActualiza_lanzaNotFound() {
        dto.setExclusivo(null);
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(eventoActual(false)));
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(eventoDAO.update(EVENTO_ID, dto)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarEvento(EVENTO_ID, dto));
    }

    @Test
    @DisplayName("actualizarEvento: cupoMaximo por debajo de los cupos reservados lanza BusinessValidationException")
    void actualizarEvento_cupoMaximoPorDebajoDeReservados_lanzaBusinessValidation() {
        dto.setCupoMaximo(5);
        dto.setCuposDisponibles(5);
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(eventoActual(false)));
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(inscripcionEventoDAO.findByEventoIdAndEstadoIgnoreCase(EVENTO_ID, "ACTIVA"))
                .thenReturn(List.of(inscripcion(1L, 5L, 10, "ACTIVA")));

        assertThrows(BusinessValidationException.class, () -> service.actualizarEvento(EVENTO_ID, dto));

        verify(eventoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarEvento: recalcula cuposDisponibles = cupoMaximo - cupos reservados (cupos nulos cuentan 1)")
    void actualizarEvento_recalculaCuposDisponibles() {
        dto.setExclusivo(null);
        dto.setCuposDisponibles(30); // lo que envia el cliente se ignora
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(eventoActual(false)));
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(inscripcionEventoDAO.findByEventoIdAndEstadoIgnoreCase(EVENTO_ID, "ACTIVA"))
                .thenReturn(List.of(inscripcion(1L, 5L, 4, "ACTIVA"), inscripcion(2L, 6L, null, "ACTIVA")));
        when(eventoDAO.update(EVENTO_ID, dto)).thenReturn(Optional.of(dto));

        EventoDTO resultado = service.actualizarEvento(EVENTO_ID, dto);

        assertEquals(25, resultado.getCuposDisponibles());
    }

    // ---------------------------------------------------------------- eliminarEvento (misma regla que cancelarEvento)

    @Test
    @DisplayName("eliminarEvento: evento programado sin inscripciones se elimina")
    void eliminarEvento_sinInscripciones_loElimina() {
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(eventoProgramado()));
        when(inscripcionEventoDAO.findByEventoId(EVENTO_ID)).thenReturn(List.of());

        service.eliminarEvento(EVENTO_ID);

        verify(eventoDAO).deleteById(EVENTO_ID);
        verify(eventoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("eliminarEvento: evento inexistente lanza ResourceNotFoundException")
    void eliminarEvento_inexistente_lanzaNotFound() {
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarEvento(EVENTO_ID));

        verify(eventoDAO, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminarEvento: evento que no esta PROGRAMADO lanza BusinessValidationException")
    void eliminarEvento_noProgramado_lanzaBusinessValidation() {
        EventoDTO evento = eventoProgramado();
        evento.setEstado("CANCELADO");
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(evento));

        assertThrows(BusinessValidationException.class, () -> service.eliminarEvento(EVENTO_ID));

        verify(eventoDAO, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminarEvento: con inscripciones cancela el evento, libera los cupos y notifica a los inscritos activos")
    void eliminarEvento_conInscripciones_cancelaYNotifica() {
        EventoDTO evento = eventoProgramado();
        InscripcionEventoDTO activa = inscripcion(100L, 5L, 2, "ACTIVA");
        InscripcionEventoDTO yaCancelada = inscripcion(101L, 6L, 1, "CANCELADA");
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(inscripcionEventoDAO.findByEventoId(EVENTO_ID)).thenReturn(List.of(activa, yaCancelada));
        when(eventoDAO.update(eq(EVENTO_ID), any(EventoDTO.class))).thenAnswer(inv -> Optional.of(inv.getArgument(1)));

        service.eliminarEvento(EVENTO_ID);

        assertEquals("CANCELADA", activa.getEstado());
        assertNotNull(activa.getFechaCancelacion());
        verify(inscripcionEventoDAO).update(100L, activa);
        verify(inscripcionEventoDAO, never()).update(eq(101L), any());

        ArgumentCaptor<NotificacionDTO> captor = ArgumentCaptor.forClass(NotificacionDTO.class);
        verify(notificacionDAO).save(captor.capture());
        assertEquals("EVENTO_CANCELADO", captor.getValue().getTipo());
        assertEquals(Long.valueOf(5L), captor.getValue().getUsuarioId());

        assertEquals("CANCELADO", evento.getEstado());
        assertEquals(30, evento.getCuposDisponibles());
        verify(eventoDAO, never()).deleteById(any());
    }

    // ---------------------------------------------------------------- cancelarEvento

    @Test
    @DisplayName("cancelarEvento: sin inscripciones elimina el evento y devuelve Optional vacio")
    void cancelarEvento_sinInscripciones_devuelveVacio() {
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(eventoProgramado()));
        when(inscripcionEventoDAO.findByEventoId(EVENTO_ID)).thenReturn(List.of());

        assertTrue(service.cancelarEvento(EVENTO_ID).isEmpty());

        verify(eventoDAO).deleteById(EVENTO_ID);
    }

    @Test
    @DisplayName("cancelarEvento: con inscripciones devuelve el evento CANCELADO")
    void cancelarEvento_conInscripciones_devuelveEventoCancelado() {
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(eventoProgramado()));
        when(inscripcionEventoDAO.findByEventoId(EVENTO_ID))
                .thenReturn(List.of(inscripcion(100L, 5L, 2, "ACTIVA")));
        when(eventoDAO.update(eq(EVENTO_ID), any(EventoDTO.class))).thenAnswer(inv -> Optional.of(inv.getArgument(1)));

        Optional<EventoDTO> resultado = service.cancelarEvento(EVENTO_ID);

        assertTrue(resultado.isPresent());
        assertEquals("CANCELADO", resultado.get().getEstado());
    }

    @Test
    @DisplayName("cancelarEvento: si el DAO no devuelve el evento actualizado lanza ResourceNotFoundException")
    void cancelarEvento_daoNoActualiza_lanzaNotFound() {
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(eventoProgramado()));
        when(inscripcionEventoDAO.findByEventoId(EVENTO_ID))
                .thenReturn(List.of(inscripcion(100L, 5L, 2, "ACTIVA")));
        when(eventoDAO.update(eq(EVENTO_ID), any(EventoDTO.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.cancelarEvento(EVENTO_ID));
    }

    // ---------------------------------------------------------------- buscarEventosProximos

    @Test
    @DisplayName("buscarEventosProximos: sin filtros devuelve todos los proximos programados")
    void buscarEventosProximos_sinFiltros_devuelveTodos() {
        LocalDateTime inicio = LocalDateTime.of(2026, 12, 1, 9, 0);
        stubProximos(List.of(proximo(1L, 7L, false, inicio), proximo(2L, 8L, true, inicio)));

        assertEquals(2, service.buscarEventosProximos(null, null, null).size());
    }

    @Test
    @DisplayName("buscarEventosProximos: filtra por sede")
    void buscarEventosProximos_filtraPorSede() {
        LocalDateTime inicio = LocalDateTime.of(2026, 12, 1, 9, 0);
        stubProximos(List.of(proximo(1L, 7L, false, inicio), proximo(2L, 8L, false, inicio)));

        List<EventoDTO> resultado = service.buscarEventosProximos(8L, null, null);

        assertEquals(1, resultado.size());
        assertEquals(Long.valueOf(2L), resultado.get(0).getId());
    }

    @Test
    @DisplayName("buscarEventosProximos: filtra por el dia de inicio")
    void buscarEventosProximos_filtraPorFecha() {
        stubProximos(List.of(
                proximo(1L, 7L, false, LocalDateTime.of(2026, 12, 1, 9, 0)),
                proximo(2L, 7L, false, LocalDateTime.of(2026, 12, 2, 9, 0))));

        List<EventoDTO> resultado = service.buscarEventosProximos(null, LocalDate.of(2026, 12, 2), null);

        assertEquals(1, resultado.size());
        assertEquals(Long.valueOf(2L), resultado.get(0).getId());
    }

    @Test
    @DisplayName("buscarEventosProximos: filtra por exclusivo y trata exclusivo nulo como false")
    void buscarEventosProximos_filtraPorExclusivo() {
        LocalDateTime inicio = LocalDateTime.of(2026, 12, 1, 9, 0);
        stubProximos(List.of(proximo(1L, 7L, null, inicio), proximo(2L, 7L, true, inicio)));

        assertEquals(Long.valueOf(2L), service.buscarEventosProximos(null, null, true).get(0).getId());
        assertEquals(Long.valueOf(1L), service.buscarEventosProximos(null, null, false).get(0).getId());
    }
}