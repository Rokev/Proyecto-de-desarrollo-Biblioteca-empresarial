package bussinesLayer.service.impl;

import bussinesLayer.dto.SancionDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.PrestamoDAO;
import persistanceLayer.dao.SancionDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SancionServiceImpl")
class SancionServiceImplTest {

    private static final Long SANCION_ID = 1L;
    private static final Long USUARIO_ID = 10L;
    private static final Long PRESTAMO_ID = 20L;
    private static final String TIPO = "SUSPENSION";

    @Mock private SancionDAO sancionDAO;
    @Mock private UsuarioDAO usuarioDAO;
    @Mock private PrestamoDAO prestamoDAO;

    @InjectMocks
    private SancionServiceImpl service;

    private SancionDTO dto;

    @BeforeEach
    void setUp() {
        LocalDateTime aplicacion = LocalDateTime.of(2026, 3, 1, 8, 0);
        dto = new SancionDTO();
        dto.setTipo(TIPO);
        dto.setMotivo("Devolucion tardia");
        dto.setFechaAplicacion(aplicacion);
        dto.setFechaFin(aplicacion.plusDays(15));
        dto.setUsuarioId(USUARIO_ID);
    }

    private SancionDTO sancionConId(Long id) {
        SancionDTO s = new SancionDTO();
        s.setId(id);
        return s;
    }

    // ---------------------------------------------------------------- crearSancion

    @Test
    @DisplayName("crearSancion: sin estado asigna ACTIVA")
    void crearSancion_sinEstado_asignaActiva() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(sancionDAO.existsByUsuarioIdAndTipoIgnoreCaseAndEstadoIgnoreCase(USUARIO_ID, TIPO, "ACTIVA"))
                .thenReturn(false);
        when(sancionDAO.save(dto)).thenAnswer(inv -> {
            SancionDTO s = inv.getArgument(0);
            s.setId(SANCION_ID);
            return s;
        });

        SancionDTO resultado = service.crearSancion(dto);

        assertEquals(SANCION_ID, resultado.getId());
        assertEquals("ACTIVA", resultado.getEstado());
        verify(prestamoDAO, never()).existsById(any());
    }

    @Test
    @DisplayName("crearSancion: estado en blanco asigna ACTIVA")
    void crearSancion_estadoEnBlanco_asignaActiva() {
        dto.setEstado("  ");
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(sancionDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("ACTIVA", service.crearSancion(dto).getEstado());
    }

    @Test
    @DisplayName("crearSancion: conserva el estado si ya viene informado")
    void crearSancion_conEstado_loConserva() {
        dto.setEstado("FINALIZADA");
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(sancionDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("FINALIZADA", service.crearSancion(dto).getEstado());
    }

    @Test
    @DisplayName("crearSancion: con prestamo existente y sin fechaFin la crea")
    void crearSancion_conPrestamoSinFechaFin_crea() {
        dto.setPrestamoId(PRESTAMO_ID);
        dto.setFechaFin(null);
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(prestamoDAO.existsById(PRESTAMO_ID)).thenReturn(true);
        when(sancionDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        assertSame(dto, service.crearSancion(dto));
    }

    @Test
    @DisplayName("crearSancion: usuario inexistente lanza ResourceNotFoundException")
    void crearSancion_usuarioInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearSancion(dto));

        verify(sancionDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearSancion: prestamo inexistente lanza ResourceNotFoundException")
    void crearSancion_prestamoInexistente_lanzaNotFound() {
        dto.setPrestamoId(PRESTAMO_ID);
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(prestamoDAO.existsById(PRESTAMO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearSancion(dto));

        verify(sancionDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearSancion: fechaFin igual a fechaAplicacion lanza BusinessValidationException")
    void crearSancion_fechaFinIgual_lanzaBusinessValidation() {
        dto.setFechaFin(dto.getFechaAplicacion());
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearSancion(dto));

        verify(sancionDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearSancion: fechaFin anterior a fechaAplicacion lanza BusinessValidationException")
    void crearSancion_fechaFinAnterior_lanzaBusinessValidation() {
        dto.setFechaFin(dto.getFechaAplicacion().minusDays(1));
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearSancion(dto));

        verify(sancionDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearSancion: sancion activa del mismo tipo lanza BusinessValidationException")
    void crearSancion_activaDelMismoTipo_lanzaBusinessValidation() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(sancionDAO.existsByUsuarioIdAndTipoIgnoreCaseAndEstadoIgnoreCase(USUARIO_ID, TIPO, "ACTIVA"))
                .thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearSancion(dto));

        verify(sancionDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerSancionPorId: devuelve la sancion cuando existe")
    void obtenerSancionPorId_existente_laDevuelve() {
        SancionDTO existente = sancionConId(SANCION_ID);
        when(sancionDAO.findById(SANCION_ID)).thenReturn(Optional.of(existente));

        assertSame(existente, service.obtenerSancionPorId(SANCION_ID));
    }

    @Test
    @DisplayName("obtenerSancionPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerSancionPorId_inexistente_lanzaNotFound() {
        when(sancionDAO.findById(SANCION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerSancionPorId(SANCION_ID));
    }

    @Test
    @DisplayName("obtenerTodasLasSanciones: delega en el DAO")
    void obtenerTodasLasSanciones_devuelveLista() {
        when(sancionDAO.findAll()).thenReturn(List.of(sancionConId(1L), sancionConId(2L)));

        assertEquals(2, service.obtenerTodasLasSanciones().size());
    }

    // ---------------------------------------------------------------- actualizarSancion

    @Test
    @DisplayName("actualizarSancion: sancion inexistente lanza ResourceNotFoundException")
    void actualizarSancion_inexistente_lanzaNotFound() {
        when(sancionDAO.existsById(SANCION_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarSancion(SANCION_ID, dto));

        verify(sancionDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarSancion: datos validos actualizan")
    void actualizarSancion_valida_actualiza() {
        when(sancionDAO.existsById(SANCION_ID)).thenReturn(true);
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(sancionDAO.update(SANCION_ID, dto)).thenReturn(Optional.of(dto));

        assertSame(dto, service.actualizarSancion(SANCION_ID, dto));
    }

    @Test
    @DisplayName("actualizarSancion: usuario inexistente lanza ResourceNotFoundException")
    void actualizarSancion_usuarioInexistente_lanzaNotFound() {
        when(sancionDAO.existsById(SANCION_ID)).thenReturn(true);
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarSancion(SANCION_ID, dto));

        verify(sancionDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarSancion: prestamo inexistente lanza ResourceNotFoundException")
    void actualizarSancion_prestamoInexistente_lanzaNotFound() {
        dto.setPrestamoId(PRESTAMO_ID);
        when(sancionDAO.existsById(SANCION_ID)).thenReturn(true);
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(prestamoDAO.existsById(PRESTAMO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarSancion(SANCION_ID, dto));

        verify(sancionDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarSancion: fechaFin no posterior a fechaAplicacion lanza BusinessValidationException")
    void actualizarSancion_fechasInvalidas_lanzaBusinessValidation() {
        dto.setFechaFin(dto.getFechaAplicacion());
        when(sancionDAO.existsById(SANCION_ID)).thenReturn(true);
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.actualizarSancion(SANCION_ID, dto));

        verify(sancionDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarSancion: si el DAO no devuelve la sancion actualizada lanza ResourceNotFoundException")
    void actualizarSancion_daoNoActualiza_lanzaNotFound() {
        when(sancionDAO.existsById(SANCION_ID)).thenReturn(true);
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(sancionDAO.update(SANCION_ID, dto)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarSancion(SANCION_ID, dto));
    }

    // ---------------------------------------------------------------- eliminarSancion

    @Test
    @DisplayName("eliminarSancion: elimina cuando la sancion existe")
    void eliminarSancion_existente_elimina() {
        when(sancionDAO.deleteById(SANCION_ID)).thenReturn(true);

        service.eliminarSancion(SANCION_ID);

        verify(sancionDAO).deleteById(SANCION_ID);
    }

    @Test
    @DisplayName("eliminarSancion: sancion inexistente lanza ResourceNotFoundException")
    void eliminarSancion_inexistente_lanzaNotFound() {
        when(sancionDAO.deleteById(SANCION_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarSancion(SANCION_ID));
    }
}
