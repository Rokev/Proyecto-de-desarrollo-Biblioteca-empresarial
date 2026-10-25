package bussinesLayer.service.impl;

import bussinesLayer.dto.EjemplarDTO;
import bussinesLayer.dto.EventoDTO;
import bussinesLayer.dto.InscripcionEventoDTO;
import bussinesLayer.dto.LibroDTO;
import bussinesLayer.dto.MultaDTO;
import bussinesLayer.dto.NotificacionDTO;
import bussinesLayer.dto.PedidoInterciudadDTO;
import bussinesLayer.dto.PrestamoDTO;
import bussinesLayer.dto.ReservaLibroDTO;
import bussinesLayer.dto.ReservaSalaDTO;
import bussinesLayer.dto.SalaEstudioDTO;
import bussinesLayer.dto.SancionDTO;
import bussinesLayer.service.ConfiguracionSistemaService;
import bussinesLayer.service.NotificacionService;
import bussinesLayer.service.ReservaLibroService;
import bussinesLayer.util.ClavePlazo;
import bussinesLayer.util.ConceptoMulta;
import bussinesLayer.util.InfraccionSancion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import persistanceLayer.dao.EjemplarDAO;
import persistanceLayer.dao.EventoDAO;
import persistanceLayer.dao.InscripcionEventoDAO;
import persistanceLayer.dao.LibroDAO;
import persistanceLayer.dao.MultaDAO;
import persistanceLayer.dao.NotificacionDAO;
import persistanceLayer.dao.PedidoInterciudadDAO;
import persistanceLayer.dao.PrestamoDAO;
import persistanceLayer.dao.ReservaLibroDAO;
import persistanceLayer.dao.ReservaSalaDAO;
import persistanceLayer.dao.SalaEstudioDAO;
import persistanceLayer.dao.SancionDAO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutomatizacionServiceImpl")
class AutomatizacionServiceImplTest {

    private static final Long USUARIO_ID = 1L;
    private static final Long PRESTAMO_ID = 10L;
    private static final Long OTRO_PRESTAMO_ID = 11L;
    private static final Long EJEMPLAR_ID = 20L;
    private static final Long LIBRO_ID = 30L;
    private static final Long RESERVA_ID = 40L;
    private static final Long SANCION_ID = 50L;
    private static final Long MULTA_ID = 60L;
    private static final Long PEDIDO_ID = 70L;
    private static final Long SEDE_ORIGEN_ID = 5L;
    private static final Long SALA_ID = 8L;
    private static final Long RESERVA_SALA_ID = 90L;
    private static final Long OTRA_RESERVA_SALA_ID = 91L;
    private static final Long EVENTO_ID = 3L;

    @Mock private PrestamoDAO prestamoDAO;
    @Mock private ReservaLibroDAO reservaLibroDAO;
    @Mock private ReservaSalaDAO reservaSalaDAO;
    @Mock private SalaEstudioDAO salaEstudioDAO;
    @Mock private EventoDAO eventoDAO;
    @Mock private InscripcionEventoDAO inscripcionEventoDAO;
    @Mock private SancionDAO sancionDAO;
    @Mock private EjemplarDAO ejemplarDAO;
    @Mock private LibroDAO libroDAO;
    @Mock private MultaDAO multaDAO;
    @Mock private NotificacionDAO notificacionDAO;
    @Mock private PedidoInterciudadDAO pedidoInterciudadDAO;
    @Mock private ReservaLibroService reservaLibroService;
    @Mock private NotificacionService notificacionService;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private ConfiguracionSistemaService configuracionService;

    @InjectMocks
    private AutomatizacionServiceImpl service;

    // ------------------------------------------------------------ helpers

    private PrestamoDTO prestamo(Long id, String estado, LocalDate fechaVencimiento) {
        PrestamoDTO p = new PrestamoDTO();
        p.setId(id);
        p.setUsuarioId(USUARIO_ID);
        p.setEjemplarId(EJEMPLAR_ID);
        p.setEstado(estado);
        p.setFechaVencimiento(fechaVencimiento);
        p.setModalidad("SEDE");
        p.setFechaSolicitud(LocalDateTime.now().minusHours(1));
        return p;
    }

    private PrestamoDTO prestamo(String estado, LocalDate fechaVencimiento) {
        return prestamo(PRESTAMO_ID, estado, fechaVencimiento);
    }

    private PrestamoDTO solicitud(String modalidad, LocalDateTime fechaSolicitud) {
        PrestamoDTO p = prestamo("SOLICITADO", LocalDate.now().plusDays(14));
        p.setModalidad(modalidad);
        p.setFechaSolicitud(fechaSolicitud);
        return p;
    }

    private MultaDTO multa(Long id, BigDecimal monto, String motivo, String estado) {
        MultaDTO m = new MultaDTO();
        m.setId(id);
        m.setMonto(monto);
        m.setMotivo(motivo);
        m.setEstado(estado);
        m.setPrestamoId(PRESTAMO_ID);
        m.setUsuarioId(USUARIO_ID);
        return m;
    }

    private SancionDTO sancion(String estado, String motivo, LocalDateTime fechaFin, Long prestamoId) {
        SancionDTO s = new SancionDTO();
        s.setId(SANCION_ID);
        s.setTipo(InfraccionSancion.TipoSancion.BLOQUEO_PRESTAMOS);
        s.setEstado(estado);
        s.setMotivo(motivo);
        s.setFechaFin(fechaFin);
        s.setUsuarioId(USUARIO_ID);
        s.setPrestamoId(prestamoId);
        return s;
    }

    private ReservaLibroDTO reservaLibro(String estado, LocalDateTime fechaLimiteRetiro, Long ejemplarId) {
        ReservaLibroDTO r = new ReservaLibroDTO();
        r.setId(RESERVA_ID);
        r.setUsuarioId(USUARIO_ID);
        r.setLibroId(LIBRO_ID);
        r.setEjemplarId(ejemplarId);
        r.setEstado(estado);
        r.setFechaLimiteRetiro(fechaLimiteRetiro);
        return r;
    }

    private ReservaSalaDTO reservaSala(Long id, String estado) {
        ReservaSalaDTO r = new ReservaSalaDTO();
        r.setId(id);
        r.setUsuarioId(USUARIO_ID);
        r.setSalaId(SALA_ID);
        r.setEstado(estado);
        r.setFechaInicio(LocalDateTime.now().plusHours(1));
        r.setFechaFin(LocalDateTime.now().plusHours(2));
        r.setFechaCreacion(LocalDateTime.now());
        return r;
    }

    private PedidoInterciudadDTO pedido(String estado) {
        PedidoInterciudadDTO p = new PedidoInterciudadDTO();
        p.setId(PEDIDO_ID);
        p.setEstado(estado);
        p.setUsuarioId(USUARIO_ID);
        p.setPrestamoId(PRESTAMO_ID);
        p.setEjemplarId(EJEMPLAR_ID);
        p.setSedeOrigenId(SEDE_ORIGEN_ID);
        return p;
    }

    private EjemplarDTO ejemplar() {
        EjemplarDTO e = new EjemplarDTO();
        e.setId(EJEMPLAR_ID);
        e.setLibroId(LIBRO_ID);
        return e;
    }

    private LibroDTO libro(String titulo) {
        LibroDTO l = new LibroDTO();
        l.setId(LIBRO_ID);
        l.setTitulo(titulo);
        return l;
    }

    /** Para las notificaciones que incluyen el titulo del libro a partir del ejemplar. */
    private void stubTituloDeEjemplar() {
        when(ejemplarDAO.findById(EJEMPLAR_ID)).thenReturn(Optional.of(ejemplar()));
        when(libroDAO.findById(LIBRO_ID)).thenReturn(Optional.of(libro("Dune")));
    }

    private void stubMontoMora() {
        when(configuracionService.montoMulta(ConceptoMulta.MORA_POR_DIA)).thenReturn(new BigDecimal("2000.00"));
    }

    /** Devuelve las sanciones indicadas solo para el tipo BLOQUEO_PRESTAMOS (el servicio consulta los 3 tipos). */
    private void stubSancionesActivas(List<SancionDTO> activas) {
        when(sancionDAO.findByTipoIgnoreCaseAndEstadoIgnoreCase(anyString(), eq("ACTIVA")))
                .thenAnswer(inv -> InfraccionSancion.TipoSancion.BLOQUEO_PRESTAMOS.equals(inv.getArgument(0))
                        ? activas : List.of());
    }

    /** El servicio consulta primero PENDIENTE_PAGO y luego CONFIRMADA. */
    private void stubReservasSala(List<ReservaSalaDTO> pendientes, List<ReservaSalaDTO> confirmadas) {
        when(reservaSalaDAO.findByEstadoIgnoreCase("PENDIENTE_PAGO")).thenReturn(pendientes);
        when(reservaSalaDAO.findByEstadoIgnoreCase("CONFIRMADA")).thenReturn(confirmadas);
    }

    /** El servicio consulta primero los que vencen en 2 dias y luego los que vencen en 1. */
    private void stubVencimientos(List<PrestamoDTO> en48h, List<PrestamoDTO> en24h) {
        when(prestamoDAO.findByEstadoAndFechaVencimiento("ACTIVO", LocalDate.now().plusDays(2))).thenReturn(en48h);
        when(prestamoDAO.findByEstadoAndFechaVencimiento("ACTIVO", LocalDate.now().plusDays(1))).thenReturn(en24h);
    }

    private NotificacionDTO notificacionEnviada() {
        ArgumentCaptor<NotificacionDTO> captor = ArgumentCaptor.forClass(NotificacionDTO.class);
        verify(notificacionDAO).save(captor.capture());
        return captor.getValue();
    }

    // ---------------------------------------------------------------- procesarPrestamosVencidos

    @Test
    @DisplayName("procesarPrestamosVencidos: sin candidatos devuelve 0")
    void procesarVencidos_sinCandidatos_devuelveCero() {
        when(prestamoDAO.findByEstadoIgnoreCaseAndFechaVencimientoBefore(eq("ACTIVO"), any(LocalDate.class)))
                .thenReturn(List.of());

        assertEquals(0, service.procesarPrestamosVencidos());

        verify(prestamoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("procesarPrestamosVencidos: marca VENCIDO, genera multa y sancion por mora y notifica")
    void procesarVencidos_activoVencido_aplicaMultaSancionYNotifica() {
        PrestamoDTO prestamo = prestamo("ACTIVO", LocalDate.now().minusDays(3));
        SancionDTO sancionNueva = sancion("ACTIVA", "Retraso en la devolucion (prestamo #10)", null, PRESTAMO_ID);
        when(prestamoDAO.findByEstadoIgnoreCaseAndFechaVencimientoBefore(eq("ACTIVO"), any(LocalDate.class)))
                .thenReturn(List.of(prestamo));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo));
        stubMontoMora();
        when(configuracionService.construirSancion(eq(InfraccionSancion.MORA_DEVOLUCION), eq(USUARIO_ID),
                eq(PRESTAMO_ID), anyString())).thenReturn(Optional.of(sancionNueva));
        stubTituloDeEjemplar();

        int procesados = service.procesarPrestamosVencidos();

        assertEquals(1, procesados);
        assertEquals("VENCIDO", prestamo.getEstado());
        verify(prestamoDAO).update(PRESTAMO_ID, prestamo);

        ArgumentCaptor<MultaDTO> multaCaptor = ArgumentCaptor.forClass(MultaDTO.class);
        verify(multaDAO).save(multaCaptor.capture());
        MultaDTO multa = multaCaptor.getValue();
        assertEquals(0, new BigDecimal("6000.00").compareTo(multa.getMonto()));
        assertEquals("PENDIENTE", multa.getEstado());
        assertEquals(USUARIO_ID, multa.getUsuarioId());
        assertEquals(PRESTAMO_ID, multa.getPrestamoId());
        assertTrue(multa.getMotivo().startsWith("Mora en la devolucion"));
        assertTrue(multa.getMotivo().contains("3 dias de retraso"));

        verify(sancionDAO).save(sancionNueva);

        NotificacionDTO n = notificacionEnviada();
        assertEquals("PRESTAMO_VENCIDO", n.getTipo());
        assertEquals(USUARIO_ID, n.getUsuarioId());
        assertFalse(n.getLeida());
        assertTrue(n.getMensaje().contains("Dune"));
        assertTrue(n.getMensaje().contains("no podras pedir mas libros"));
    }

    @Test
    @DisplayName("procesarPrestamosVencidos: si la politica no genera sancion no guarda ninguna")
    void procesarVencidos_sinSancionConfigurada_noGuardaSancion() {
        PrestamoDTO prestamo = prestamo("ACTIVO", LocalDate.now().minusDays(1));
        when(prestamoDAO.findByEstadoIgnoreCaseAndFechaVencimientoBefore(eq("ACTIVO"), any(LocalDate.class)))
                .thenReturn(List.of(prestamo));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo));
        stubMontoMora();
        when(configuracionService.construirSancion(any(), any(), any(), any())).thenReturn(Optional.empty());
        stubTituloDeEjemplar();

        assertEquals(1, service.procesarPrestamosVencidos());

        verify(sancionDAO, never()).save(any());
        NotificacionDTO n = notificacionEnviada();
        assertTrue(n.getMensaje().contains("Debes devolverlo y pagar la multa"));
        ArgumentCaptor<MultaDTO> multaCaptor = ArgumentCaptor.forClass(MultaDTO.class);
        verify(multaDAO).save(multaCaptor.capture());
        assertTrue(multaCaptor.getValue().getMotivo().contains("1 dia de retraso"));
    }

    @Test
    @DisplayName("procesarPrestamosVencidos: si ya tiene sancion por mora no crea otra")
    void procesarVencidos_yaTieneSancionPorMora_noLaDuplica() {
        PrestamoDTO prestamo = prestamo("ACTIVO", LocalDate.now().minusDays(2));
        when(prestamoDAO.findByEstadoIgnoreCaseAndFechaVencimientoBefore(eq("ACTIVO"), any(LocalDate.class)))
                .thenReturn(List.of(prestamo));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo));
        stubMontoMora();
        when(sancionDAO.findByPrestamoId(PRESTAMO_ID))
                .thenReturn(List.of(sancion("ACTIVA", "Retraso en la devolucion (prestamo #10)", null, PRESTAMO_ID)));
        stubTituloDeEjemplar();

        assertEquals(1, service.procesarPrestamosVencidos());

        verify(configuracionService, never()).construirSancion(any(), any(), any(), any());
        verify(sancionDAO, never()).save(any());
        assertTrue(notificacionEnviada().getMensaje().contains("no podras pedir mas libros"));
    }

    @Test
    @DisplayName("procesarPrestamosVencidos: si al releerlo ya no esta ACTIVO no hace nada")
    void procesarVencidos_yaNoEstaActivo_noLoProcesa() {
        when(prestamoDAO.findByEstadoIgnoreCaseAndFechaVencimientoBefore(eq("ACTIVO"), any(LocalDate.class)))
                .thenReturn(List.of(prestamo("ACTIVO", LocalDate.now().minusDays(3))));
        when(prestamoDAO.findById(PRESTAMO_ID))
                .thenReturn(Optional.of(prestamo("DEVUELTO", LocalDate.now().minusDays(3))));

        assertEquals(0, service.procesarPrestamosVencidos());

        verify(prestamoDAO, never()).update(any(), any());
        verifyNoInteractions(multaDAO, notificacionDAO);
    }

    @Test
    @DisplayName("procesarPrestamosVencidos: si el prestamo desaparecio no cuenta como procesado")
    void procesarVencidos_prestamoDesaparecio_noLoCuenta() {
        when(prestamoDAO.findByEstadoIgnoreCaseAndFechaVencimientoBefore(eq("ACTIVO"), any(LocalDate.class)))
                .thenReturn(List.of(prestamo("ACTIVO", LocalDate.now().minusDays(3))));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.empty());

        assertEquals(0, service.procesarPrestamosVencidos());
    }

    @Test
    @DisplayName("procesarPrestamosVencidos: un error en un prestamo hace rollback y no detiene a los demas")
    void procesarVencidos_falloEnUno_continuaConElSiguiente() {
        PrestamoDTO fallido = prestamo(PRESTAMO_ID, "ACTIVO", LocalDate.now().minusDays(2));
        PrestamoDTO bueno = prestamo(OTRO_PRESTAMO_ID, "ACTIVO", LocalDate.now().minusDays(2));
        when(prestamoDAO.findByEstadoIgnoreCaseAndFechaVencimientoBefore(eq("ACTIVO"), any(LocalDate.class)))
                .thenReturn(List.of(fallido, bueno));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenThrow(new RuntimeException("falla simulada"));
        when(prestamoDAO.findById(OTRO_PRESTAMO_ID)).thenReturn(Optional.of(bueno));
        stubMontoMora();
        when(configuracionService.construirSancion(any(), any(), any(), any())).thenReturn(Optional.empty());
        stubTituloDeEjemplar();

        assertEquals(1, service.procesarPrestamosVencidos());

        assertEquals("VENCIDO", bueno.getEstado());
        verify(transactionManager).rollback(any());
        verify(transactionManager).commit(any());
    }

    // ---------------------------------------------------------------- evaluarRetrasoAlDevolver

    @Test
    @DisplayName("evaluarRetrasoAlDevolver: prestamo inexistente devuelve 0")
    void evaluarRetraso_prestamoInexistente_devuelveCero() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.empty());

        assertEquals(0, service.evaluarRetrasoAlDevolver(PRESTAMO_ID));

        verifyNoInteractions(multaDAO);
    }

    @Test
    @DisplayName("evaluarRetrasoAlDevolver: si se devuelve a tiempo devuelve 0 y no genera multa")
    void evaluarRetraso_aTiempo_devuelveCero() {
        when(prestamoDAO.findById(PRESTAMO_ID))
                .thenReturn(Optional.of(prestamo("ACTIVO", LocalDate.now().plusDays(1))));

        assertEquals(0, service.evaluarRetrasoAlDevolver(PRESTAMO_ID));

        verifyNoInteractions(multaDAO);
    }

    @Test
    @DisplayName("evaluarRetrasoAlDevolver: sin fecha de vencimiento devuelve 0")
    void evaluarRetraso_sinFechaVencimiento_devuelveCero() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo("ACTIVO", null)));

        assertEquals(0, service.evaluarRetrasoAlDevolver(PRESTAMO_ID));

        verifyNoInteractions(multaDAO);
    }

    @Test
    @DisplayName("evaluarRetrasoAlDevolver: un prestamo en estado no evaluable devuelve 0")
    void evaluarRetraso_estadoNoEvaluable_devuelveCero() {
        when(prestamoDAO.findById(PRESTAMO_ID))
                .thenReturn(Optional.of(prestamo("DEVUELTO", LocalDate.now().minusDays(5))));

        assertEquals(0, service.evaluarRetrasoAlDevolver(PRESTAMO_ID));

        verifyNoInteractions(multaDAO);
    }

    @Test
    @DisplayName("evaluarRetrasoAlDevolver: ACTIVO con retraso lo marca VENCIDO y devuelve los dias de atraso")
    void evaluarRetraso_activoConRetraso_marcaVencidoYDevuelveDias() {
        PrestamoDTO prestamo = prestamo("ACTIVO", LocalDate.now().minusDays(2));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo));
        stubMontoMora();
        when(configuracionService.construirSancion(any(), any(), any(), any())).thenReturn(Optional.empty());
        stubTituloDeEjemplar();

        assertEquals(2L, service.evaluarRetrasoAlDevolver(PRESTAMO_ID));

        assertEquals("VENCIDO", prestamo.getEstado());
        verify(multaDAO).save(any(MultaDTO.class));
    }

    @Test
    @DisplayName("evaluarRetrasoAlDevolver: VENCIDO solo ajusta la multa final sin marcar ni notificar de nuevo")
    void evaluarRetraso_yaVencido_soloSincronizaLaMulta() {
        when(prestamoDAO.findById(PRESTAMO_ID))
                .thenReturn(Optional.of(prestamo("VENCIDO", LocalDate.now().minusDays(5))));
        stubMontoMora();

        assertEquals(5L, service.evaluarRetrasoAlDevolver(PRESTAMO_ID));

        verify(multaDAO).save(any(MultaDTO.class));
        verify(prestamoDAO, never()).update(any(), any());
        verifyNoInteractions(notificacionDAO);
    }

    // ---------------------------------------------------------------- cancelarSolicitudesNoRetiradas

    @Test
    @DisplayName("cancelarSolicitudesNoRetiradas: cancela la solicitud, libera el ejemplar, reasigna y notifica")
    void cancelarSolicitudes_vencida_cancelaYLiberaEjemplar() {
        PrestamoDTO solicitud = solicitud("SEDE", LocalDateTime.now().minusHours(72));
        when(configuracionService.plazo(ClavePlazo.HORAS_RETIRO_LIBRO)).thenReturn(48);
        when(prestamoDAO.findByEstadoIgnoreCase("SOLICITADO")).thenReturn(List.of(solicitud));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(solicitud));
        when(ejemplarDAO.cambiarEstadoSiEs(EJEMPLAR_ID, "PRESTADO", "DISPONIBLE")).thenReturn(1);
        stubTituloDeEjemplar();

        assertEquals(1, service.cancelarSolicitudesNoRetiradas());

        assertEquals("CANCELADO", solicitud.getEstado());
        verify(prestamoDAO).update(PRESTAMO_ID, solicitud);
        verify(reservaLibroService).asignarEjemplarAReservaEnEspera(EJEMPLAR_ID);
        NotificacionDTO n = notificacionEnviada();
        assertEquals("SOLICITUD_CANCELADA", n.getTipo());
        assertTrue(n.getMensaje().contains("Dune"));
        assertTrue(n.getMensaje().contains("48 horas"));
    }

    @Test
    @DisplayName("cancelarSolicitudesNoRetiradas: ignora las de domicilio y las que aun estan a tiempo")
    void cancelarSolicitudes_domicilioOReciente_noLasCancela() {
        when(configuracionService.plazo(ClavePlazo.HORAS_RETIRO_LIBRO)).thenReturn(48);
        when(prestamoDAO.findByEstadoIgnoreCase("SOLICITADO")).thenReturn(List.of(
                solicitud("DOMICILIO", LocalDateTime.now().minusHours(72)),
                solicitud("SEDE", LocalDateTime.now().minusHours(2))));

        assertEquals(0, service.cancelarSolicitudesNoRetiradas());

        verify(prestamoDAO, never()).findById(any());
        verify(prestamoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("cancelarSolicitudesNoRetiradas: si el ejemplar no estaba PRESTADO cancela pero no reasigna")
    void cancelarSolicitudes_ejemplarNoPrestado_noReasigna() {
        PrestamoDTO solicitud = solicitud("SEDE", LocalDateTime.now().minusHours(72));
        when(configuracionService.plazo(ClavePlazo.HORAS_RETIRO_LIBRO)).thenReturn(48);
        when(prestamoDAO.findByEstadoIgnoreCase("SOLICITADO")).thenReturn(List.of(solicitud));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(solicitud));
        when(ejemplarDAO.cambiarEstadoSiEs(EJEMPLAR_ID, "PRESTADO", "DISPONIBLE")).thenReturn(0);
        stubTituloDeEjemplar();

        assertEquals(1, service.cancelarSolicitudesNoRetiradas());

        verify(reservaLibroService, never()).asignarEjemplarAReservaEnEspera(any());
    }

    @Test
    @DisplayName("cancelarSolicitudesNoRetiradas: si al releerla ya no esta SOLICITADA no la cancela")
    void cancelarSolicitudes_yaNoSolicitada_noLaCancela() {
        PrestamoDTO enBase = solicitud("SEDE", LocalDateTime.now().minusHours(72));
        enBase.setEstado("ACTIVO");
        when(configuracionService.plazo(ClavePlazo.HORAS_RETIRO_LIBRO)).thenReturn(48);
        when(prestamoDAO.findByEstadoIgnoreCase("SOLICITADO"))
                .thenReturn(List.of(solicitud("SEDE", LocalDateTime.now().minusHours(72))));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(enBase));

        assertEquals(0, service.cancelarSolicitudesNoRetiradas());

        verify(prestamoDAO, never()).update(any(), any());
        verifyNoInteractions(ejemplarDAO);
    }

    // ---------------------------------------------------------------- expirarReservasNoRetiradas

    @Test
    @DisplayName("expirarReservasNoRetiradas: sin candidatas devuelve 0")
    void expirarReservas_sinCandidatas_devuelveCero() {
        when(reservaLibroDAO.findByEstadoIgnoreCaseAndFechaLimiteRetiroBefore(eq("NOTIFICADA"), any(LocalDateTime.class)))
                .thenReturn(List.of());

        assertEquals(0, service.expirarReservasNoRetiradas());

        verify(reservaLibroDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("expirarReservasNoRetiradas: expira la reserva, libera el ejemplar RESERVADO y notifica")
    void expirarReservas_vencida_expiraYLiberaEjemplar() {
        ReservaLibroDTO reserva = reservaLibro("NOTIFICADA", LocalDateTime.now().minusHours(1), EJEMPLAR_ID);
        when(reservaLibroDAO.findByEstadoIgnoreCaseAndFechaLimiteRetiroBefore(eq("NOTIFICADA"), any(LocalDateTime.class)))
                .thenReturn(List.of(reserva));
        when(reservaLibroDAO.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(ejemplarDAO.cambiarEstadoSiEs(EJEMPLAR_ID, "RESERVADO", "DISPONIBLE")).thenReturn(1);
        when(libroDAO.findById(LIBRO_ID)).thenReturn(Optional.of(libro("Dune")));

        assertEquals(1, service.expirarReservasNoRetiradas());

        assertEquals("EXPIRADA", reserva.getEstado());
        verify(reservaLibroDAO).update(RESERVA_ID, reserva);
        verify(reservaLibroService).asignarEjemplarAReservaEnEspera(EJEMPLAR_ID);
        NotificacionDTO n = notificacionEnviada();
        assertEquals("RESERVA_EXPIRADA", n.getTipo());
        assertTrue(n.getMensaje().contains("Dune"));
    }

    @Test
    @DisplayName("expirarReservasNoRetiradas: una reserva sin ejemplar asignado expira sin tocar ejemplares")
    void expirarReservas_sinEjemplar_noTocaEjemplares() {
        ReservaLibroDTO reserva = reservaLibro("NOTIFICADA", LocalDateTime.now().minusHours(1), null);
        when(reservaLibroDAO.findByEstadoIgnoreCaseAndFechaLimiteRetiroBefore(eq("NOTIFICADA"), any(LocalDateTime.class)))
                .thenReturn(List.of(reserva));
        when(reservaLibroDAO.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(libroDAO.findById(LIBRO_ID)).thenReturn(Optional.of(libro("Dune")));

        assertEquals(1, service.expirarReservasNoRetiradas());

        verifyNoInteractions(ejemplarDAO);
    }

    @Test
    @DisplayName("expirarReservasNoRetiradas: si al releerla aun tiene tiempo no la expira")
    void expirarReservas_aunConTiempo_noLaExpira() {
        when(reservaLibroDAO.findByEstadoIgnoreCaseAndFechaLimiteRetiroBefore(eq("NOTIFICADA"), any(LocalDateTime.class)))
                .thenReturn(List.of(reservaLibro("NOTIFICADA", LocalDateTime.now().minusHours(1), EJEMPLAR_ID)));
        when(reservaLibroDAO.findById(RESERVA_ID))
                .thenReturn(Optional.of(reservaLibro("NOTIFICADA", LocalDateTime.now().plusHours(5), EJEMPLAR_ID)));

        assertEquals(0, service.expirarReservasNoRetiradas());

        verify(reservaLibroDAO, never()).update(any(), any());
    }

    // ---------------------------------------------------------------- actualizarMultasPorMora

    @Test
    @DisplayName("actualizarMultasPorMora: crea la multa cuando el prestamo vencido aun no tiene")
    void actualizarMultas_sinMulta_laCrea() {
        PrestamoDTO vencido = prestamo("VENCIDO", LocalDate.now().minusDays(4));
        when(prestamoDAO.findByEstadoIgnoreCase("VENCIDO")).thenReturn(List.of(vencido));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(vencido));
        stubMontoMora();

        assertEquals(1, service.actualizarMultasPorMora());

        ArgumentCaptor<MultaDTO> captor = ArgumentCaptor.forClass(MultaDTO.class);
        verify(multaDAO).save(captor.capture());
        assertEquals(0, new BigDecimal("8000.00").compareTo(captor.getValue().getMonto()));
        assertEquals("PENDIENTE", captor.getValue().getEstado());
        assertTrue(captor.getValue().getMotivo().contains("4 dias de retraso"));
    }

    @Test
    @DisplayName("actualizarMultasPorMora: actualiza el monto de la multa pendiente cuando crecio")
    void actualizarMultas_pendienteDesactualizada_laActualiza() {
        PrestamoDTO vencido = prestamo("VENCIDO", LocalDate.now().minusDays(4));
        MultaDTO pendiente = multa(MULTA_ID, new BigDecimal("4000.00"),
                "Mora en la devolucion (prestamo #10, 2 dias de retraso)", "PENDIENTE");
        when(prestamoDAO.findByEstadoIgnoreCase("VENCIDO")).thenReturn(List.of(vencido));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(vencido));
        stubMontoMora();
        when(multaDAO.findByPrestamoId(PRESTAMO_ID)).thenReturn(List.of(pendiente));

        assertEquals(1, service.actualizarMultasPorMora());

        assertEquals(0, new BigDecimal("8000.00").compareTo(pendiente.getMonto()));
        assertTrue(pendiente.getMotivo().contains("4 dias de retraso"));
        verify(multaDAO).update(MULTA_ID, pendiente);
        verify(multaDAO, never()).save(any());
    }

    @Test
    @DisplayName("actualizarMultasPorMora: si la multa pendiente ya tiene el monto correcto no cambia nada")
    void actualizarMultas_pendienteAlDia_noHaceNada() {
        PrestamoDTO vencido = prestamo("VENCIDO", LocalDate.now().minusDays(4));
        MultaDTO pendiente = multa(MULTA_ID, new BigDecimal("8000.00"),
                "Mora en la devolucion (prestamo #10, 4 dias de retraso)", "PENDIENTE");
        when(prestamoDAO.findByEstadoIgnoreCase("VENCIDO")).thenReturn(List.of(vencido));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(vencido));
        stubMontoMora();
        when(multaDAO.findByPrestamoId(PRESTAMO_ID)).thenReturn(List.of(pendiente));

        assertEquals(0, service.actualizarMultasPorMora());

        verify(multaDAO, never()).update(any(), any());
        verify(multaDAO, never()).save(any());
    }

    @Test
    @DisplayName("actualizarMultasPorMora: descuenta lo ya pagado y cobra solo el resto")
    void actualizarMultas_conParteYaPagada_cobraElRestante() {
        PrestamoDTO vencido = prestamo("VENCIDO", LocalDate.now().minusDays(4));
        MultaDTO pagada = multa(MULTA_ID, new BigDecimal("4000.00"),
                "Mora en la devolucion (prestamo #10, 2 dias de retraso)", "PAGADA");
        when(prestamoDAO.findByEstadoIgnoreCase("VENCIDO")).thenReturn(List.of(vencido));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(vencido));
        stubMontoMora();
        when(multaDAO.findByPrestamoId(PRESTAMO_ID)).thenReturn(List.of(pagada));

        assertEquals(1, service.actualizarMultasPorMora());

        ArgumentCaptor<MultaDTO> captor = ArgumentCaptor.forClass(MultaDTO.class);
        verify(multaDAO).save(captor.capture());
        assertEquals(0, new BigDecimal("4000.00").compareTo(captor.getValue().getMonto()));
    }

    @Test
    @DisplayName("actualizarMultasPorMora: si ya esta todo pagado no genera una multa nueva")
    void actualizarMultas_todoPagado_noGeneraMulta() {
        PrestamoDTO vencido = prestamo("VENCIDO", LocalDate.now().minusDays(4));
        MultaDTO pagada = multa(MULTA_ID, new BigDecimal("8000.00"),
                "Mora en la devolucion (prestamo #10, 4 dias de retraso)", "PAGADA");
        when(prestamoDAO.findByEstadoIgnoreCase("VENCIDO")).thenReturn(List.of(vencido));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(vencido));
        stubMontoMora();
        when(multaDAO.findByPrestamoId(PRESTAMO_ID)).thenReturn(List.of(pagada));

        assertEquals(0, service.actualizarMultasPorMora());

        verify(multaDAO, never()).save(any());
        verify(multaDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarMultasPorMora: ignora multas anuladas y las de otro motivo")
    void actualizarMultas_anuladasYOtroMotivo_seIgnoran() {
        PrestamoDTO vencido = prestamo("VENCIDO", LocalDate.now().minusDays(4));
        MultaDTO anulada = multa(MULTA_ID, new BigDecimal("8000.00"),
                "Mora en la devolucion (prestamo #10, 4 dias de retraso)", "ANULADA");
        MultaDTO otroMotivo = multa(MULTA_ID + 1, new BigDecimal("15000.00"), "Libro danado", "PENDIENTE");
        when(prestamoDAO.findByEstadoIgnoreCase("VENCIDO")).thenReturn(List.of(vencido));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(vencido));
        stubMontoMora();
        when(multaDAO.findByPrestamoId(PRESTAMO_ID)).thenReturn(List.of(anulada, otroMotivo));

        assertEquals(1, service.actualizarMultasPorMora());

        ArgumentCaptor<MultaDTO> captor = ArgumentCaptor.forClass(MultaDTO.class);
        verify(multaDAO).save(captor.capture());
        assertEquals(0, new BigDecimal("8000.00").compareTo(captor.getValue().getMonto()));
        verify(multaDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarMultasPorMora: un prestamo sin fecha de vencimiento no se procesa")
    void actualizarMultas_sinFechaVencimiento_noSeProcesa() {
        when(prestamoDAO.findByEstadoIgnoreCase("VENCIDO"))
                .thenReturn(List.of(prestamo("VENCIDO", null)));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo("VENCIDO", null)));

        assertEquals(0, service.actualizarMultasPorMora());

        verifyNoInteractions(multaDAO);
    }

    @Test
    @DisplayName("actualizarMultasPorMora: si al releerlo ya no esta VENCIDO no lo procesa")
    void actualizarMultas_yaNoVencido_noSeProcesa() {
        when(prestamoDAO.findByEstadoIgnoreCase("VENCIDO"))
                .thenReturn(List.of(prestamo("VENCIDO", LocalDate.now().minusDays(4))));
        when(prestamoDAO.findById(PRESTAMO_ID))
                .thenReturn(Optional.of(prestamo("DEVUELTO", LocalDate.now().minusDays(4))));

        assertEquals(0, service.actualizarMultasPorMora());

        verifyNoInteractions(multaDAO);
    }

    @Test
    @DisplayName("cada tarea se ejecuta en su propia transaccion (REQUIRES_NEW) y se confirma")
    void tareas_usanUnaTransaccionNuevaPorElemento() {
        when(prestamoDAO.findByEstadoIgnoreCase("VENCIDO"))
                .thenReturn(List.of(prestamo("VENCIDO", LocalDate.now().minusDays(4))));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.empty());

        service.actualizarMultasPorMora();

        ArgumentCaptor<TransactionDefinition> captor = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(transactionManager).getTransaction(captor.capture());
        assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW, captor.getValue().getPropagationBehavior());
        verify(transactionManager).commit(any());
    }

    // ---------------------------------------------------------------- levantarSancionesPorDevolucion

    @Test
    @DisplayName("levantarSancionesPorDevolucion: finaliza la sancion por mora cuando el prestamo ya fue DEVUELTO")
    void levantarSanciones_prestamoDevuelto_lasFinaliza() {
        SancionDTO activa = sancion("ACTIVA", "Retraso en la devolucion (prestamo #10)", null, PRESTAMO_ID);
        stubSancionesActivas(List.of(activa));
        when(sancionDAO.findById(SANCION_ID)).thenReturn(Optional.of(activa));
        when(prestamoDAO.findById(PRESTAMO_ID))
                .thenReturn(Optional.of(prestamo("DEVUELTO", LocalDate.now().minusDays(3))));

        assertEquals(1, service.levantarSancionesPorDevolucion());

        assertEquals("FINALIZADA", activa.getEstado());
        assertNotNull(activa.getFechaFin());
        verify(sancionDAO).update(SANCION_ID, activa);
        NotificacionDTO n = notificacionEnviada();
        assertEquals("SANCION_FINALIZADA", n.getTipo());
        assertEquals(USUARIO_ID, n.getUsuarioId());
    }

    @Test
    @DisplayName("levantarSancionesPorDevolucion: si el prestamo sigue VENCIDO no levanta la sancion")
    void levantarSanciones_prestamoAunVencido_noLaLevanta() {
        SancionDTO activa = sancion("ACTIVA", "Retraso en la devolucion (prestamo #10)", null, PRESTAMO_ID);
        stubSancionesActivas(List.of(activa));
        when(sancionDAO.findById(SANCION_ID)).thenReturn(Optional.of(activa));
        when(prestamoDAO.findById(PRESTAMO_ID))
                .thenReturn(Optional.of(prestamo("VENCIDO", LocalDate.now().minusDays(3))));

        assertEquals(0, service.levantarSancionesPorDevolucion());

        verify(sancionDAO, never()).update(any(), any());
        verifyNoInteractions(notificacionDAO);
    }

    @Test
    @DisplayName("levantarSancionesPorDevolucion: ignora sanciones sin prestamo o que no son por mora")
    void levantarSanciones_sinPrestamoOOtroMotivo_seIgnoran() {
        stubSancionesActivas(List.of(
                sancion("ACTIVA", "Retraso en la devolucion (prestamo #10)", null, null),
                sancion("ACTIVA", "Otro motivo", null, PRESTAMO_ID)));

        assertEquals(0, service.levantarSancionesPorDevolucion());

        verify(sancionDAO, never()).findById(any());
    }

    @Test
    @DisplayName("levantarSancionesPorDevolucion: si al releerla ya no esta ACTIVA no la toca")
    void levantarSanciones_yaNoActiva_noLaToca() {
        SancionDTO enLista = sancion("ACTIVA", "Retraso en la devolucion (prestamo #10)", null, PRESTAMO_ID);
        SancionDTO enBase = sancion("FINALIZADA", "Retraso en la devolucion (prestamo #10)", null, PRESTAMO_ID);
        stubSancionesActivas(List.of(enLista));
        when(sancionDAO.findById(SANCION_ID)).thenReturn(Optional.of(enBase));

        assertEquals(0, service.levantarSancionesPorDevolucion());

        verify(prestamoDAO, never()).findById(any());
        verify(sancionDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("levantarSancionesPorDevolucion: si el prestamo no existe no levanta la sancion")
    void levantarSanciones_prestamoInexistente_noLaLevanta() {
        SancionDTO activa = sancion("ACTIVA", "Retraso en la devolucion (prestamo #10)", null, PRESTAMO_ID);
        stubSancionesActivas(List.of(activa));
        when(sancionDAO.findById(SANCION_ID)).thenReturn(Optional.of(activa));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.empty());

        assertEquals(0, service.levantarSancionesPorDevolucion());

        verify(sancionDAO, never()).update(any(), any());
    }

    // ---------------------------------------------------------------- liberarReservasSalaVencidas

    @Test
    @DisplayName("liberarReservasSalaVencidas: cancela la reserva pendiente de pago pasados 30 minutos y notifica")
    void liberarSalas_pendienteSinPago_laCancela() {
        ReservaSalaDTO pendiente = reservaSala(RESERVA_SALA_ID, "PENDIENTE_PAGO");
        pendiente.setFechaCreacion(LocalDateTime.now().minusMinutes(45));
        stubReservasSala(List.of(pendiente), List.of());
        when(reservaSalaDAO.findById(RESERVA_SALA_ID)).thenReturn(Optional.of(pendiente));

        assertEquals(1, service.liberarReservasSalaVencidas());

        assertEquals("CANCELADA", pendiente.getEstado());
        assertNotNull(pendiente.getFechaCancelacion());
        verify(reservaSalaDAO).update(RESERVA_SALA_ID, pendiente);
        assertEquals("RESERVA_SALA_CANCELADA", notificacionEnviada().getTipo());
    }

    @Test
    @DisplayName("liberarReservasSalaVencidas: no cancela si aun no pasaron 30 minutos")
    void liberarSalas_pendienteReciente_noLaCancela() {
        ReservaSalaDTO pendiente = reservaSala(RESERVA_SALA_ID, "PENDIENTE_PAGO");
        pendiente.setFechaCreacion(LocalDateTime.now().minusMinutes(10));
        stubReservasSala(List.of(pendiente), List.of());
        when(reservaSalaDAO.findById(RESERVA_SALA_ID)).thenReturn(Optional.of(pendiente));

        assertEquals(0, service.liberarReservasSalaVencidas());

        verify(reservaSalaDAO, never()).update(any(), any());
        verifyNoInteractions(notificacionDAO);
    }

    @Test
    @DisplayName("liberarReservasSalaVencidas: una pendiente sin fecha de creacion no se cancela")
    void liberarSalas_pendienteSinFechaCreacion_noSeCancela() {
        ReservaSalaDTO pendiente = reservaSala(RESERVA_SALA_ID, "PENDIENTE_PAGO");
        pendiente.setFechaCreacion(null);
        stubReservasSala(List.of(pendiente), List.of());
        when(reservaSalaDAO.findById(RESERVA_SALA_ID)).thenReturn(Optional.of(pendiente));

        assertEquals(0, service.liberarReservasSalaVencidas());

        verify(reservaSalaDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("liberarReservasSalaVencidas: finaliza la reserva confirmada cuya hora de fin ya paso")
    void liberarSalas_confirmadaTerminada_laFinaliza() {
        ReservaSalaDTO confirmada = reservaSala(RESERVA_SALA_ID, "CONFIRMADA");
        confirmada.setFechaFin(LocalDateTime.now().minusHours(1));
        stubReservasSala(List.of(), List.of(confirmada));
        when(reservaSalaDAO.findById(RESERVA_SALA_ID)).thenReturn(Optional.of(confirmada));

        assertEquals(1, service.liberarReservasSalaVencidas());

        assertEquals("FINALIZADA", confirmada.getEstado());
        verify(reservaSalaDAO).update(RESERVA_SALA_ID, confirmada);
        verifyNoInteractions(notificacionDAO);
    }

    @Test
    @DisplayName("liberarReservasSalaVencidas: no finaliza una confirmada que aun no termina")
    void liberarSalas_confirmadaEnCurso_noLaFinaliza() {
        ReservaSalaDTO confirmada = reservaSala(RESERVA_SALA_ID, "CONFIRMADA");
        confirmada.setFechaFin(LocalDateTime.now().plusHours(1));
        stubReservasSala(List.of(), List.of(confirmada));

        assertEquals(0, service.liberarReservasSalaVencidas());

        verify(reservaSalaDAO, never()).findById(any());
        verify(reservaSalaDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("liberarReservasSalaVencidas: suma las canceladas y las finalizadas")
    void liberarSalas_pendienteYConfirmada_devuelveLaSuma() {
        ReservaSalaDTO pendiente = reservaSala(RESERVA_SALA_ID, "PENDIENTE_PAGO");
        pendiente.setFechaCreacion(LocalDateTime.now().minusMinutes(45));
        ReservaSalaDTO confirmada = reservaSala(OTRA_RESERVA_SALA_ID, "CONFIRMADA");
        confirmada.setFechaFin(LocalDateTime.now().minusHours(1));
        stubReservasSala(List.of(pendiente), List.of(confirmada));
        when(reservaSalaDAO.findById(RESERVA_SALA_ID)).thenReturn(Optional.of(pendiente));
        when(reservaSalaDAO.findById(OTRA_RESERVA_SALA_ID)).thenReturn(Optional.of(confirmada));

        assertEquals(2, service.liberarReservasSalaVencidas());

        assertEquals("CANCELADA", pendiente.getEstado());
        assertEquals("FINALIZADA", confirmada.getEstado());
    }

    // ---------------------------------------------------------------- cancelarPedidosInterciudadSinPago

    @Test
    @DisplayName("cancelarPedidosInterciudadSinPago: cancela el pedido, su prestamo, libera el ejemplar y avisa a la sede")
    void cancelarPedidos_sinPago_cancelaTodo() {
        PedidoInterciudadDTO pedido = pedido("PENDIENTE_PAGO");
        pedido.setFechaSolicitud(LocalDateTime.now().minusMinutes(45));
        PrestamoDTO prestamo = prestamo("SOLICITADO", LocalDate.now().plusDays(14));
        when(pedidoInterciudadDAO.findByEstadoIgnoreCaseOrderByFechaSolicitudAsc("PENDIENTE_PAGO"))
                .thenReturn(List.of(pedido));
        when(pedidoInterciudadDAO.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo));
        when(ejemplarDAO.cambiarEstadoSiEs(EJEMPLAR_ID, "RESERVADO", "DISPONIBLE")).thenReturn(1);

        assertEquals(1, service.cancelarPedidosInterciudadSinPago());

        assertEquals("CANCELADO", pedido.getEstado());
        assertNotNull(pedido.getFechaCancelacion());
        verify(pedidoInterciudadDAO).update(PEDIDO_ID, pedido);
        assertEquals("CANCELADO", prestamo.getEstado());
        verify(prestamoDAO).update(PRESTAMO_ID, prestamo);
        verify(reservaLibroService).asignarEjemplarAReservaEnEspera(EJEMPLAR_ID);
        assertEquals("PEDIDO_CANCELADO_SIN_PAGO", notificacionEnviada().getTipo());
        verify(notificacionService).notificarSede(eq(SEDE_ORIGEN_ID), eq("PEDIDO_NO_RECOGIDO"),
                anyString(), anyString());
    }

    @Test
    @DisplayName("cancelarPedidosInterciudadSinPago: un pedido sin prestamo asociado igual se cancela")
    void cancelarPedidos_sinPrestamo_cancelaSinTocarPrestamos() {
        PedidoInterciudadDTO pedido = pedido("PENDIENTE_PAGO");
        pedido.setPrestamoId(null);
        pedido.setFechaSolicitud(LocalDateTime.now().minusMinutes(45));
        when(pedidoInterciudadDAO.findByEstadoIgnoreCaseOrderByFechaSolicitudAsc("PENDIENTE_PAGO"))
                .thenReturn(List.of(pedido));
        when(pedidoInterciudadDAO.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(ejemplarDAO.cambiarEstadoSiEs(EJEMPLAR_ID, "RESERVADO", "DISPONIBLE")).thenReturn(1);

        assertEquals(1, service.cancelarPedidosInterciudadSinPago());

        verifyNoInteractions(prestamoDAO);
    }

    @Test
    @DisplayName("cancelarPedidosInterciudadSinPago: si el ejemplar no estaba RESERVADO cancela pero no reasigna")
    void cancelarPedidos_ejemplarNoReservado_noReasigna() {
        PedidoInterciudadDTO pedido = pedido("PENDIENTE_PAGO");
        pedido.setPrestamoId(null);
        pedido.setFechaSolicitud(LocalDateTime.now().minusMinutes(45));
        when(pedidoInterciudadDAO.findByEstadoIgnoreCaseOrderByFechaSolicitudAsc("PENDIENTE_PAGO"))
                .thenReturn(List.of(pedido));
        when(pedidoInterciudadDAO.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(ejemplarDAO.cambiarEstadoSiEs(EJEMPLAR_ID, "RESERVADO", "DISPONIBLE")).thenReturn(0);

        assertEquals(1, service.cancelarPedidosInterciudadSinPago());

        verify(reservaLibroService, never()).asignarEjemplarAReservaEnEspera(any());
    }

    @Test
    @DisplayName("cancelarPedidosInterciudadSinPago: ignora los pedidos que aun estan dentro del plazo de pago")
    void cancelarPedidos_recientes_noSeCancelan() {
        PedidoInterciudadDTO pedido = pedido("PENDIENTE_PAGO");
        pedido.setFechaSolicitud(LocalDateTime.now().minusMinutes(5));
        when(pedidoInterciudadDAO.findByEstadoIgnoreCaseOrderByFechaSolicitudAsc("PENDIENTE_PAGO"))
                .thenReturn(List.of(pedido));

        assertEquals(0, service.cancelarPedidosInterciudadSinPago());

        verify(pedidoInterciudadDAO, never()).findById(any());
    }

    @Test
    @DisplayName("cancelarPedidosInterciudadSinPago: si al releerlo ya no esta PENDIENTE_PAGO no lo cancela")
    void cancelarPedidos_yaPagado_noSeCancela() {
        PedidoInterciudadDTO enLista = pedido("PENDIENTE_PAGO");
        enLista.setFechaSolicitud(LocalDateTime.now().minusMinutes(45));
        PedidoInterciudadDTO enBase = pedido("EN_ESPERA_DE_RECOGIDA");
        enBase.setFechaSolicitud(LocalDateTime.now().minusMinutes(45));
        when(pedidoInterciudadDAO.findByEstadoIgnoreCaseOrderByFechaSolicitudAsc("PENDIENTE_PAGO"))
                .thenReturn(List.of(enLista));
        when(pedidoInterciudadDAO.findById(PEDIDO_ID)).thenReturn(Optional.of(enBase));

        assertEquals(0, service.cancelarPedidosInterciudadSinPago());

        verify(pedidoInterciudadDAO, never()).update(any(), any());
        verifyNoInteractions(notificacionService);
    }

    // ---------------------------------------------------------------- procesarPedidosNoRecogidos

    @Test
    @DisplayName("procesarPedidosNoRecogidos: pasa el pedido a pendiente de traslado, cancela el prestamo, multa y sanciona")
    void pedidosNoRecogidos_vencido_aplicaMultaYSancion() {
        PedidoInterciudadDTO pedido = pedido("EN_ESPERA_DE_RECOGIDA");
        pedido.setFechaLimiteRecogida(LocalDateTime.now().minusHours(2));
        PrestamoDTO prestamo = prestamo("SOLICITADO", LocalDate.now().plusDays(14));
        SancionDTO sancionNueva = sancion("ACTIVA", "No recogio el pedido interciudad a tiempo (pedido #70)",
                LocalDateTime.now().plusDays(7), PRESTAMO_ID);
        when(pedidoInterciudadDAO.findByEstadoIgnoreCaseOrderByFechaSolicitudAsc("EN_ESPERA_DE_RECOGIDA"))
                .thenReturn(List.of(pedido));
        when(pedidoInterciudadDAO.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo));
        when(configuracionService.montoMulta(ConceptoMulta.PEDIDO_NO_RECOGIDO)).thenReturn(new BigDecimal("20000.00"));
        when(configuracionService.construirSancion(eq(InfraccionSancion.PEDIDO_NO_RECOGIDO), eq(USUARIO_ID),
                eq(PRESTAMO_ID), anyString())).thenReturn(Optional.of(sancionNueva));

        assertEquals(1, service.procesarPedidosNoRecogidos());

        assertEquals("DEVUELTO_PENDIENTE_TRASLADO", pedido.getEstado());
        verify(pedidoInterciudadDAO).update(PEDIDO_ID, pedido);
        assertEquals("CANCELADO", prestamo.getEstado());
        verify(prestamoDAO).update(PRESTAMO_ID, prestamo);

        ArgumentCaptor<MultaDTO> multaCaptor = ArgumentCaptor.forClass(MultaDTO.class);
        verify(multaDAO).save(multaCaptor.capture());
        MultaDTO multa = multaCaptor.getValue();
        assertEquals(0, new BigDecimal("20000.00").compareTo(multa.getMonto()));
        assertEquals("PENDIENTE", multa.getEstado());
        assertEquals(USUARIO_ID, multa.getUsuarioId());
        assertEquals(PRESTAMO_ID, multa.getPrestamoId());
        assertTrue(multa.getMotivo().startsWith("Pedido interciudad no recogido a tiempo"));

        verify(sancionDAO).save(sancionNueva);
        NotificacionDTO n = notificacionEnviada();
        assertEquals("PEDIDO_NO_RECOGIDO", n.getTipo());
        assertTrue(n.getMensaje().contains("hasta pagarla y hasta el " + sancionNueva.getFechaFin().toLocalDate()));
    }

    @Test
    @DisplayName("procesarPedidosNoRecogidos: si la politica no genera sancion solo aplica la multa")
    void pedidosNoRecogidos_sinSancionConfigurada_soloMulta() {
        PedidoInterciudadDTO pedido = pedido("EN_ESPERA_DE_RECOGIDA");
        pedido.setFechaLimiteRecogida(LocalDateTime.now().minusHours(2));
        when(pedidoInterciudadDAO.findByEstadoIgnoreCaseOrderByFechaSolicitudAsc("EN_ESPERA_DE_RECOGIDA"))
                .thenReturn(List.of(pedido));
        when(pedidoInterciudadDAO.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(prestamoDAO.findById(PRESTAMO_ID))
                .thenReturn(Optional.of(prestamo("SOLICITADO", LocalDate.now().plusDays(14))));
        when(configuracionService.montoMulta(ConceptoMulta.PEDIDO_NO_RECOGIDO)).thenReturn(new BigDecimal("20000.00"));
        when(configuracionService.construirSancion(any(), any(), any(), any())).thenReturn(Optional.empty());

        assertEquals(1, service.procesarPedidosNoRecogidos());

        verify(multaDAO).save(any(MultaDTO.class));
        verify(sancionDAO, never()).save(any());
        assertTrue(notificacionEnviada().getMensaje().contains("Debes pagarla para volver a pedir libros"));
    }

    @Test
    @DisplayName("procesarPedidosNoRecogidos: una sancion sin fecha de fin dura hasta pagar la multa")
    void pedidosNoRecogidos_sancionSinFechaFin_dicePagarLaMulta() {
        PedidoInterciudadDTO pedido = pedido("EN_ESPERA_DE_RECOGIDA");
        pedido.setFechaLimiteRecogida(LocalDateTime.now().minusHours(2));
        SancionDTO indefinida = sancion("ACTIVA", "No recogio el pedido interciudad a tiempo", null, PRESTAMO_ID);
        when(pedidoInterciudadDAO.findByEstadoIgnoreCaseOrderByFechaSolicitudAsc("EN_ESPERA_DE_RECOGIDA"))
                .thenReturn(List.of(pedido));
        when(pedidoInterciudadDAO.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(prestamoDAO.findById(PRESTAMO_ID))
                .thenReturn(Optional.of(prestamo("SOLICITADO", LocalDate.now().plusDays(14))));
        when(configuracionService.montoMulta(ConceptoMulta.PEDIDO_NO_RECOGIDO)).thenReturn(new BigDecimal("20000.00"));
        when(configuracionService.construirSancion(any(), any(), any(), any())).thenReturn(Optional.of(indefinida));

        assertEquals(1, service.procesarPedidosNoRecogidos());

        String mensaje = notificacionEnviada().getMensaje();
        assertTrue(mensaje.contains("hasta pagarla."));
        assertFalse(mensaje.contains("hasta el"));
    }

    @Test
    @DisplayName("procesarPedidosNoRecogidos: un pedido sin prestamo asociado igual recibe multa")
    void pedidosNoRecogidos_sinPrestamo_noTocaPrestamos() {
        PedidoInterciudadDTO pedido = pedido("EN_ESPERA_DE_RECOGIDA");
        pedido.setPrestamoId(null);
        pedido.setFechaLimiteRecogida(LocalDateTime.now().minusHours(2));
        when(pedidoInterciudadDAO.findByEstadoIgnoreCaseOrderByFechaSolicitudAsc("EN_ESPERA_DE_RECOGIDA"))
                .thenReturn(List.of(pedido));
        when(pedidoInterciudadDAO.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        when(configuracionService.montoMulta(ConceptoMulta.PEDIDO_NO_RECOGIDO)).thenReturn(new BigDecimal("20000.00"));
        when(configuracionService.construirSancion(any(), any(), any(), any())).thenReturn(Optional.empty());

        assertEquals(1, service.procesarPedidosNoRecogidos());

        verifyNoInteractions(prestamoDAO);
        verify(multaDAO).save(any(MultaDTO.class));
    }

    @Test
    @DisplayName("procesarPedidosNoRecogidos: ignora los pedidos cuyo plazo de recogida aun no vence")
    void pedidosNoRecogidos_aunConPlazo_seIgnoran() {
        PedidoInterciudadDTO pedido = pedido("EN_ESPERA_DE_RECOGIDA");
        pedido.setFechaLimiteRecogida(LocalDateTime.now().plusHours(5));
        when(pedidoInterciudadDAO.findByEstadoIgnoreCaseOrderByFechaSolicitudAsc("EN_ESPERA_DE_RECOGIDA"))
                .thenReturn(List.of(pedido));

        assertEquals(0, service.procesarPedidosNoRecogidos());

        verify(pedidoInterciudadDAO, never()).findById(any());
    }

    @Test
    @DisplayName("procesarPedidosNoRecogidos: si al releerlo ya cambio de estado no lo procesa")
    void pedidosNoRecogidos_yaCambioDeEstado_noSeProcesa() {
        PedidoInterciudadDTO enLista = pedido("EN_ESPERA_DE_RECOGIDA");
        enLista.setFechaLimiteRecogida(LocalDateTime.now().minusHours(2));
        PedidoInterciudadDTO enBase = pedido("ENTREGADO");
        enBase.setFechaLimiteRecogida(LocalDateTime.now().minusHours(2));
        when(pedidoInterciudadDAO.findByEstadoIgnoreCaseOrderByFechaSolicitudAsc("EN_ESPERA_DE_RECOGIDA"))
                .thenReturn(List.of(enLista));
        when(pedidoInterciudadDAO.findById(PEDIDO_ID)).thenReturn(Optional.of(enBase));

        assertEquals(0, service.procesarPedidosNoRecogidos());

        verify(pedidoInterciudadDAO, never()).update(any(), any());
        verifyNoInteractions(multaDAO);
    }

    // ---------------------------------------------------------------- finalizarSancionesVencidas

    @Test
    @DisplayName("finalizarSancionesVencidas: finaliza la sancion cuya fecha de fin ya paso y notifica")
    void finalizarSanciones_vencida_laFinaliza() {
        SancionDTO vencida = sancion("ACTIVA", "Otro motivo", LocalDateTime.now().minusHours(1), null);
        stubSancionesActivas(List.of(vencida));
        when(sancionDAO.findById(SANCION_ID)).thenReturn(Optional.of(vencida));

        assertEquals(1, service.finalizarSancionesVencidas());

        assertEquals("FINALIZADA", vencida.getEstado());
        verify(sancionDAO).update(SANCION_ID, vencida);
        assertEquals("SANCION_FINALIZADA", notificacionEnviada().getTipo());
    }

    @Test
    @DisplayName("finalizarSancionesVencidas: no finaliza las que aun no vencen")
    void finalizarSanciones_aunVigente_noLaFinaliza() {
        stubSancionesActivas(List.of(sancion("ACTIVA", "Otro motivo", LocalDateTime.now().plusDays(2), null)));

        assertEquals(0, service.finalizarSancionesVencidas());

        verify(sancionDAO, never()).findById(any());
    }

    @Test
    @DisplayName("finalizarSancionesVencidas: ignora las sanciones indefinidas (sin fecha de fin)")
    void finalizarSanciones_sinFechaFin_seIgnora() {
        stubSancionesActivas(List.of(sancion("ACTIVA", "Otro motivo", null, PRESTAMO_ID)));

        assertEquals(0, service.finalizarSancionesVencidas());

        verify(sancionDAO, never()).findById(any());
    }

    @Test
    @DisplayName("finalizarSancionesVencidas: si al releerla ya no esta ACTIVA no la toca")
    void finalizarSanciones_yaNoActiva_noLaToca() {
        SancionDTO enLista = sancion("ACTIVA", "Otro motivo", LocalDateTime.now().minusHours(1), null);
        SancionDTO enBase = sancion("FINALIZADA", "Otro motivo", LocalDateTime.now().minusHours(1), null);
        stubSancionesActivas(List.of(enLista));
        when(sancionDAO.findById(SANCION_ID)).thenReturn(Optional.of(enBase));

        assertEquals(0, service.finalizarSancionesVencidas());

        verify(sancionDAO, never()).update(any(), any());
        verifyNoInteractions(notificacionDAO);
    }

    // ---------------------------------------------------------------- enviarRecordatoriosPrestamosProximos

    @Test
    @DisplayName("enviarRecordatoriosPrestamosProximos: sin prestamos por vencer no envia nada")
    void recordatoriosPrestamos_sinPrestamos_devuelveCero() {
        stubVencimientos(List.of(), List.of());

        assertEquals(0, service.enviarRecordatoriosPrestamosProximos());

        verifyNoInteractions(notificacionDAO);
    }

    @Test
    @DisplayName("enviarRecordatoriosPrestamosProximos: avisa 48 horas antes con el titulo del libro")
    void recordatoriosPrestamos_vence48h_envia() {
        stubVencimientos(List.of(prestamo("ACTIVO", LocalDate.now().plusDays(2))), List.of());
        stubTituloDeEjemplar();

        assertEquals(1, service.enviarRecordatoriosPrestamosProximos());

        NotificacionDTO n = notificacionEnviada();
        assertEquals("RECORDATORIO_PRESTAMO", n.getTipo());
        assertEquals(USUARIO_ID, n.getUsuarioId());
        assertTrue(n.getMensaje().contains("Dune"));
    }

    @Test
    @DisplayName("enviarRecordatoriosPrestamosProximos: avisa 24 horas antes")
    void recordatoriosPrestamos_vence24h_envia() {
        stubVencimientos(List.of(), List.of(prestamo("ACTIVO", LocalDate.now().plusDays(1))));
        stubTituloDeEjemplar();

        assertEquals(1, service.enviarRecordatoriosPrestamosProximos());

        assertEquals("RECORDATORIO_PRESTAMO", notificacionEnviada().getTipo());
    }

    @Test
    @DisplayName("enviarRecordatoriosPrestamosProximos: suma los avisos de 48 y de 24 horas")
    void recordatoriosPrestamos_ambosGrupos_devuelveLaSuma() {
        stubVencimientos(
                List.of(prestamo(PRESTAMO_ID, "ACTIVO", LocalDate.now().plusDays(2))),
                List.of(prestamo(OTRO_PRESTAMO_ID, "ACTIVO", LocalDate.now().plusDays(1))));
        stubTituloDeEjemplar();

        assertEquals(2, service.enviarRecordatoriosPrestamosProximos());

        verify(notificacionDAO, times(2)).save(any(NotificacionDTO.class));
    }

    @Test
    @DisplayName("enviarRecordatoriosPrestamosProximos: si falla un aviso sigue con los demas")
    void recordatoriosPrestamos_falloUnAviso_continua() {
        stubVencimientos(
                List.of(prestamo(PRESTAMO_ID, "ACTIVO", LocalDate.now().plusDays(2)),
                        prestamo(OTRO_PRESTAMO_ID, "ACTIVO", LocalDate.now().plusDays(2))),
                List.of());
        stubTituloDeEjemplar();
        when(notificacionDAO.save(any(NotificacionDTO.class)))
                .thenThrow(new RuntimeException("falla simulada"))
                .thenReturn(new NotificacionDTO());

        assertEquals(1, service.enviarRecordatoriosPrestamosProximos());

        verify(notificacionDAO, times(2)).save(any(NotificacionDTO.class));
    }

    // ---------------------------------------------------------------- enviarRecordatoriosEventosProximos

    @Test
    @DisplayName("enviarRecordatoriosEventosProximos: sin eventos proximos no envia nada")
    void recordatoriosEventos_sinEventos_devuelveCero() {
        when(eventoDAO.findByEstadoAndFechaInicioBetween(eq("PROGRAMADO"), any(LocalDateTime.class),
                any(LocalDateTime.class))).thenReturn(List.of());

        assertEquals(0, service.enviarRecordatoriosEventosProximos());

        verifyNoInteractions(inscripcionEventoDAO, notificacionDAO);
    }

    @Test
    @DisplayName("enviarRecordatoriosEventosProximos: avisa a cada inscrito activo del evento")
    void recordatoriosEventos_conInscritos_avisaATodos() {
        EventoDTO evento = new EventoDTO();
        evento.setId(EVENTO_ID);
        evento.setTitulo("Club de lectura");
        evento.setFechaInicio(LocalDateTime.now().plusHours(5));
        InscripcionEventoDTO primera = new InscripcionEventoDTO();
        primera.setId(1L);
        primera.setUsuarioId(USUARIO_ID);
        InscripcionEventoDTO segunda = new InscripcionEventoDTO();
        segunda.setId(2L);
        segunda.setUsuarioId(2L);
        when(eventoDAO.findByEstadoAndFechaInicioBetween(eq("PROGRAMADO"), any(LocalDateTime.class),
                any(LocalDateTime.class))).thenReturn(List.of(evento));
        when(inscripcionEventoDAO.findByEventoIdAndEstadoIgnoreCase(EVENTO_ID, "ACTIVA"))
                .thenReturn(List.of(primera, segunda));

        assertEquals(2, service.enviarRecordatoriosEventosProximos());

        ArgumentCaptor<NotificacionDTO> captor = ArgumentCaptor.forClass(NotificacionDTO.class);
        verify(notificacionDAO, times(2)).save(captor.capture());
        List<NotificacionDTO> enviadas = captor.getAllValues();
        assertEquals("RECORDATORIO_EVENTO", enviadas.get(0).getTipo());
        assertTrue(enviadas.get(0).getTitulo().contains("Club de lectura"));
        assertEquals(USUARIO_ID, enviadas.get(0).getUsuarioId());
        assertEquals(2L, enviadas.get(1).getUsuarioId());
    }

    @Test
    @DisplayName("enviarRecordatoriosEventosProximos: un evento sin inscritos activos no genera avisos")
    void recordatoriosEventos_sinInscritos_devuelveCero() {
        EventoDTO evento = new EventoDTO();
        evento.setId(EVENTO_ID);
        evento.setTitulo("Club de lectura");
        evento.setFechaInicio(LocalDateTime.now().plusHours(5));
        when(eventoDAO.findByEstadoAndFechaInicioBetween(eq("PROGRAMADO"), any(LocalDateTime.class),
                any(LocalDateTime.class))).thenReturn(List.of(evento));

        assertEquals(0, service.enviarRecordatoriosEventosProximos());

        verifyNoInteractions(notificacionDAO);
    }

    // ---------------------------------------------------------------- enviarRecordatoriosSalasProximas

    @Test
    @DisplayName("enviarRecordatoriosSalasProximas: avisa con el nombre de la sala y el numero de reserva")
    void recordatoriosSalas_confirmada_envia() {
        ReservaSalaDTO reserva = reservaSala(RESERVA_SALA_ID, "CONFIRMADA");
        SalaEstudioDTO sala = new SalaEstudioDTO();
        sala.setId(SALA_ID);
        sala.setNombre("Sala Azul");
        when(reservaSalaDAO.findByEstadoInAndFechaInicioBetween(eq(List.of("CONFIRMADA")),
                any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(reserva));
        when(salaEstudioDAO.findById(SALA_ID)).thenReturn(Optional.of(sala));

        assertEquals(1, service.enviarRecordatoriosSalasProximas());

        NotificacionDTO n = notificacionEnviada();
        assertEquals("RECORDATORIO_RESERVA_SALA", n.getTipo());
        assertEquals(USUARIO_ID, n.getUsuarioId());
        assertTrue(n.getMensaje().contains("Sala Azul"));
        assertTrue(n.getMensaje().contains("#" + RESERVA_SALA_ID));
    }

    @Test
    @DisplayName("enviarRecordatoriosSalasProximas: si la sala no se encuentra usa un nombre de respaldo")
    void recordatoriosSalas_salaNoEncontrada_usaNombreDeRespaldo() {
        ReservaSalaDTO reserva = reservaSala(RESERVA_SALA_ID, "CONFIRMADA");
        when(reservaSalaDAO.findByEstadoInAndFechaInicioBetween(eq(List.of("CONFIRMADA")),
                any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(reserva));
        when(salaEstudioDAO.findById(SALA_ID)).thenReturn(Optional.empty());

        assertEquals(1, service.enviarRecordatoriosSalasProximas());

        assertTrue(notificacionEnviada().getMensaje().contains("Sala #" + SALA_ID));
    }

    @Test
    @DisplayName("enviarRecordatoriosSalasProximas: no repite el aviso si ya se envio antes")
    void recordatoriosSalas_yaAvisado_noRepite() {
        ReservaSalaDTO reserva = reservaSala(RESERVA_SALA_ID, "CONFIRMADA");
        when(reservaSalaDAO.findByEstadoInAndFechaInicioBetween(eq(List.of("CONFIRMADA")),
                any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(reserva));
        when(salaEstudioDAO.findById(SALA_ID)).thenReturn(Optional.empty());
        when(notificacionDAO.existsByUsuarioIdAndTipoAndMensaje(eq(USUARIO_ID), eq("RECORDATORIO_RESERVA_SALA"),
                anyString())).thenReturn(true);

        assertEquals(0, service.enviarRecordatoriosSalasProximas());

        verify(notificacionDAO, never()).save(any());
    }

    @Test
    @DisplayName("enviarRecordatoriosSalasProximas: sin reservas proximas devuelve 0")
    void recordatoriosSalas_sinReservas_devuelveCero() {
        when(reservaSalaDAO.findByEstadoInAndFechaInicioBetween(eq(List.of("CONFIRMADA")),
                any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());

        assertEquals(0, service.enviarRecordatoriosSalasProximas());

        verifyNoInteractions(notificacionDAO, salaEstudioDAO);
    }
}
