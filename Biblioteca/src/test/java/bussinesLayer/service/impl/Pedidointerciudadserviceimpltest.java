package bussinesLayer.service.impl;

import bussinesLayer.dto.EjemplarDTO;
import bussinesLayer.dto.MultaDTO;
import bussinesLayer.dto.NotificacionDTO;
import bussinesLayer.dto.PagoDTO;
import bussinesLayer.dto.PedidoInterciudadDTO;
import bussinesLayer.dto.PrestamoDTO;
import bussinesLayer.dto.SedeDTO;
import bussinesLayer.dto.SolicitudPedidoInterciudadDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.AutomatizacionService;
import bussinesLayer.service.ConfiguracionSistemaService;
import bussinesLayer.service.NotificacionService;
import bussinesLayer.service.ReservaLibroService;
import bussinesLayer.service.SuscripcionService;
import bussinesLayer.service.TarifaEnvioService;
import bussinesLayer.service.ValidadorUsuarioService;
import bussinesLayer.util.ClavePlazo;
import bussinesLayer.util.ConceptoMulta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.EjemplarDAO;
import persistanceLayer.dao.MultaDAO;
import persistanceLayer.dao.NotificacionDAO;
import persistanceLayer.dao.PagoDAO;
import persistanceLayer.dao.PedidoInterciudadDAO;
import persistanceLayer.dao.PrestamoDAO;
import persistanceLayer.dao.SancionDAO;
import persistanceLayer.dao.SedeDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.math.BigDecimal;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoInterciudadServiceImpl")
class PedidoInterciudadServiceImplTest {

    private static final Long USUARIO_ID = 1L;
    private static final Long LIBRO_ID = 2L;
    private static final Long SEDE_ORIGEN_ID = 3L;
    private static final Long SEDE_DEVOLUCION_ID = 4L;
    private static final Long PEDIDO_ID = 5L;
    private static final Long EJEMPLAR_ID = 20L;
    private static final Long PRESTAMO_ID = 30L;
    private static final Long BIBLIOTECARIO_ID = 40L;
    private static final Long REPARTIDOR_ID = 50L;
    private static final String CIUDAD_ORIGEN = "Bogota";
    private static final String CIUDAD_ENTREGA = "Cali";

    @Mock private PedidoInterciudadDAO pedidoDAO;
    @Mock private UsuarioDAO usuarioDAO;
    @Mock private EjemplarDAO ejemplarDAO;
    @Mock private SedeDAO sedeDAO;
    @Mock private PagoDAO pagoDAO;
    @Mock private PrestamoDAO prestamoDAO;
    @Mock private SancionDAO sancionDAO;
    @Mock private MultaDAO multaDAO;
    @Mock private NotificacionDAO notificacionDAO;
    @Mock private SuscripcionService suscripcionService;
    @Mock private TarifaEnvioService tarifaEnvioService;
    @Mock private ConfiguracionSistemaService configuracionService;
    @Mock private ReservaLibroService reservaLibroService;
    @Mock private NotificacionService notificacionService;
    @Mock private ValidadorUsuarioService validadorUsuarioService;
    @Mock private AutomatizacionService automatizacionService;

    @InjectMocks
    private PedidoInterciudadServiceImpl service;

    private SolicitudPedidoInterciudadDTO solicitud;

    @BeforeEach
    void setUp() {
        solicitud = new SolicitudPedidoInterciudadDTO();
        solicitud.setUsuarioId(USUARIO_ID);
        solicitud.setLibroId(LIBRO_ID);
        solicitud.setCiudadEntrega(CIUDAD_ENTREGA);
        solicitud.setDireccionEntrega("  Calle 1 # 2-3  ");
        solicitud.setSedeDevolucionId(SEDE_DEVOLUCION_ID);
        solicitud.setFechaRecepcion(LocalDate.now().plusDays(1));
        solicitud.setHoraRecepcion(" 10:00 ");
    }

    // ---------------------------------------------------------------- helpers

    private PedidoInterciudadDTO pedido(String estado) {
        PedidoInterciudadDTO p = new PedidoInterciudadDTO();
        p.setId(PEDIDO_ID);
        p.setEstado(estado);
        p.setUsuarioId(USUARIO_ID);
        p.setEjemplarId(EJEMPLAR_ID);
        p.setSedeOrigenId(SEDE_ORIGEN_ID);
        p.setSedeDevolucionId(SEDE_DEVOLUCION_ID);
        p.setSedeDevolucionNombre("Sede Norte");
        p.setPrestamoId(PRESTAMO_ID);
        return p;
    }

    private PrestamoDTO prestamo() {
        PrestamoDTO p = new PrestamoDTO();
        p.setId(PRESTAMO_ID);
        p.setEstado("SOLICITADO");
        return p;
    }

    private SedeDTO sede(Long id, String ciudad, boolean enOperacion) {
        SedeDTO s = new SedeDTO();
        s.setId(id);
        s.setNombre("Sede " + id);
        s.setCiudad(ciudad);
        s.setEnOperacion(enOperacion);
        return s;
    }

    private EjemplarDTO ejemplar(Long id) {
        EjemplarDTO e = new EjemplarDTO();
        e.setId(id);
        e.setLibroId(LIBRO_ID);
        e.setSedeId(SEDE_ORIGEN_ID);
        e.setEstado("DISPONIBLE");
        return e;
    }

    private PagoDTO pagoEnvio() {
        PagoDTO p = new PagoDTO();
        p.setId(99L);
        p.setMonto(new BigDecimal("5000"));
        p.setReferencia("REF-1");
        p.setEstado("APROBADO");
        return p;
    }

    /** Stubs comunes de una solicitud que supera las validaciones previas al ejemplar. */
    private void referenciasSolicitudValidas() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(sedeDAO.findById(SEDE_DEVOLUCION_ID)).thenReturn(Optional.of(sede(SEDE_DEVOLUCION_ID, "Medellin", true)));
    }

    private void sedeOrigenConCiudad() {
        when(sedeDAO.findById(SEDE_ORIGEN_ID)).thenReturn(Optional.of(sede(SEDE_ORIGEN_ID, CIUDAD_ORIGEN, true)));
    }

    private void pedidoExiste(PedidoInterciudadDTO pedido) {
        when(pedidoDAO.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
    }

    private void actualizacionDevuelveElPedido() {
        when(pedidoDAO.update(eq(PEDIDO_ID), any(PedidoInterciudadDTO.class)))
                .thenAnswer(inv -> Optional.of(inv.getArgument(1)));
    }

    private void cambioDeEstadoEjemplar(String esperado, String nuevo, int filas) {
        when(ejemplarDAO.cambiarEstadoSiEs(EJEMPLAR_ID, esperado, nuevo)).thenReturn(filas);
    }

    private NotificacionDTO notificacionGuardada() {
        ArgumentCaptor<NotificacionDTO> captor = ArgumentCaptor.forClass(NotificacionDTO.class);
        verify(notificacionDAO).save(captor.capture());
        return captor.getValue();
    }

    // ---------------------------------------------------------------- solicitarPedido

    @Test
    @DisplayName("solicitarPedido: usuario inexistente lanza ResourceNotFoundException")
    void solicitar_usuarioInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.solicitarPedido(solicitud));

        verifyNoInteractions(suscripcionService, pedidoDAO);
    }

    @Test
    @DisplayName("solicitarPedido: sede de devolucion inexistente lanza ResourceNotFoundException")
    void solicitar_sedeDevolucionInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(sedeDAO.findById(SEDE_DEVOLUCION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.solicitarPedido(solicitud));

        verify(pedidoDAO, never()).save(any());
    }

    @Test
    @DisplayName("solicitarPedido: sede de devolucion fuera de operacion lanza BusinessValidationException")
    void solicitar_sedeFueraDeOperacion_lanzaBusinessValidation() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(sedeDAO.findById(SEDE_DEVOLUCION_ID)).thenReturn(Optional.of(sede(SEDE_DEVOLUCION_ID, "Medellin", false)));

        assertThrows(BusinessValidationException.class, () -> service.solicitarPedido(solicitud));

        verify(pedidoDAO, never()).save(any());
    }

    @Test
    @DisplayName("solicitarPedido: fecha de recepcion a mas de 7 dias lanza BusinessValidationException")
    void solicitar_fechaMuyLejana_lanzaBusinessValidation() {
        referenciasSolicitudValidas();
        solicitud.setFechaRecepcion(LocalDate.now().plusDays(8));

        assertThrows(BusinessValidationException.class, () -> service.solicitarPedido(solicitud));

        verify(pedidoDAO, never()).save(any());
    }

    @Test
    @DisplayName("solicitarPedido: sin ejemplares en otras ciudades lanza BusinessValidationException")
    void solicitar_sinEjemplares_lanzaBusinessValidation() {
        referenciasSolicitudValidas();
        when(ejemplarDAO.findDisponiblesEnOtraCiudad(LIBRO_ID, CIUDAD_ENTREGA)).thenReturn(List.of());

        assertThrows(BusinessValidationException.class, () -> service.solicitarPedido(solicitud));

        verify(pedidoDAO, never()).save(any());
    }

    @Test
    @DisplayName("solicitarPedido: ejemplares sin tarifa de envio lanza BusinessValidationException")
    void solicitar_sinTarifa_lanzaBusinessValidation() {
        referenciasSolicitudValidas();
        sedeOrigenConCiudad();
        when(ejemplarDAO.findDisponiblesEnOtraCiudad(LIBRO_ID, CIUDAD_ENTREGA)).thenReturn(List.of(ejemplar(EJEMPLAR_ID)));
        when(tarifaEnvioService.buscarCosto(CIUDAD_ORIGEN, CIUDAD_ENTREGA)).thenReturn(Optional.empty());

        assertThrows(BusinessValidationException.class, () -> service.solicitarPedido(solicitud));

        verify(ejemplarDAO, never()).cambiarEstadoSiEs(any(), any(), any());
        verify(pedidoDAO, never()).save(any());
    }

    @Test
    @DisplayName("solicitarPedido: si otro tomo todos los ejemplares lanza BusinessValidationException")
    void solicitar_ejemplaresYaTomados_lanzaBusinessValidation() {
        referenciasSolicitudValidas();
        sedeOrigenConCiudad();
        when(ejemplarDAO.findDisponiblesEnOtraCiudad(LIBRO_ID, CIUDAD_ENTREGA)).thenReturn(List.of(ejemplar(EJEMPLAR_ID)));
        when(tarifaEnvioService.buscarCosto(CIUDAD_ORIGEN, CIUDAD_ENTREGA)).thenReturn(Optional.of(new BigDecimal("5000")));
        cambioDeEstadoEjemplar("DISPONIBLE", "RESERVADO", 0);

        assertThrows(BusinessValidationException.class, () -> service.solicitarPedido(solicitud));

        verify(pedidoDAO, never()).save(any());
    }

    @Test
    @DisplayName("solicitarPedido: crea prestamo y pedido PENDIENTE_PAGO, con textos recortados, y notifica")
    void solicitar_valida_creaPedidoPendienteDePago() {
        referenciasSolicitudValidas();
        sedeOrigenConCiudad();
        when(ejemplarDAO.findDisponiblesEnOtraCiudad(LIBRO_ID, CIUDAD_ENTREGA)).thenReturn(List.of(ejemplar(EJEMPLAR_ID)));
        when(tarifaEnvioService.buscarCosto(CIUDAD_ORIGEN, CIUDAD_ENTREGA)).thenReturn(Optional.of(new BigDecimal("5000")));
        cambioDeEstadoEjemplar("DISPONIBLE", "RESERVADO", 1);
        when(tarifaEnvioService.calcularCosto(CIUDAD_ORIGEN, CIUDAD_ENTREGA)).thenReturn(new BigDecimal("5000"));
        when(prestamoDAO.save(any(PrestamoDTO.class))).thenAnswer(inv -> {
            PrestamoDTO p = inv.getArgument(0);
            p.setId(PRESTAMO_ID);
            return p;
        });
        when(pedidoDAO.save(any(PedidoInterciudadDTO.class))).thenAnswer(inv -> {
            PedidoInterciudadDTO p = inv.getArgument(0);
            p.setId(PEDIDO_ID);
            return p;
        });

        PedidoInterciudadDTO resultado = service.solicitarPedido(solicitud);

        assertEquals(PEDIDO_ID, resultado.getId());
        assertEquals("PENDIENTE_PAGO", resultado.getEstado());
        assertEquals(CIUDAD_ENTREGA, resultado.getCiudadEntrega());
        assertEquals("Calle 1 # 2-3", resultado.getDireccionEntrega());
        assertEquals("10:00", resultado.getHoraRecepcion());
        assertEquals(5000.0, resultado.getCostoEnvio());
        assertEquals(EJEMPLAR_ID, resultado.getEjemplarId());
        assertEquals(SEDE_ORIGEN_ID, resultado.getSedeOrigenId());
        assertEquals(SEDE_DEVOLUCION_ID, resultado.getSedeDevolucionId());
        assertEquals(PRESTAMO_ID, resultado.getPrestamoId());

        ArgumentCaptor<PrestamoDTO> prestamoCaptor = ArgumentCaptor.forClass(PrestamoDTO.class);
        verify(prestamoDAO).save(prestamoCaptor.capture());
        assertEquals("INTERCIUDAD", prestamoCaptor.getValue().getModalidad());
        assertEquals("SOLICITADO", prestamoCaptor.getValue().getEstado());

        NotificacionDTO n = notificacionGuardada();
        assertEquals("PEDIDO_PENDIENTE_PAGO", n.getTipo());
        assertEquals(USUARIO_ID, n.getUsuarioId());
    }

    @Test
    @DisplayName("solicitarPedido: si el primer ejemplar ya fue tomado reserva el siguiente candidato")
    void solicitar_primerCandidatoTomado_usaElSiguiente() {
        referenciasSolicitudValidas();
        sedeOrigenConCiudad();
        when(ejemplarDAO.findDisponiblesEnOtraCiudad(LIBRO_ID, CIUDAD_ENTREGA))
                .thenReturn(List.of(ejemplar(EJEMPLAR_ID), ejemplar(21L)));
        when(tarifaEnvioService.buscarCosto(CIUDAD_ORIGEN, CIUDAD_ENTREGA)).thenReturn(Optional.of(new BigDecimal("5000")));
        cambioDeEstadoEjemplar("DISPONIBLE", "RESERVADO", 0);
        when(ejemplarDAO.cambiarEstadoSiEs(21L, "DISPONIBLE", "RESERVADO")).thenReturn(1);
        when(tarifaEnvioService.calcularCosto(CIUDAD_ORIGEN, CIUDAD_ENTREGA)).thenReturn(new BigDecimal("5000"));
        when(prestamoDAO.save(any(PrestamoDTO.class))).thenAnswer(inv -> {
            PrestamoDTO p = inv.getArgument(0);
            p.setId(PRESTAMO_ID);
            return p;
        });
        when(pedidoDAO.save(any(PedidoInterciudadDTO.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoInterciudadDTO resultado = service.solicitarPedido(solicitud);

        assertEquals(21L, resultado.getEjemplarId());
    }

    // ---------------------------------------------------------------- cancelarPedido

    @Test
    @DisplayName("cancelarPedido: pedido inexistente lanza ResourceNotFoundException")
    void cancelar_inexistente_lanzaNotFound() {
        when(pedidoDAO.findById(PEDIDO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.cancelarPedido(PEDIDO_ID, USUARIO_ID));
    }

    @Test
    @DisplayName("cancelarPedido: pedido de otro usuario lanza BusinessValidationException")
    void cancelar_otroUsuario_lanzaBusinessValidation() {
        pedidoExiste(pedido("SOLICITADO"));

        assertThrows(BusinessValidationException.class, () -> service.cancelarPedido(PEDIDO_ID, 99L));

        verify(pedidoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("cancelarPedido: estado no cancelable notifica el bloqueo y lanza BusinessValidationException")
    void cancelar_estadoNoCancelable_notificaYLanza() {
        pedidoExiste(pedido("ENTREGADO"));

        assertThrows(BusinessValidationException.class, () -> service.cancelarPedido(PEDIDO_ID, USUARIO_ID));

        assertEquals("CANCELACION_BLOQUEADA", notificacionGuardada().getTipo());
        verify(pedidoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("cancelarPedido: si el ejemplar no estaba reservado lanza BusinessValidationException")
    void cancelar_ejemplarNoReservado_lanzaBusinessValidation() {
        pedidoExiste(pedido("PENDIENTE_PAGO"));
        cambioDeEstadoEjemplar("RESERVADO", "DISPONIBLE", 0);

        assertThrows(BusinessValidationException.class, () -> service.cancelarPedido(PEDIDO_ID, USUARIO_ID));

        verify(pedidoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("cancelarPedido: PENDIENTE_PAGO sin pago libera el ejemplar, cancela prestamo y pedido")
    void cancelar_pendientePago_cancelaSinReembolso() {
        pedidoExiste(pedido("PENDIENTE_PAGO"));
        cambioDeEstadoEjemplar("RESERVADO", "DISPONIBLE", 1);
        PrestamoDTO prestamo = prestamo();
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo));
        when(pagoDAO.findEnvioPagadoPorPedido(PEDIDO_ID)).thenReturn(Optional.empty());
        actualizacionDevuelveElPedido();

        PedidoInterciudadDTO resultado = service.cancelarPedido(PEDIDO_ID, USUARIO_ID);

        assertEquals("CANCELADO", resultado.getEstado());
        assertNotNull(resultado.getFechaCancelacion());
        assertEquals("CANCELADO", prestamo.getEstado());
        verify(prestamoDAO).update(PRESTAMO_ID, prestamo);
        verify(pagoDAO, never()).update(any(), any());
        assertEquals("PEDIDO_CANCELADO", notificacionGuardada().getTipo());
    }

    @Test
    @DisplayName("cancelarPedido: SOLICITADO con envio pagado reembolsa el pago y lo informa")
    void cancelar_solicitadoConPago_reembolsaElEnvio() {
        pedidoExiste(pedido("SOLICITADO"));
        cambioDeEstadoEjemplar("RESERVADO", "DISPONIBLE", 1);
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo()));
        PagoDTO pago = pagoEnvio();
        when(pagoDAO.findEnvioPagadoPorPedido(PEDIDO_ID)).thenReturn(Optional.of(pago));
        actualizacionDevuelveElPedido();

        service.cancelarPedido(PEDIDO_ID, USUARIO_ID);

        assertEquals("REEMBOLSADO", pago.getEstado());
        verify(pagoDAO).update(99L, pago);
        assertTrue(notificacionGuardada().getMensaje().contains("REF-1"));
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerPedidoPorId: devuelve el pedido cuando existe")
    void obtenerPorId_existente_loDevuelve() {
        PedidoInterciudadDTO existente = pedido("SOLICITADO");
        pedidoExiste(existente);

        assertSame(existente, service.obtenerPedidoPorId(PEDIDO_ID));
    }

    @Test
    @DisplayName("obtenerPedidoPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerPorId_inexistente_lanzaNotFound() {
        when(pedidoDAO.findById(PEDIDO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerPedidoPorId(PEDIDO_ID));
    }

    @Test
    @DisplayName("obtenerPedidosDeUsuario: sin estado lista todos los del usuario")
    void obtenerDeUsuario_sinEstado_listaTodos() {
        when(pedidoDAO.findByUsuarioIdOrderByFechaSolicitudDesc(USUARIO_ID)).thenReturn(List.of(pedido("SOLICITADO")));

        assertEquals(1, service.obtenerPedidosDeUsuario(USUARIO_ID, null).size());
    }

    @Test
    @DisplayName("obtenerPedidosDeUsuario: estado en blanco se trata como sin filtro")
    void obtenerDeUsuario_estadoEnBlanco_listaTodos() {
        when(pedidoDAO.findByUsuarioIdOrderByFechaSolicitudDesc(USUARIO_ID)).thenReturn(List.of());

        assertTrue(service.obtenerPedidosDeUsuario(USUARIO_ID, "  ").isEmpty());
    }

    @Test
    @DisplayName("obtenerPedidosDeUsuario: con estado filtra por ese estado")
    void obtenerDeUsuario_conEstado_filtra() {
        when(pedidoDAO.findByUsuarioIdAndEstadoIgnoreCaseOrderByFechaSolicitudDesc(USUARIO_ID, "ENTREGADO"))
                .thenReturn(List.of(pedido("ENTREGADO")));

        assertEquals(1, service.obtenerPedidosDeUsuario(USUARIO_ID, "ENTREGADO").size());
    }

    @Test
    @DisplayName("obtenerPedidosSedeOrigen: consulta los estados SOLICITADO y EN_CAMINO_SEDE_ORIGEN")
    void obtenerSedeOrigen_consultaEstadosEsperados() {
        when(pedidoDAO.findBySedeOrigenAndEstados(SEDE_ORIGEN_ID, List.of("SOLICITADO", "EN_CAMINO_SEDE_ORIGEN")))
                .thenReturn(List.of(pedido("SOLICITADO")));

        assertEquals(1, service.obtenerPedidosSedeOrigen(SEDE_ORIGEN_ID).size());
    }

    @Test
    @DisplayName("obtenerPedidosSedeDevolucion: sede inexistente lanza ResourceNotFoundException")
    void obtenerSedeDevolucion_sedeInexistente_lanzaNotFound() {
        when(sedeDAO.existsById(SEDE_DEVOLUCION_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerPedidosSedeDevolucion(SEDE_DEVOLUCION_ID));
    }

    @Test
    @DisplayName("obtenerPedidosSedeDevolucion: consulta los estados que llegan o esperan en la sede")
    void obtenerSedeDevolucion_consultaEstadosEsperados() {
        when(sedeDAO.existsById(SEDE_DEVOLUCION_ID)).thenReturn(true);
        when(pedidoDAO.findBySedeDevolucionAndEstados(SEDE_DEVOLUCION_ID, List.of(
                "EN_TRANSITO", "ENTREGADO", "NO_RECIBIDO", "EN_ESPERA_DE_RECOGIDA", "DEVUELTO_PENDIENTE_TRASLADO")))
                .thenReturn(List.of(pedido("EN_TRANSITO")));

        assertEquals(1, service.obtenerPedidosSedeDevolucion(SEDE_DEVOLUCION_ID).size());
    }

    // ---------------------------------------------------------------- despacharPedido

    @Test
    @DisplayName("despacharPedido: pedido inexistente lanza ResourceNotFoundException")
    void despachar_inexistente_lanzaNotFound() {
        when(pedidoDAO.findById(PEDIDO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.despacharPedido(PEDIDO_ID, BIBLIOTECARIO_ID, REPARTIDOR_ID));
    }

    @Test
    @DisplayName("despacharPedido: estado distinto de SOLICITADO lanza BusinessValidationException")
    void despachar_estadoIncorrecto_lanzaBusinessValidation() {
        pedidoExiste(pedido("PENDIENTE_PAGO"));

        assertThrows(BusinessValidationException.class,
                () -> service.despacharPedido(PEDIDO_ID, BIBLIOTECARIO_ID, REPARTIDOR_ID));

        verify(pedidoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("despacharPedido: sin envio pagado lanza BusinessValidationException")
    void despachar_sinPago_lanzaBusinessValidation() {
        pedidoExiste(pedido("SOLICITADO"));
        when(pagoDAO.findEnvioPagadoPorPedido(PEDIDO_ID)).thenReturn(Optional.empty());

        assertThrows(BusinessValidationException.class,
                () -> service.despacharPedido(PEDIDO_ID, BIBLIOTECARIO_ID, REPARTIDOR_ID));

        verify(ejemplarDAO, never()).cambiarEstadoSiEs(any(), any(), any());
    }

    @Test
    @DisplayName("despacharPedido: si el ejemplar no estaba reservado lanza BusinessValidationException")
    void despachar_ejemplarNoReservado_lanzaBusinessValidation() {
        pedidoExiste(pedido("SOLICITADO"));
        when(pagoDAO.findEnvioPagadoPorPedido(PEDIDO_ID)).thenReturn(Optional.of(pagoEnvio()));
        cambioDeEstadoEjemplar("RESERVADO", "EN_TRANSITO", 0);

        assertThrows(BusinessValidationException.class,
                () -> service.despacharPedido(PEDIDO_ID, BIBLIOTECARIO_ID, REPARTIDOR_ID));

        verify(pedidoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("despacharPedido: pasa a EN_TRANSITO, asigna responsables y notifica al usuario y a la sede")
    void despachar_valido_quedaEnTransito() {
        pedidoExiste(pedido("SOLICITADO"));
        when(pagoDAO.findEnvioPagadoPorPedido(PEDIDO_ID)).thenReturn(Optional.of(pagoEnvio()));
        cambioDeEstadoEjemplar("RESERVADO", "EN_TRANSITO", 1);
        actualizacionDevuelveElPedido();

        PedidoInterciudadDTO resultado = service.despacharPedido(PEDIDO_ID, BIBLIOTECARIO_ID, REPARTIDOR_ID);

        assertEquals("EN_TRANSITO", resultado.getEstado());
        assertNotNull(resultado.getFechaTransito());
        assertEquals(BIBLIOTECARIO_ID, resultado.getBibliotecarioId());
        assertEquals(REPARTIDOR_ID, resultado.getRepartidorId());
        assertEquals("PEDIDO_EN_TRANSITO", notificacionGuardada().getTipo());
        verify(notificacionService).notificarSede(eq(SEDE_DEVOLUCION_ID), eq("PEDIDO_DESPACHADO"), anyString(), anyString());
    }

    // ---------------------------------------------------------------- registrarDevolucionEnSede

    @Test
    @DisplayName("registrarDevolucionEnSede: estado distinto de ENTREGADO lanza BusinessValidationException")
    void devolucion_estadoIncorrecto_lanzaBusinessValidation() {
        pedidoExiste(pedido("EN_TRANSITO"));

        assertThrows(BusinessValidationException.class,
                () -> service.registrarDevolucionEnSede(PEDIDO_ID, BIBLIOTECARIO_ID, SEDE_DEVOLUCION_ID));

        verifyNoInteractions(automatizacionService);
    }

    @Test
    @DisplayName("registrarDevolucionEnSede: sede distinta a la elegida lanza BusinessValidationException")
    void devolucion_sedeIncorrecta_lanzaBusinessValidation() {
        pedidoExiste(pedido("ENTREGADO"));

        assertThrows(BusinessValidationException.class,
                () -> service.registrarDevolucionEnSede(PEDIDO_ID, BIBLIOTECARIO_ID, 99L));

        verifyNoInteractions(automatizacionService);
    }

    @Test
    @DisplayName("registrarDevolucionEnSede: si el ejemplar no estaba PRESTADO lanza BusinessValidationException")
    void devolucion_ejemplarNoPrestado_lanzaBusinessValidation() {
        pedidoExiste(pedido("ENTREGADO"));
        when(automatizacionService.evaluarRetrasoAlDevolver(PRESTAMO_ID)).thenReturn(0L);
        cambioDeEstadoEjemplar("PRESTADO", "EN_TRANSITO", 0);

        assertThrows(BusinessValidationException.class,
                () -> service.registrarDevolucionEnSede(PEDIDO_ID, BIBLIOTECARIO_ID, SEDE_DEVOLUCION_ID));

        verify(pedidoDAO, never()).update(any(), any());
    }

    private PedidoInterciudadDTO devolucionConRetraso(long diasRetraso) {
        pedidoExiste(pedido("ENTREGADO"));
        when(automatizacionService.evaluarRetrasoAlDevolver(PRESTAMO_ID)).thenReturn(diasRetraso);
        cambioDeEstadoEjemplar("PRESTADO", "EN_TRANSITO", 1);
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo()));
        actualizacionDevuelveElPedido();

        return service.registrarDevolucionEnSede(PEDIDO_ID, BIBLIOTECARIO_ID, SEDE_DEVOLUCION_ID);
    }

    @Test
    @DisplayName("registrarDevolucionEnSede: a tiempo deja DEVUELTO_PENDIENTE_TRASLADO sin aviso de retraso")
    void devolucion_aTiempo_pendienteDeTraslado() {
        PedidoInterciudadDTO resultado = devolucionConRetraso(0);

        assertEquals("DEVUELTO_PENDIENTE_TRASLADO", resultado.getEstado());
        assertEquals(BIBLIOTECARIO_ID, resultado.getBibliotecarioId());
        NotificacionDTO n = notificacionGuardada();
        assertEquals("LIBRO_DEVUELTO", n.getTipo());
        assertFalse(n.getMensaje().contains("retraso"));
        verify(notificacionService).notificarSede(eq(SEDE_ORIGEN_ID), eq("LIBRO_DEVUELTO_REGRESA"), anyString(), anyString());
    }

    @Test
    @DisplayName("registrarDevolucionEnSede: con 1 dia de retraso avisa en singular")
    void devolucion_unDiaDeRetraso_avisaEnSingular() {
        devolucionConRetraso(1);

        assertTrue(notificacionGuardada().getMensaje().contains("1 día de retraso"));
    }

    @Test
    @DisplayName("registrarDevolucionEnSede: con varios dias de retraso avisa en plural")
    void devolucion_variosDiasDeRetraso_avisaEnPlural() {
        devolucionConRetraso(3);

        assertTrue(notificacionGuardada().getMensaje().contains("3 días de retraso"));
    }

    @Test
    @DisplayName("registrarDevolucionEnSede: marca el prestamo como DEVUELTO con su fecha")
    void devolucion_valida_marcaPrestamoDevuelto() {
        pedidoExiste(pedido("ENTREGADO"));
        when(automatizacionService.evaluarRetrasoAlDevolver(PRESTAMO_ID)).thenReturn(0L);
        cambioDeEstadoEjemplar("PRESTADO", "EN_TRANSITO", 1);
        PrestamoDTO prestamo = prestamo();
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo));
        actualizacionDevuelveElPedido();

        service.registrarDevolucionEnSede(PEDIDO_ID, BIBLIOTECARIO_ID, SEDE_DEVOLUCION_ID);

        assertEquals("DEVUELTO", prestamo.getEstado());
        assertNotNull(prestamo.getFechaDevolucion());
        verify(prestamoDAO).update(PRESTAMO_ID, prestamo);
    }

    // ---------------------------------------------------------------- registrarRecepcionNoRecibido

    @Test
    @DisplayName("registrarRecepcionNoRecibido: estado distinto de NO_RECIBIDO lanza BusinessValidationException")
    void noRecibido_estadoIncorrecto_lanzaBusinessValidation() {
        pedidoExiste(pedido("ENTREGADO"));

        assertThrows(BusinessValidationException.class,
                () -> service.registrarRecepcionNoRecibido(PEDIDO_ID, BIBLIOTECARIO_ID));

        verify(pedidoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("registrarRecepcionNoRecibido: pasa a EN_ESPERA_DE_RECOGIDA con el plazo configurado y notifica")
    void noRecibido_valido_quedaEnEsperaDeRecogida() {
        pedidoExiste(pedido("NO_RECIBIDO"));
        when(configuracionService.plazo(ClavePlazo.DIAS_RECOGIDA_SEDE_DEVOLUCION)).thenReturn(2);
        actualizacionDevuelveElPedido();

        PedidoInterciudadDTO resultado = service.registrarRecepcionNoRecibido(PEDIDO_ID, BIBLIOTECARIO_ID);

        assertEquals("EN_ESPERA_DE_RECOGIDA", resultado.getEstado());
        assertEquals(BIBLIOTECARIO_ID, resultado.getBibliotecarioId());
        assertTrue(resultado.getFechaLimiteRecogida().isAfter(LocalDateTime.now().plusDays(1)));
        assertEquals("EN_ESPERA_DE_RECOGIDA", notificacionGuardada().getTipo());
        verify(notificacionService).notificarSede(eq(SEDE_ORIGEN_ID), eq("LIBRO_DEVUELTO_REGRESA"), anyString(), anyString());
    }

    // ---------------------------------------------------------------- registrarEntregaEnSede

    @Test
    @DisplayName("registrarEntregaEnSede: estado distinto de EN_ESPERA_DE_RECOGIDA lanza BusinessValidationException")
    void entregaEnSede_estadoIncorrecto_lanzaBusinessValidation() {
        pedidoExiste(pedido("NO_RECIBIDO"));

        assertThrows(BusinessValidationException.class,
                () -> service.registrarEntregaEnSede(PEDIDO_ID, BIBLIOTECARIO_ID));

        verifyNoInteractions(configuracionService);
    }

    @Test
    @DisplayName("registrarEntregaEnSede: si el ejemplar no estaba EN_TRANSITO lanza BusinessValidationException")
    void entregaEnSede_ejemplarNoEnTransito_lanzaBusinessValidation() {
        pedidoExiste(pedido("EN_ESPERA_DE_RECOGIDA"));
        when(configuracionService.plazo(ClavePlazo.DIAS_PRESTAMO_INTERCIUDAD)).thenReturn(14);
        cambioDeEstadoEjemplar("EN_TRANSITO", "PRESTADO", 0);

        assertThrows(BusinessValidationException.class,
                () -> service.registrarEntregaEnSede(PEDIDO_ID, BIBLIOTECARIO_ID));

        verify(pedidoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("registrarEntregaEnSede: entrega, activa el prestamo y fija el limite de devolucion")
    void entregaEnSede_valida_activaPrestamo() {
        pedidoExiste(pedido("EN_ESPERA_DE_RECOGIDA"));
        when(configuracionService.plazo(ClavePlazo.DIAS_PRESTAMO_INTERCIUDAD)).thenReturn(14);
        cambioDeEstadoEjemplar("EN_TRANSITO", "PRESTADO", 1);
        PrestamoDTO prestamo = prestamo();
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo));
        actualizacionDevuelveElPedido();

        PedidoInterciudadDTO resultado = service.registrarEntregaEnSede(PEDIDO_ID, BIBLIOTECARIO_ID);

        assertEquals("ENTREGADO", resultado.getEstado());
        assertNotNull(resultado.getFechaEntrega());
        assertEquals(resultado.getFechaEntrega().plusDays(14), resultado.getFechaLimiteDevolucion());
        assertEquals(BIBLIOTECARIO_ID, resultado.getBibliotecarioId());
        assertEquals("ACTIVO", prestamo.getEstado());
        assertNotNull(prestamo.getFechaVencimiento());
        verify(prestamoDAO).update(PRESTAMO_ID, prestamo);
    }

    // ---------------------------------------------------------------- registrarRecepcionEnSedeOrigen

    @Test
    @DisplayName("registrarRecepcionEnSedeOrigen: estado distinto de EN_CAMINO_SEDE_ORIGEN lanza BusinessValidationException")
    void recepcionOrigen_estadoIncorrecto_lanzaBusinessValidation() {
        pedidoExiste(pedido("ENTREGADO"));

        assertThrows(BusinessValidationException.class,
                () -> service.registrarRecepcionEnSedeOrigen(PEDIDO_ID, BIBLIOTECARIO_ID));

        verifyNoInteractions(reservaLibroService);
    }

    @Test
    @DisplayName("registrarRecepcionEnSedeOrigen: si el ejemplar no estaba EN_TRANSITO lanza BusinessValidationException")
    void recepcionOrigen_ejemplarNoEnTransito_lanzaBusinessValidation() {
        pedidoExiste(pedido("EN_CAMINO_SEDE_ORIGEN"));
        cambioDeEstadoEjemplar("EN_TRANSITO", "DISPONIBLE", 0);

        assertThrows(BusinessValidationException.class,
                () -> service.registrarRecepcionEnSedeOrigen(PEDIDO_ID, BIBLIOTECARIO_ID));

        verifyNoInteractions(reservaLibroService);
    }

    @Test
    @DisplayName("registrarRecepcionEnSedeOrigen: cierra el pedido y ofrece el ejemplar a las reservas en espera")
    void recepcionOrigen_valida_cierraYAsignaReservas() {
        pedidoExiste(pedido("EN_CAMINO_SEDE_ORIGEN"));
        cambioDeEstadoEjemplar("EN_TRANSITO", "DISPONIBLE", 1);
        actualizacionDevuelveElPedido();

        PedidoInterciudadDTO resultado = service.registrarRecepcionEnSedeOrigen(PEDIDO_ID, BIBLIOTECARIO_ID);

        assertEquals("CERRADO", resultado.getEstado());
        assertEquals(BIBLIOTECARIO_ID, resultado.getBibliotecarioId());
        verify(reservaLibroService).asignarEjemplarAReservaEnEspera(EJEMPLAR_ID);
    }

    // ---------------------------------------------------------------- obtenerAsignacionesRepartidor

    @Test
    @DisplayName("obtenerAsignacionesRepartidor: repartidor inexistente lanza ResourceNotFoundException")
    void asignaciones_repartidorInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(REPARTIDOR_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerAsignacionesRepartidor(REPARTIDOR_ID));
    }

    @Test
    @DisplayName("obtenerAsignacionesRepartidor: combina asignados y traslados pendientes sin duplicados")
    void asignaciones_combinaSinDuplicados() {
        PedidoInterciudadDTO p1 = pedido("EN_TRANSITO");
        p1.setId(1L);
        PedidoInterciudadDTO p2 = pedido("EN_CAMINO_SEDE_ORIGEN");
        p2.setId(2L);
        PedidoInterciudadDTO p2Repetido = pedido("DEVUELTO_PENDIENTE_TRASLADO");
        p2Repetido.setId(2L);
        PedidoInterciudadDTO p3 = pedido("DEVUELTO_PENDIENTE_TRASLADO");
        p3.setId(3L);

        when(usuarioDAO.existsById(REPARTIDOR_ID)).thenReturn(true);
        when(pedidoDAO.findByRepartidorIdAndEstados(REPARTIDOR_ID,
                List.of("EN_TRANSITO", "EN_CAMINO_SEDE_ORIGEN", "DEVUELTO_PENDIENTE_TRASLADO")))
                .thenReturn(List.of(p1, p2));
        when(pedidoDAO.findPendientesDeTraslado()).thenReturn(List.of(p2Repetido, p3));

        List<PedidoInterciudadDTO> resultado = service.obtenerAsignacionesRepartidor(REPARTIDOR_ID);

        assertEquals(3, resultado.size());
        assertSame(p1, resultado.get(0));
        assertSame(p2, resultado.get(1));
        assertSame(p3, resultado.get(2));
    }

    // ---------------------------------------------------------------- confirmarEntregaDomicilio

    @Test
    @DisplayName("confirmarEntregaDomicilio: estado distinto de EN_TRANSITO lanza BusinessValidationException")
    void entregaDomicilio_estadoIncorrecto_lanzaBusinessValidation() {
        pedidoExiste(pedido("SOLICITADO"));

        assertThrows(BusinessValidationException.class,
                () -> service.confirmarEntregaDomicilio(PEDIDO_ID, REPARTIDOR_ID));

        verifyNoInteractions(configuracionService);
    }

    @Test
    @DisplayName("confirmarEntregaDomicilio: si el ejemplar no estaba EN_TRANSITO lanza BusinessValidationException")
    void entregaDomicilio_ejemplarNoEnTransito_lanzaBusinessValidation() {
        pedidoExiste(pedido("EN_TRANSITO"));
        when(configuracionService.plazo(ClavePlazo.DIAS_PRESTAMO_INTERCIUDAD)).thenReturn(14);
        cambioDeEstadoEjemplar("EN_TRANSITO", "PRESTADO", 0);

        assertThrows(BusinessValidationException.class,
                () -> service.confirmarEntregaDomicilio(PEDIDO_ID, REPARTIDOR_ID));

        verify(pedidoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("confirmarEntregaDomicilio: entrega, activa el prestamo y notifica al usuario")
    void entregaDomicilio_valida_quedaEntregado() {
        pedidoExiste(pedido("EN_TRANSITO"));
        when(configuracionService.plazo(ClavePlazo.DIAS_PRESTAMO_INTERCIUDAD)).thenReturn(14);
        cambioDeEstadoEjemplar("EN_TRANSITO", "PRESTADO", 1);
        PrestamoDTO prestamo = prestamo();
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo));
        actualizacionDevuelveElPedido();

        PedidoInterciudadDTO resultado = service.confirmarEntregaDomicilio(PEDIDO_ID, REPARTIDOR_ID);

        assertEquals("ENTREGADO", resultado.getEstado());
        assertEquals(REPARTIDOR_ID, resultado.getRepartidorId());
        assertEquals(resultado.getFechaEntrega().plusDays(14), resultado.getFechaLimiteDevolucion());
        assertEquals("ACTIVO", prestamo.getEstado());
        assertNotNull(prestamo.getFechaVencimiento());
        assertEquals("ENTREGA_CONFIRMADA", notificacionGuardada().getTipo());
    }

    // ---------------------------------------------------------------- registrarUsuarioNoRecibio

    @Test
    @DisplayName("registrarUsuarioNoRecibio: estado distinto de EN_TRANSITO lanza BusinessValidationException")
    void usuarioNoRecibio_estadoIncorrecto_lanzaBusinessValidation() {
        pedidoExiste(pedido("ENTREGADO"));

        assertThrows(BusinessValidationException.class,
                () -> service.registrarUsuarioNoRecibio(PEDIDO_ID, REPARTIDOR_ID));

        verifyNoInteractions(multaDAO);
    }

    @Test
    @DisplayName("registrarUsuarioNoRecibio: pasa a NO_RECIBIDO, genera el recargo pendiente y notifica")
    void usuarioNoRecibio_valido_generaRecargo() {
        pedidoExiste(pedido("EN_TRANSITO"));
        when(configuracionService.montoMulta(ConceptoMulta.RECARGO_PEDIDO_NO_RECIBIDO))
                .thenReturn(new BigDecimal("15000"));
        actualizacionDevuelveElPedido();

        PedidoInterciudadDTO resultado = service.registrarUsuarioNoRecibio(PEDIDO_ID, REPARTIDOR_ID);

        assertEquals("NO_RECIBIDO", resultado.getEstado());
        assertEquals(REPARTIDOR_ID, resultado.getRepartidorId());

        ArgumentCaptor<MultaDTO> captor = ArgumentCaptor.forClass(MultaDTO.class);
        verify(multaDAO).save(captor.capture());
        MultaDTO recargo = captor.getValue();
        assertEquals(new BigDecimal("15000"), recargo.getMonto());
        assertEquals("PENDIENTE", recargo.getEstado());
        assertEquals(USUARIO_ID, recargo.getUsuarioId());
        assertEquals(PRESTAMO_ID, recargo.getPrestamoId());
        assertNotNull(recargo.getFechaGeneracion());
        assertEquals("PEDIDO_NO_RECIBIDO", notificacionGuardada().getTipo());
    }

    // ---------------------------------------------------------------- iniciarTrasladoRetorno

    @Test
    @DisplayName("iniciarTrasladoRetorno: estado distinto de DEVUELTO_PENDIENTE_TRASLADO lanza BusinessValidationException")
    void traslado_estadoIncorrecto_lanzaBusinessValidation() {
        pedidoExiste(pedido("ENTREGADO"));

        assertThrows(BusinessValidationException.class,
                () -> service.iniciarTrasladoRetorno(PEDIDO_ID, REPARTIDOR_ID));

        verifyNoInteractions(notificacionService);
    }

    @Test
    @DisplayName("iniciarTrasladoRetorno: pasa a EN_CAMINO_SEDE_ORIGEN y avisa a la sede de origen")
    void traslado_valido_quedaEnCamino() {
        pedidoExiste(pedido("DEVUELTO_PENDIENTE_TRASLADO"));
        actualizacionDevuelveElPedido();

        PedidoInterciudadDTO resultado = service.iniciarTrasladoRetorno(PEDIDO_ID, REPARTIDOR_ID);

        assertEquals("EN_CAMINO_SEDE_ORIGEN", resultado.getEstado());
        assertEquals(REPARTIDOR_ID, resultado.getRepartidorId());
        verify(notificacionService).notificarSede(eq(SEDE_ORIGEN_ID), eq("TRASLADO_EN_CAMINO"), anyString(), anyString());
    }
}