package bussinesLayer.service.impl;

import bussinesLayer.dto.EventoDTO;
import bussinesLayer.dto.InscripcionEventoDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.EventoDAO;
import persistanceLayer.dao.InscripcionEventoDAO;
import persistanceLayer.dao.SedeDAO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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
    @DisplayName("actualizarEvento: sin indicar exclusivo actualiza sin consultar inscripciones")
    void actualizarEvento_sinExclusivo_actualizaSinConsultarInscripciones() {
        dto.setExclusivo(null);
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(eventoActual(false)));
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(eventoDAO.update(EVENTO_ID, dto)).thenReturn(Optional.of(dto));

        assertSame(dto, service.actualizarEvento(EVENTO_ID, dto));

        verify(inscripcionEventoDAO, never()).findByEventoIdAndEstadoIgnoreCase(any(), anyString());
    }

    @Test
    @DisplayName("actualizarEvento: mismo valor de exclusivo actualiza sin consultar inscripciones")
    void actualizarEvento_mismoExclusivo_actualizaSinConsultarInscripciones() {
        dto.setExclusivo(true);
        when(eventoDAO.findById(EVENTO_ID)).thenReturn(Optional.of(eventoActual(true)));
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(eventoDAO.update(EVENTO_ID, dto)).thenReturn(Optional.of(dto));

        assertSame(dto, service.actualizarEvento(EVENTO_ID, dto));

        verify(inscripcionEventoDAO, never()).findByEventoIdAndEstadoIgnoreCase(any(), anyString());
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

    // ---------------------------------------------------------------- eliminarEvento

    @Test
    @DisplayName("eliminarEvento: elimina cuando el evento existe")
    void eliminarEvento_existente_elimina() {
        when(eventoDAO.deleteById(EVENTO_ID)).thenReturn(true);

        service.eliminarEvento(EVENTO_ID);

        verify(eventoDAO).deleteById(EVENTO_ID);
    }

    @Test
    @DisplayName("eliminarEvento: evento inexistente lanza ResourceNotFoundException")
    void eliminarEvento_inexistente_lanzaNotFound() {
        when(eventoDAO.deleteById(EVENTO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarEvento(EVENTO_ID));
    }
}
