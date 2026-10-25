package bussinesLayer.service.impl;

import bussinesLayer.dto.ReservaSalaDTO;
import bussinesLayer.dto.SalaEstudioDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.ReservaSalaDAO;
import persistanceLayer.dao.SalaEstudioDAO;
import persistanceLayer.dao.SedeDAO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalaEstudioServiceImpl")
class SalaEstudioServiceImplTest {

    private static final Long SALA_ID = 1L;
    private static final Long SEDE_ID = 7L;
    private static final Long OTRA_SEDE_ID = 8L;

    @Mock private SalaEstudioDAO salaEstudioDAO;
    @Mock private SedeDAO sedeDAO;
    @Mock private ReservaSalaDAO reservaSalaDAO;

    @InjectMocks
    private SalaEstudioServiceImpl service;

    private SalaEstudioDTO dto;

    @BeforeEach
    void setUp() {
        dto = new SalaEstudioDTO();
        dto.setNombre("Sala A");
        dto.setCapacidad(6);
        dto.setDescripcion("Sala con tablero");
        dto.setTarifaPorHora(new BigDecimal("10000"));
        dto.setSedeId(SEDE_ID);
    }

    private SalaEstudioDTO salaActual(Long sedeId, String nombre) {
        SalaEstudioDTO s = new SalaEstudioDTO();
        s.setId(SALA_ID);
        s.setSedeId(sedeId);
        s.setNombre(nombre);
        return s;
    }

    // ---------------------------------------------------------------- crearSalaEstudio

    @Test
    @DisplayName("crearSalaEstudio: asigna activa=true cuando viene vacia")
    void crearSalaEstudio_valida_aplicaActivaPorDefecto() {
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(salaEstudioDAO.existsBySedeIdAndNombreIgnoreCase(SEDE_ID, "Sala A")).thenReturn(false);
        when(salaEstudioDAO.save(dto)).thenAnswer(inv -> {
            SalaEstudioDTO s = inv.getArgument(0);
            s.setId(SALA_ID);
            return s;
        });

        SalaEstudioDTO resultado = service.crearSalaEstudio(dto);

        assertEquals(SALA_ID, resultado.getId());
        assertTrue(resultado.getActiva());
    }

    @Test
    @DisplayName("crearSalaEstudio: conserva activa=false si ya viene informada")
    void crearSalaEstudio_conActivaFalse_laConserva() {
        dto.setActiva(false);
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(salaEstudioDAO.existsBySedeIdAndNombreIgnoreCase(SEDE_ID, "Sala A")).thenReturn(false);
        when(salaEstudioDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        assertFalse(service.crearSalaEstudio(dto).getActiva());
    }

    @Test
    @DisplayName("crearSalaEstudio: sede inexistente lanza ResourceNotFoundException")
    void crearSalaEstudio_sedeInexistente_lanzaNotFound() {
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearSalaEstudio(dto));

        verify(salaEstudioDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearSalaEstudio: capacidad menor a 1 lanza BusinessValidationException")
    void crearSalaEstudio_capacidadInvalida_lanzaBusinessValidation() {
        dto.setCapacidad(0);
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearSalaEstudio(dto));

        verify(salaEstudioDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearSalaEstudio: nombre repetido en la misma sede lanza BusinessValidationException")
    void crearSalaEstudio_nombreDuplicadoEnSede_lanzaBusinessValidation() {
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(salaEstudioDAO.existsBySedeIdAndNombreIgnoreCase(SEDE_ID, "Sala A")).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearSalaEstudio(dto));

        verify(salaEstudioDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerSalaEstudioPorId: devuelve la sala cuando existe")
    void obtenerSalaEstudioPorId_existente_laDevuelve() {
        SalaEstudioDTO existente = salaActual(SEDE_ID, "Sala A");
        when(salaEstudioDAO.findById(SALA_ID)).thenReturn(Optional.of(existente));

        assertSame(existente, service.obtenerSalaEstudioPorId(SALA_ID));
    }

    @Test
    @DisplayName("obtenerSalaEstudioPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerSalaEstudioPorId_inexistente_lanzaNotFound() {
        when(salaEstudioDAO.findById(SALA_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerSalaEstudioPorId(SALA_ID));
    }

    @Test
    @DisplayName("obtenerTodasLasSalasEstudio: delega en el DAO")
    void obtenerTodasLasSalasEstudio_devuelveLista() {
        when(salaEstudioDAO.findAll()).thenReturn(List.of(
                salaActual(SEDE_ID, "Sala A"), salaActual(SEDE_ID, "Sala B")));

        assertEquals(2, service.obtenerTodasLasSalasEstudio().size());
    }

    // ---------------------------------------------------------------- actualizarSalaEstudio

    @Test
    @DisplayName("actualizarSalaEstudio: sala inexistente lanza ResourceNotFoundException")
    void actualizarSalaEstudio_inexistente_lanzaNotFound() {
        when(salaEstudioDAO.findById(SALA_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarSalaEstudio(SALA_ID, dto));

        verify(salaEstudioDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarSalaEstudio: sede inexistente lanza ResourceNotFoundException")
    void actualizarSalaEstudio_sedeInexistente_lanzaNotFound() {
        when(salaEstudioDAO.findById(SALA_ID)).thenReturn(Optional.of(salaActual(SEDE_ID, "Sala A")));
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarSalaEstudio(SALA_ID, dto));

        verify(salaEstudioDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarSalaEstudio: capacidad menor a 1 lanza BusinessValidationException")
    void actualizarSalaEstudio_capacidadInvalida_lanzaBusinessValidation() {
        dto.setCapacidad(0);
        when(salaEstudioDAO.findById(SALA_ID)).thenReturn(Optional.of(salaActual(SEDE_ID, "Sala A")));
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.actualizarSalaEstudio(SALA_ID, dto));

        verify(salaEstudioDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarSalaEstudio: misma sede y mismo nombre (sin importar mayusculas) no valida duplicado")
    void actualizarSalaEstudio_mismaClave_actualizaSinValidarDuplicado() {
        when(salaEstudioDAO.findById(SALA_ID)).thenReturn(Optional.of(salaActual(SEDE_ID, "SALA A")));
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(salaEstudioDAO.update(SALA_ID, dto)).thenReturn(Optional.of(dto));

        assertSame(dto, service.actualizarSalaEstudio(SALA_ID, dto));

        verify(salaEstudioDAO, never()).existsBySedeIdAndNombreIgnoreCase(any(), any());
    }

    @Test
    @DisplayName("actualizarSalaEstudio: nombre nuevo y libre en la sede actualiza")
    void actualizarSalaEstudio_nombreNuevoLibre_actualiza() {
        when(salaEstudioDAO.findById(SALA_ID)).thenReturn(Optional.of(salaActual(SEDE_ID, "Otra sala")));
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(salaEstudioDAO.existsBySedeIdAndNombreIgnoreCase(SEDE_ID, "Sala A")).thenReturn(false);
        when(salaEstudioDAO.update(SALA_ID, dto)).thenReturn(Optional.of(dto));

        assertSame(dto, service.actualizarSalaEstudio(SALA_ID, dto));
    }

    @Test
    @DisplayName("actualizarSalaEstudio: nombre nuevo que ya existe en la sede lanza BusinessValidationException")
    void actualizarSalaEstudio_nombreDuplicado_lanzaBusinessValidation() {
        when(salaEstudioDAO.findById(SALA_ID)).thenReturn(Optional.of(salaActual(SEDE_ID, "Otra sala")));
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(salaEstudioDAO.existsBySedeIdAndNombreIgnoreCase(SEDE_ID, "Sala A")).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.actualizarSalaEstudio(SALA_ID, dto));

        verify(salaEstudioDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarSalaEstudio: cambiar de sede con el mismo nombre valida el duplicado en la nueva sede")
    void actualizarSalaEstudio_cambioDeSede_validaDuplicadoEnNuevaSede() {
        when(salaEstudioDAO.findById(SALA_ID)).thenReturn(Optional.of(salaActual(OTRA_SEDE_ID, "Sala A")));
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(salaEstudioDAO.existsBySedeIdAndNombreIgnoreCase(SEDE_ID, "Sala A")).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.actualizarSalaEstudio(SALA_ID, dto));

        verify(salaEstudioDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarSalaEstudio: si el DAO no devuelve la sala actualizada lanza ResourceNotFoundException")
    void actualizarSalaEstudio_daoNoActualiza_lanzaNotFound() {
        when(salaEstudioDAO.findById(SALA_ID)).thenReturn(Optional.of(salaActual(SEDE_ID, "Sala A")));
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(salaEstudioDAO.update(SALA_ID, dto)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarSalaEstudio(SALA_ID, dto));
    }

    // ---------------------------------------------------------------- eliminarSalaEstudio

    @Test
    @DisplayName("eliminarSalaEstudio: elimina cuando existe, sin reservas activas ni historial")
    void eliminarSalaEstudio_existente_elimina() {
        when(salaEstudioDAO.existsById(SALA_ID)).thenReturn(true);
        when(reservaSalaDAO.existsBySalaIdAndEstadoInAndFechaFinAfter(
                eq(SALA_ID), anyCollection(), any(LocalDateTime.class))).thenReturn(false);
        when(reservaSalaDAO.findBySalaId(SALA_ID)).thenReturn(List.of());
        when(salaEstudioDAO.deleteById(SALA_ID)).thenReturn(true);

        service.eliminarSalaEstudio(SALA_ID);

        verify(salaEstudioDAO).deleteById(SALA_ID);
    }

    @Test
    @DisplayName("eliminarSalaEstudio: sala inexistente lanza ResourceNotFoundException")
    void eliminarSalaEstudio_inexistente_lanzaNotFound() {
        when(salaEstudioDAO.existsById(SALA_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarSalaEstudio(SALA_ID));

        verify(salaEstudioDAO, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminarSalaEstudio: con reservas activas lanza BusinessValidationException")
    void eliminarSalaEstudio_conReservasActivas_lanzaBusinessValidation() {
        when(salaEstudioDAO.existsById(SALA_ID)).thenReturn(true);
        when(reservaSalaDAO.existsBySalaIdAndEstadoInAndFechaFinAfter(
                eq(SALA_ID), anyCollection(), any(LocalDateTime.class))).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.eliminarSalaEstudio(SALA_ID));

        verify(salaEstudioDAO, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminarSalaEstudio: con historial de reservas lanza BusinessValidationException")
    void eliminarSalaEstudio_conHistorial_lanzaBusinessValidation() {
        when(salaEstudioDAO.existsById(SALA_ID)).thenReturn(true);
        when(reservaSalaDAO.existsBySalaIdAndEstadoInAndFechaFinAfter(
                eq(SALA_ID), anyCollection(), any(LocalDateTime.class))).thenReturn(false);
        when(reservaSalaDAO.findBySalaId(SALA_ID)).thenReturn(List.of(new ReservaSalaDTO()));

        assertThrows(BusinessValidationException.class, () -> service.eliminarSalaEstudio(SALA_ID));

        verify(salaEstudioDAO, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminarSalaEstudio: si el DAO no la elimina lanza ResourceNotFoundException")
    void eliminarSalaEstudio_daoNoElimina_lanzaNotFound() {
        when(salaEstudioDAO.existsById(SALA_ID)).thenReturn(true);
        when(reservaSalaDAO.findBySalaId(SALA_ID)).thenReturn(List.of());
        when(salaEstudioDAO.deleteById(SALA_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarSalaEstudio(SALA_ID));
    }
}