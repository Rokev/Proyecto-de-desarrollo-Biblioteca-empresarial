package bussinesLayer.service.impl;

import bussinesLayer.dto.MultaDTO;
import bussinesLayer.dto.NotificacionDTO;
import bussinesLayer.dto.SancionDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.ConfiguracionSistemaService;
import bussinesLayer.util.ConceptoMulta;
import bussinesLayer.util.InfraccionSancion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.MultaDAO;
import persistanceLayer.dao.NotificacionDAO;
import persistanceLayer.dao.PrestamoDAO;
import persistanceLayer.dao.SancionDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.math.BigDecimal;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MultaServiceImpl")
class MultaServiceImplTest {

    private static final Long MULTA_ID = 1L;
    private static final Long USUARIO_ID = 10L;
    private static final Long PRESTAMO_ID = 20L;
    private static final BigDecimal MONTO = new BigDecimal("25000");

    @Mock private MultaDAO multaDAO;
    @Mock private UsuarioDAO usuarioDAO;
    @Mock private PrestamoDAO prestamoDAO;
    @Mock private NotificacionDAO notificacionDAO;
    @Mock private SancionDAO sancionDAO;
    @Mock private ConfiguracionSistemaService configuracionService;

    @InjectMocks
    private MultaServiceImpl service;

    private MultaDTO dto;

    @BeforeEach
    void setUp() {
        dto = new MultaDTO();
        dto.setMonto(MONTO);
        dto.setMotivo("Devolucion tardia");
        dto.setUsuarioId(USUARIO_ID);
    }

    private MultaDTO multaConId(Long id) {
        MultaDTO m = new MultaDTO();
        m.setId(id);
        return m;
    }

    // ---------------------------------------------------------------- crearMulta

    @Test
    @DisplayName("crearMulta: asigna estado PENDIENTE y fechaGeneracion cuando vienen vacios")
    void crearMulta_valida_aplicaDefaults() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(multaDAO.save(dto)).thenAnswer(inv -> {
            MultaDTO m = inv.getArgument(0);
            m.setId(MULTA_ID);
            return m;
        });

        MultaDTO resultado = service.crearMulta(dto);

        assertEquals(MULTA_ID, resultado.getId());
        assertEquals("PENDIENTE", resultado.getEstado());
        assertNotNull(resultado.getFechaGeneracion());
        verify(prestamoDAO, never()).existsById(any());
    }

    @Test
    @DisplayName("crearMulta: estado en blanco asigna PENDIENTE")
    void crearMulta_estadoEnBlanco_asignaPendiente() {
        dto.setEstado(" ");
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(multaDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("PENDIENTE", service.crearMulta(dto).getEstado());
    }

    @Test
    @DisplayName("crearMulta: conserva estado y fechaGeneracion si ya vienen informados, y valida el prestamo")
    void crearMulta_conEstadoFechaYPrestamo_losConserva() {
        LocalDateTime fecha = LocalDateTime.of(2026, 4, 1, 10, 0);
        dto.setEstado("PAGADA");
        dto.setFechaGeneracion(fecha);
        dto.setPrestamoId(PRESTAMO_ID);
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(prestamoDAO.existsById(PRESTAMO_ID)).thenReturn(true);
        when(multaDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        MultaDTO resultado = service.crearMulta(dto);

        assertEquals("PAGADA", resultado.getEstado());
        assertEquals(fecha, resultado.getFechaGeneracion());
    }

    @Test
    @DisplayName("crearMulta: usuario inexistente lanza ResourceNotFoundException")
    void crearMulta_usuarioInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearMulta(dto));

        verify(multaDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearMulta: prestamo inexistente lanza ResourceNotFoundException")
    void crearMulta_prestamoInexistente_lanzaNotFound() {
        dto.setPrestamoId(PRESTAMO_ID);
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(prestamoDAO.existsById(PRESTAMO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearMulta(dto));

        verify(multaDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerMultaPorId: devuelve la multa cuando existe")
    void obtenerMultaPorId_existente_laDevuelve() {
        MultaDTO existente = multaConId(MULTA_ID);
        when(multaDAO.findById(MULTA_ID)).thenReturn(Optional.of(existente));

        assertSame(existente, service.obtenerMultaPorId(MULTA_ID));
    }

    @Test
    @DisplayName("obtenerMultaPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerMultaPorId_inexistente_lanzaNotFound() {
        when(multaDAO.findById(MULTA_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerMultaPorId(MULTA_ID));
    }

    @Test
    @DisplayName("obtenerTodasLasMultas: delega en el DAO")
    void obtenerTodasLasMultas_devuelveLista() {
        when(multaDAO.findAll()).thenReturn(List.of(multaConId(1L), multaConId(2L)));

        assertEquals(2, service.obtenerTodasLasMultas().size());
    }

    // ---------------------------------------------------------------- actualizarMulta

    @Test
    @DisplayName("actualizarMulta: multa inexistente lanza ResourceNotFoundException")
    void actualizarMulta_inexistente_lanzaNotFound() {
        when(multaDAO.existsById(MULTA_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarMulta(MULTA_ID, dto));

        verify(multaDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarMulta: datos validos actualizan")
    void actualizarMulta_valida_actualiza() {
        when(multaDAO.existsById(MULTA_ID)).thenReturn(true);
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(multaDAO.update(MULTA_ID, dto)).thenReturn(Optional.of(dto));

        assertSame(dto, service.actualizarMulta(MULTA_ID, dto));
    }

    @Test
    @DisplayName("actualizarMulta: usuario inexistente lanza ResourceNotFoundException")
    void actualizarMulta_usuarioInexistente_lanzaNotFound() {
        when(multaDAO.existsById(MULTA_ID)).thenReturn(true);
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarMulta(MULTA_ID, dto));

        verify(multaDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarMulta: prestamo inexistente lanza ResourceNotFoundException")
    void actualizarMulta_prestamoInexistente_lanzaNotFound() {
        dto.setPrestamoId(PRESTAMO_ID);
        when(multaDAO.existsById(MULTA_ID)).thenReturn(true);
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(prestamoDAO.existsById(PRESTAMO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarMulta(MULTA_ID, dto));

        verify(multaDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarMulta: si el DAO no devuelve la multa actualizada lanza ResourceNotFoundException")
    void actualizarMulta_daoNoActualiza_lanzaNotFound() {
        when(multaDAO.existsById(MULTA_ID)).thenReturn(true);
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(multaDAO.update(MULTA_ID, dto)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarMulta(MULTA_ID, dto));
    }

    // ---------------------------------------------------------------- eliminarMulta

    @Test
    @DisplayName("eliminarMulta: elimina cuando la multa existe")
    void eliminarMulta_existente_elimina() {
        when(multaDAO.deleteById(MULTA_ID)).thenReturn(true);

        service.eliminarMulta(MULTA_ID);

        verify(multaDAO).deleteById(MULTA_ID);
    }

    @Test
    @DisplayName("eliminarMulta: multa inexistente lanza ResourceNotFoundException")
    void eliminarMulta_inexistente_lanzaNotFound() {
        when(multaDAO.deleteById(MULTA_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarMulta(MULTA_ID));
    }

    // ---------------------------------------------------------------- obtenerMultasPorUsuario

    @Test
    @DisplayName("obtenerMultasPorUsuario: usuario inexistente lanza ResourceNotFoundException")
    void obtenerMultasPorUsuario_usuarioInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerMultasPorUsuario(USUARIO_ID, null));
    }

    @Test
    @DisplayName("obtenerMultasPorUsuario: sin estado devuelve todas ordenadas por fecha")
    void obtenerMultasPorUsuario_sinEstado_devuelveTodas() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(multaDAO.findByUsuarioIdOrderByFechaGeneracionDesc(USUARIO_ID))
                .thenReturn(List.of(multaConId(1L), multaConId(2L)));

        assertEquals(2, service.obtenerMultasPorUsuario(USUARIO_ID, null).size());

        verify(multaDAO, never()).findByUsuarioIdAndEstadoIgnoreCase(any(), anyString());
    }

    @Test
    @DisplayName("obtenerMultasPorUsuario: estado en blanco devuelve todas")
    void obtenerMultasPorUsuario_estadoEnBlanco_devuelveTodas() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(multaDAO.findByUsuarioIdOrderByFechaGeneracionDesc(USUARIO_ID)).thenReturn(List.of(multaConId(1L)));

        assertEquals(1, service.obtenerMultasPorUsuario(USUARIO_ID, "  ").size());
    }

    @Test
    @DisplayName("obtenerMultasPorUsuario: con estado filtra por ese estado")
    void obtenerMultasPorUsuario_conEstado_filtra() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(multaDAO.findByUsuarioIdAndEstadoIgnoreCase(USUARIO_ID, "PENDIENTE"))
                .thenReturn(List.of(multaConId(1L)));

        assertEquals(1, service.obtenerMultasPorUsuario(USUARIO_ID, "PENDIENTE").size());

        verify(multaDAO, never()).findByUsuarioIdOrderByFechaGeneracionDesc(any());
    }

    // ---------------------------------------------------------------- obtenerSaldoPendienteUsuario

    @Test
    @DisplayName("obtenerSaldoPendienteUsuario: usuario inexistente lanza ResourceNotFoundException")
    void obtenerSaldoPendiente_usuarioInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerSaldoPendienteUsuario(USUARIO_ID));
    }

    @Test
    @DisplayName("obtenerSaldoPendienteUsuario: devuelve el total pendiente del DAO")
    void obtenerSaldoPendiente_existente_devuelveTotal() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(multaDAO.totalPendientePorUsuario(USUARIO_ID)).thenReturn(MONTO);

        assertEquals(MONTO, service.obtenerSaldoPendienteUsuario(USUARIO_ID));
    }

    // ---------------------------------------------------------------- generarMultaPorDanioOPerdida

    @Test
    @DisplayName("generarMultaPorDanioOPerdida: crea la multa PENDIENTE y notifica al usuario")
    void generarMulta_valida_creaMultaYNotifica() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(prestamoDAO.existsById(PRESTAMO_ID)).thenReturn(true);
        when(multaDAO.save(any(MultaDTO.class))).thenAnswer(inv -> {
            MultaDTO m = inv.getArgument(0);
            m.setId(MULTA_ID);
            return m;
        });

        MultaDTO resultado = service.generarMultaPorDanioOPerdida(USUARIO_ID, PRESTAMO_ID, "PERDIDO", MONTO,
                "Libro perdido");

        assertEquals(MULTA_ID, resultado.getId());
        assertEquals(USUARIO_ID, resultado.getUsuarioId());
        assertEquals(PRESTAMO_ID, resultado.getPrestamoId());
        assertEquals(MONTO, resultado.getMonto());
        assertEquals("Libro perdido", resultado.getMotivo());
        assertEquals("PENDIENTE", resultado.getEstado());
        assertNotNull(resultado.getFechaGeneracion());

        ArgumentCaptor<NotificacionDTO> captor = ArgumentCaptor.forClass(NotificacionDTO.class);
        verify(notificacionDAO).save(captor.capture());
        NotificacionDTO notif = captor.getValue();
        assertEquals(USUARIO_ID, notif.getUsuarioId());
        assertEquals("MULTA_GENERADA", notif.getTipo());
        assertEquals("APP", notif.getCanal());
        assertFalse(notif.getLeida());
        assertNotNull(notif.getFechaCreacion());
        assertTrue(notif.getMensaje().contains("25000"));
        assertTrue(notif.getMensaje().contains("Libro perdido"));
    }

    @Test
    @DisplayName("generarMultaPorDanioOPerdida: sin prestamo no consulta el prestamo")
    void generarMulta_sinPrestamo_noConsultaPrestamo() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(multaDAO.save(any(MultaDTO.class))).thenAnswer(inv -> inv.getArgument(0));

        service.generarMultaPorDanioOPerdida(USUARIO_ID, null, "DANADO", MONTO, "Libro danado");

        verify(prestamoDAO, never()).existsById(any());
    }

    @Test
    @DisplayName("generarMultaPorDanioOPerdida: motivo nulo usa un motivo por defecto")
    void generarMulta_motivoNulo_usaMotivoPorDefecto() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(multaDAO.save(any(MultaDTO.class))).thenAnswer(inv -> inv.getArgument(0));

        MultaDTO resultado = service.generarMultaPorDanioOPerdida(USUARIO_ID, null, "PERDIDO", MONTO, null);

        assertNotNull(resultado.getMotivo());
        assertTrue(resultado.getMotivo().startsWith("Multa por ejemplar"));
    }

    @Test
    @DisplayName("generarMultaPorDanioOPerdida: motivo en blanco usa un motivo por defecto")
    void generarMulta_motivoEnBlanco_usaMotivoPorDefecto() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(multaDAO.save(any(MultaDTO.class))).thenAnswer(inv -> inv.getArgument(0));

        MultaDTO resultado = service.generarMultaPorDanioOPerdida(USUARIO_ID, null, "PERDIDO", MONTO, "   ");

        assertTrue(resultado.getMotivo().startsWith("Multa por ejemplar"));
    }

    @Test
    @DisplayName("generarMultaPorDanioOPerdida: usuario inexistente lanza ResourceNotFoundException")
    void generarMulta_usuarioInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> service.generarMultaPorDanioOPerdida(USUARIO_ID, null, "PERDIDO", MONTO, "x"));

        verify(multaDAO, never()).save(any());
        verify(notificacionDAO, never()).save(any());
    }

    @Test
    @DisplayName("generarMultaPorDanioOPerdida: prestamo inexistente lanza ResourceNotFoundException")
    void generarMulta_prestamoInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(prestamoDAO.existsById(PRESTAMO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> service.generarMultaPorDanioOPerdida(USUARIO_ID, PRESTAMO_ID, "PERDIDO", MONTO, "x"));

        verify(multaDAO, never()).save(any());
    }

    @Test
    @DisplayName("generarMultaPorDanioOPerdida: tipo nulo lanza BusinessValidationException")
    void generarMulta_tipoNulo_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class,
                () -> service.generarMultaPorDanioOPerdida(USUARIO_ID, null, null, MONTO, "x"));

        verify(multaDAO, never()).save(any());
        verify(notificacionDAO, never()).save(any());
    }

    @Test
    @DisplayName("generarMultaPorDanioOPerdida: tipo distinto de DANADO o PERDIDO lanza BusinessValidationException")
    void generarMulta_tipoInvalido_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class,
                () -> service.generarMultaPorDanioOPerdida(USUARIO_ID, null, "ROBADO", MONTO, "x"));

        verify(multaDAO, never()).save(any());
    }

    @Test
    @DisplayName("generarMultaPorDanioOPerdida: el tipo no distingue mayusculas ni minusculas")
    void generarMulta_tipoEnMinusculas_esValido() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(multaDAO.save(any(MultaDTO.class))).thenAnswer(inv -> inv.getArgument(0));

        MultaDTO resultado = service.generarMultaPorDanioOPerdida(USUARIO_ID, null, "danado", MONTO, "x");

        assertEquals(MONTO, resultado.getMonto());
    }

    @Test
    @DisplayName("generarMultaPorDanioOPerdida: monto nulo y tipo PERDIDO cobra el monto configurado para libro perdido")
    void generarMulta_montoNuloPerdido_usaMontoConfigurado() {
        BigDecimal configurado = new BigDecimal("60000.00");
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(configuracionService.montoMulta(ConceptoMulta.LIBRO_PERDIDO)).thenReturn(configurado);
        when(multaDAO.save(any(MultaDTO.class))).thenAnswer(inv -> inv.getArgument(0));

        MultaDTO resultado = service.generarMultaPorDanioOPerdida(USUARIO_ID, null, "PERDIDO", null, "x");

        assertEquals(configurado, resultado.getMonto());
    }

    @Test
    @DisplayName("generarMultaPorDanioOPerdida: monto nulo y tipo DANADO cobra el monto configurado para libro danado")
    void generarMulta_montoNuloDanado_usaMontoConfigurado() {
        BigDecimal configurado = new BigDecimal("30000.00");
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(configuracionService.montoMulta(ConceptoMulta.LIBRO_DANADO)).thenReturn(configurado);
        when(multaDAO.save(any(MultaDTO.class))).thenAnswer(inv -> inv.getArgument(0));

        MultaDTO resultado = service.generarMultaPorDanioOPerdida(USUARIO_ID, null, "DANADO", null, "x");

        assertEquals(configurado, resultado.getMonto());
    }

    @Test
    @DisplayName("generarMultaPorDanioOPerdida: si la politica genera sancion, se guarda")
    void generarMulta_politicaGeneraSancion_laGuarda() {
        SancionDTO sancion = new SancionDTO();
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(prestamoDAO.existsById(PRESTAMO_ID)).thenReturn(true);
        when(multaDAO.save(any(MultaDTO.class))).thenAnswer(inv -> inv.getArgument(0));
        when(configuracionService.construirSancion(InfraccionSancion.LIBRO_PERDIDO, USUARIO_ID, PRESTAMO_ID,
                "Libro perdido (prestamo #" + PRESTAMO_ID + ")")).thenReturn(Optional.of(sancion));

        service.generarMultaPorDanioOPerdida(USUARIO_ID, PRESTAMO_ID, "PERDIDO", MONTO, "x");

        verify(sancionDAO).save(sancion);
    }

    @Test
    @DisplayName("generarMultaPorDanioOPerdida: si la politica no genera sancion, no se guarda ninguna")
    void generarMulta_politicaSinSancion_noGuardaSancion() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(multaDAO.save(any(MultaDTO.class))).thenAnswer(inv -> inv.getArgument(0));
        when(configuracionService.construirSancion(any(), any(), any(), anyString())).thenReturn(Optional.empty());

        service.generarMultaPorDanioOPerdida(USUARIO_ID, null, "DANADO", MONTO, "x");

        verify(sancionDAO, never()).save(any());
    }

    @Test
    @DisplayName("generarMultaPorDanioOPerdida: monto cero lanza BusinessValidationException")
    void generarMulta_montoCero_lanzaBusinessValidation() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);

        assertThrows(BusinessValidationException.class,
                () -> service.generarMultaPorDanioOPerdida(USUARIO_ID, null, "PERDIDO", BigDecimal.ZERO, "x"));

        verify(multaDAO, never()).save(any());
    }

    @Test
    @DisplayName("generarMultaPorDanioOPerdida: monto negativo lanza BusinessValidationException")
    void generarMulta_montoNegativo_lanzaBusinessValidation() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);

        assertThrows(BusinessValidationException.class,
                () -> service.generarMultaPorDanioOPerdida(USUARIO_ID, null, "PERDIDO", new BigDecimal("-1"), "x"));

        verify(multaDAO, never()).save(any());
        verify(notificacionDAO, never()).save(any());
    }
}