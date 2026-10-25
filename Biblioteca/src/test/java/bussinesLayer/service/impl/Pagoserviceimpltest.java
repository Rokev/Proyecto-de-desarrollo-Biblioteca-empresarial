package bussinesLayer.service.impl;

import bussinesLayer.dto.ComprobantePagoDTO;
import bussinesLayer.dto.MetodoPagoDTO;
import bussinesLayer.dto.MultaDTO;
import bussinesLayer.dto.NotificacionDTO;
import bussinesLayer.dto.PagoDTO;
import bussinesLayer.dto.PedidoInterciudadDTO;
import bussinesLayer.dto.PlanSuscripcionDTO;
import bussinesLayer.dto.ReservaSalaDTO;
import bussinesLayer.dto.SolicitudPagoDTO;
import bussinesLayer.dto.SuscripcionDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.SuscripcionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.MetodoPagoDAO;
import persistanceLayer.dao.MultaDAO;
import persistanceLayer.dao.NotificacionDAO;
import persistanceLayer.dao.PagoDAO;
import persistanceLayer.dao.PedidoInterciudadDAO;
import persistanceLayer.dao.PlanSuscripcionDAO;
import persistanceLayer.dao.ReservaSalaDAO;
import persistanceLayer.dao.SuscripcionDAO;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagoServiceImpl")
class PagoServiceImplTest {

    private static final Long PAGO_ID = 100L;
    private static final Long USUARIO_ID = 1L;
    private static final Long METODO_ID = 2L;
    private static final Long MULTA_ID = 5L;
    private static final Long SUSCRIPCION_ID = 30L;
    private static final Long PLAN_ID = 4L;
    private static final Long RESERVA_ID = 8L;
    private static final Long PEDIDO_ID = 9L;

    private static final String TARJETA_APROBADA = "4111111111111111";
    private static final String TARJETA_RECHAZADA = "4000000000000002";
    private static final String TARJETA_DESCONOCIDA = "1234567812345678";

    @Mock private PagoDAO pagoDAO;
    @Mock private UsuarioDAO usuarioDAO;
    @Mock private MetodoPagoDAO metodoPagoDAO;
    @Mock private MultaDAO multaDAO;
    @Mock private SuscripcionDAO suscripcionDAO;
    @Mock private ReservaSalaDAO reservaSalaDAO;
    @Mock private NotificacionDAO notificacionDAO;
    @Mock private PedidoInterciudadDAO pedidoInterciudadDAO;
    @Mock private PlanSuscripcionDAO planSuscripcionDAO;
    @Mock private SuscripcionService suscripcionService;

    @InjectMocks
    private PagoServiceImpl service;

    // ---------------------------------------------------------------- helpers

    private PagoDTO pagoMulta() {
        PagoDTO p = new PagoDTO();
        p.setMonto(new BigDecimal("1000"));
        p.setFechaPago(LocalDateTime.of(2026, 3, 10, 10, 0));
        p.setConcepto("multa");
        p.setEstado("aprobado");
        p.setReferencia("REF-1");
        p.setUsuarioId(USUARIO_ID);
        p.setMetodoPagoId(METODO_ID);
        p.setMultaId(MULTA_ID);
        return p;
    }

    private PagoDTO pagoGuardado(Long id, String concepto, String estado, Long usuarioId, LocalDateTime fecha) {
        PagoDTO p = new PagoDTO();
        p.setId(id);
        p.setConcepto(concepto);
        p.setEstado(estado);
        p.setUsuarioId(usuarioId);
        p.setFechaPago(fecha);
        p.setMonto(new BigDecimal("1000"));
        p.setReferencia("REF-" + id);
        return p;
    }

    /** Stubs para que validarPago() acepte un pago de multa. */
    private void stubsValidarPagoMulta() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(metodoPagoDAO.findByIdAndUsuarioId(METODO_ID, USUARIO_ID)).thenReturn(Optional.of(new MetodoPagoDTO()));
        when(multaDAO.existsById(MULTA_ID)).thenReturn(true);
    }

    private void stubsUsuarioYMetodo() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(metodoPagoDAO.findByIdAndUsuarioId(METODO_ID, USUARIO_ID)).thenReturn(Optional.of(new MetodoPagoDTO()));
    }

    private MultaDTO multa(String estado, Long usuarioId, String monto) {
        MultaDTO m = new MultaDTO();
        m.setId(MULTA_ID);
        m.setUsuarioId(usuarioId);
        m.setEstado(estado);
        m.setMonto(new BigDecimal(monto));
        return m;
    }

    private ReservaSalaDTO reservaSala(String estado, Long usuarioId, String valorTotal) {
        ReservaSalaDTO r = new ReservaSalaDTO();
        r.setId(RESERVA_ID);
        r.setUsuarioId(usuarioId);
        r.setEstado(estado);
        r.setValorTotal(new BigDecimal(valorTotal));
        return r;
    }

    private PedidoInterciudadDTO pedido(String estado, Long usuarioId, double costo) {
        PedidoInterciudadDTO p = new PedidoInterciudadDTO();
        p.setId(PEDIDO_ID);
        p.setEstado(estado);
        p.setUsuarioId(usuarioId);
        p.setCostoEnvio(costo);
        p.setCiudadEntrega("Medellin");
        p.setSedeDevolucionNombre("Sede Centro");
        return p;
    }

    private SuscripcionDTO suscripcionPendiente(Long planId) {
        SuscripcionDTO s = new SuscripcionDTO();
        s.setId(SUSCRIPCION_ID);
        s.setEstado("PENDIENTE_PAGO");
        s.setPlanId(planId);
        s.setUsuarioId(USUARIO_ID);
        return s;
    }

    private PlanSuscripcionDTO plan(boolean activo, String precio) {
        PlanSuscripcionDTO p = new PlanSuscripcionDTO();
        p.setId(PLAN_ID);
        p.setActivo(activo);
        p.setPrecio(new BigDecimal(precio));
        return p;
    }

    private SolicitudPagoDTO solicitud(String concepto, String monto, String tarjeta) {
        SolicitudPagoDTO s = new SolicitudPagoDTO();
        s.setUsuarioId(USUARIO_ID);
        s.setConcepto(concepto);
        s.setMonto(new BigDecimal(monto));
        s.setNumeroTarjeta(tarjeta);
        return s;
    }

    private void usuarioExiste() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
    }

    private void guardarAsignandoId() {
        when(pagoDAO.save(any(PagoDTO.class))).thenAnswer(inv -> {
            PagoDTO p = inv.getArgument(0);
            p.setId(PAGO_ID);
            return p;
        });
    }

    // ---------------------------------------------------------------- crearPago

    @Test
    @DisplayName("crearPago: referencia repetida lanza BusinessValidationException")
    void crearPago_referenciaRepetida_lanzaBusinessValidation() {
        when(pagoDAO.existsByReferencia("REF-1")).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearPago(pagoMulta()));

        verify(pagoDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearPago: pago de multa valido normaliza concepto y estado y se guarda")
    void crearPago_multaValida_normalizaYGuarda() {
        stubsValidarPagoMulta();
        when(pagoDAO.save(any(PagoDTO.class))).thenAnswer(inv -> {
            PagoDTO p = inv.getArgument(0);
            p.setId(PAGO_ID);
            return p;
        });

        PagoDTO resultado = service.crearPago(pagoMulta());

        assertEquals(PAGO_ID, resultado.getId());
        assertEquals("MULTA", resultado.getConcepto());
        assertEquals("APROBADO", resultado.getEstado());
        verifyNoInteractions(suscripcionService);
    }

    @Test
    @DisplayName("crearPago: usuario inexistente lanza ResourceNotFoundException")
    void crearPago_usuarioInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearPago(pagoMulta()));

        verify(pagoDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearPago: metodo de pago que no es del usuario lanza BusinessValidationException")
    void crearPago_metodoPagoAjeno_lanzaBusinessValidation() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(metodoPagoDAO.findByIdAndUsuarioId(METODO_ID, USUARIO_ID)).thenReturn(Optional.empty());

        assertThrows(BusinessValidationException.class, () -> service.crearPago(pagoMulta()));

        verify(pagoDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearPago: multa sin multaId lanza BusinessValidationException")
    void crearPago_multaSinId_lanzaBusinessValidation() {
        stubsUsuarioYMetodo();
        PagoDTO dto = pagoMulta();
        dto.setMultaId(null);

        assertThrows(BusinessValidationException.class, () -> service.crearPago(dto));
    }

    @Test
    @DisplayName("crearPago: multa inexistente lanza ResourceNotFoundException")
    void crearPago_multaInexistente_lanzaNotFound() {
        stubsUsuarioYMetodo();
        when(multaDAO.existsById(MULTA_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearPago(pagoMulta()));
    }

    @Test
    @DisplayName("crearPago: suscripcion sin suscripcionId lanza BusinessValidationException")
    void crearPago_suscripcionSinId_lanzaBusinessValidation() {
        stubsUsuarioYMetodo();
        PagoDTO dto = pagoMulta();
        dto.setConcepto("SUSCRIPCION");
        dto.setMultaId(null);

        assertThrows(BusinessValidationException.class, () -> service.crearPago(dto));
    }

    @Test
    @DisplayName("crearPago: suscripcion inexistente lanza ResourceNotFoundException")
    void crearPago_suscripcionInexistente_lanzaNotFound() {
        stubsUsuarioYMetodo();
        when(suscripcionDAO.existsById(SUSCRIPCION_ID)).thenReturn(false);
        PagoDTO dto = pagoMulta();
        dto.setConcepto("SUSCRIPCION");
        dto.setSuscripcionId(SUSCRIPCION_ID);

        assertThrows(ResourceNotFoundException.class, () -> service.crearPago(dto));
    }

    @Test
    @DisplayName("crearPago: reserva de sala sin reservaSalaId lanza BusinessValidationException")
    void crearPago_reservaSalaSinId_lanzaBusinessValidation() {
        stubsUsuarioYMetodo();
        PagoDTO dto = pagoMulta();
        dto.setConcepto("RESERVA_SALA");
        dto.setMultaId(null);

        assertThrows(BusinessValidationException.class, () -> service.crearPago(dto));
    }

    @Test
    @DisplayName("crearPago: reserva de sala inexistente lanza ResourceNotFoundException")
    void crearPago_reservaSalaInexistente_lanzaNotFound() {
        stubsUsuarioYMetodo();
        when(reservaSalaDAO.findById(RESERVA_ID)).thenReturn(Optional.empty());
        PagoDTO dto = pagoMulta();
        dto.setConcepto("RESERVA_SALA");
        dto.setReservaSalaId(RESERVA_ID);

        assertThrows(ResourceNotFoundException.class, () -> service.crearPago(dto));
    }

    @Test
    @DisplayName("crearPago: monto distinto al valorTotal de la reserva lanza BusinessValidationException")
    void crearPago_reservaSalaMontoDistinto_lanzaBusinessValidation() {
        stubsUsuarioYMetodo();
        when(reservaSalaDAO.findById(RESERVA_ID))
                .thenReturn(Optional.of(reservaSala("PENDIENTE_PAGO", USUARIO_ID, "50000")));
        PagoDTO dto = pagoMulta();
        dto.setConcepto("RESERVA_SALA");
        dto.setReservaSalaId(RESERVA_ID);

        assertThrows(BusinessValidationException.class, () -> service.crearPago(dto));
    }

    @Test
    @DisplayName("crearPago: reserva de sala con monto exacto se guarda")
    void crearPago_reservaSalaValida_guarda() {
        stubsUsuarioYMetodo();
        when(reservaSalaDAO.findById(RESERVA_ID))
                .thenReturn(Optional.of(reservaSala("PENDIENTE_PAGO", USUARIO_ID, "1000")));
        when(pagoDAO.save(any(PagoDTO.class))).thenAnswer(inv -> inv.getArgument(0));
        PagoDTO dto = pagoMulta();
        dto.setConcepto("RESERVA_SALA");
        dto.setReservaSalaId(RESERVA_ID);

        assertEquals("RESERVA_SALA", service.crearPago(dto).getConcepto());
    }

    @Test
    @DisplayName("crearPago: envio sin pedidoInterciudadId lanza BusinessValidationException")
    void crearPago_envioSinPedido_lanzaBusinessValidation() {
        stubsUsuarioYMetodo();
        PagoDTO dto = pagoMulta();
        dto.setConcepto("ENVIO_INTERCIUDAD");

        assertThrows(BusinessValidationException.class, () -> service.crearPago(dto));
    }

    @Test
    @DisplayName("crearPago: envio con pedido inexistente lanza ResourceNotFoundException")
    void crearPago_envioPedidoInexistente_lanzaNotFound() {
        stubsUsuarioYMetodo();
        when(pedidoInterciudadDAO.findById(PEDIDO_ID)).thenReturn(Optional.empty());
        PagoDTO dto = pagoMulta();
        dto.setConcepto("ENVIO_INTERCIUDAD");
        dto.setPedidoInterciudadId(PEDIDO_ID);

        assertThrows(ResourceNotFoundException.class, () -> service.crearPago(dto));
    }

    @Test
    @DisplayName("crearPago: envio de un pedido de otro usuario lanza BusinessValidationException")
    void crearPago_envioPedidoAjeno_lanzaBusinessValidation() {
        stubsUsuarioYMetodo();
        when(pedidoInterciudadDAO.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido("PENDIENTE_PAGO", 99L, 1000.0)));
        PagoDTO dto = pagoMulta();
        dto.setConcepto("ENVIO_INTERCIUDAD");
        dto.setPedidoInterciudadId(PEDIDO_ID);

        assertThrows(BusinessValidationException.class, () -> service.crearPago(dto));
    }

    @Test
    @DisplayName("crearPago: envio con monto distinto al costo lanza BusinessValidationException")
    void crearPago_envioMontoDistinto_lanzaBusinessValidation() {
        stubsUsuarioYMetodo();
        when(pedidoInterciudadDAO.findById(PEDIDO_ID))
                .thenReturn(Optional.of(pedido("PENDIENTE_PAGO", USUARIO_ID, 8000.0)));
        PagoDTO dto = pagoMulta();
        dto.setConcepto("ENVIO_INTERCIUDAD");
        dto.setPedidoInterciudadId(PEDIDO_ID);

        assertThrows(BusinessValidationException.class, () -> service.crearPago(dto));
    }

    @Test
    @DisplayName("crearPago: concepto invalido lanza BusinessValidationException")
    void crearPago_conceptoInvalido_lanzaBusinessValidation() {
        stubsUsuarioYMetodo();
        PagoDTO dto = pagoMulta();
        dto.setConcepto("OTRO");

        assertThrows(BusinessValidationException.class, () -> service.crearPago(dto));
    }

    @Test
    @DisplayName("crearPago: estado invalido lanza BusinessValidationException")
    void crearPago_estadoInvalido_lanzaBusinessValidation() {
        stubsValidarPagoMulta();
        PagoDTO dto = pagoMulta();
        dto.setEstado("XX");

        assertThrows(BusinessValidationException.class, () -> service.crearPago(dto));

        verify(pagoDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearPago: estado nulo lanza BusinessValidationException")
    void crearPago_estadoNulo_lanzaBusinessValidation() {
        stubsValidarPagoMulta();
        PagoDTO dto = pagoMulta();
        dto.setEstado(null);

        assertThrows(BusinessValidationException.class, () -> service.crearPago(dto));
    }

    @Test
    @DisplayName("crearPago: suscripcion APROBADA y pendiente de pago activa la suscripcion")
    void crearPago_suscripcionAprobada_activaSuscripcion() {
        stubsUsuarioYMetodo();
        when(suscripcionDAO.existsById(SUSCRIPCION_ID)).thenReturn(true);
        when(suscripcionDAO.findById(SUSCRIPCION_ID)).thenReturn(Optional.of(suscripcionPendiente(PLAN_ID)));
        when(pagoDAO.save(any(PagoDTO.class))).thenAnswer(inv -> inv.getArgument(0));
        PagoDTO dto = pagoMulta();
        dto.setConcepto("SUSCRIPCION");
        dto.setMultaId(null);
        dto.setSuscripcionId(SUSCRIPCION_ID);

        service.crearPago(dto);

        verify(suscripcionService).activarSuscripcion(SUSCRIPCION_ID);
    }

    @Test
    @DisplayName("crearPago: suscripcion que ya no esta pendiente no se vuelve a activar")
    void crearPago_suscripcionYaActiva_noActiva() {
        stubsUsuarioYMetodo();
        when(suscripcionDAO.existsById(SUSCRIPCION_ID)).thenReturn(true);
        SuscripcionDTO activa = suscripcionPendiente(PLAN_ID);
        activa.setEstado("ACTIVA");
        when(suscripcionDAO.findById(SUSCRIPCION_ID)).thenReturn(Optional.of(activa));
        when(pagoDAO.save(any(PagoDTO.class))).thenAnswer(inv -> inv.getArgument(0));
        PagoDTO dto = pagoMulta();
        dto.setConcepto("SUSCRIPCION");
        dto.setMultaId(null);
        dto.setSuscripcionId(SUSCRIPCION_ID);

        service.crearPago(dto);

        verifyNoInteractions(suscripcionService);
    }

    @Test
    @DisplayName("crearPago: suscripcion con pago PENDIENTE no activa la suscripcion")
    void crearPago_suscripcionPagoPendiente_noActiva() {
        stubsUsuarioYMetodo();
        when(suscripcionDAO.existsById(SUSCRIPCION_ID)).thenReturn(true);
        when(pagoDAO.save(any(PagoDTO.class))).thenAnswer(inv -> inv.getArgument(0));
        PagoDTO dto = pagoMulta();
        dto.setConcepto("SUSCRIPCION");
        dto.setEstado("PENDIENTE");
        dto.setMultaId(null);
        dto.setSuscripcionId(SUSCRIPCION_ID);

        service.crearPago(dto);

        verifyNoInteractions(suscripcionService);
        verify(suscripcionDAO, never()).findById(any());
    }

    // ---------------------------------------------------------------- consultas y eliminar

    @Test
    @DisplayName("obtenerPagoPorId: devuelve el pago cuando existe")
    void obtenerPorId_existente_loDevuelve() {
        PagoDTO existente = pagoGuardado(PAGO_ID, "MULTA", "APROBADO", USUARIO_ID, null);
        when(pagoDAO.findById(PAGO_ID)).thenReturn(Optional.of(existente));

        assertSame(existente, service.obtenerPagoPorId(PAGO_ID));
    }

    @Test
    @DisplayName("obtenerPagoPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerPorId_inexistente_lanzaNotFound() {
        when(pagoDAO.findById(PAGO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerPagoPorId(PAGO_ID));
    }

    @Test
    @DisplayName("obtenerTodosLosPagos: delega en el DAO")
    void obtenerTodos_devuelveLista() {
        when(pagoDAO.findAll()).thenReturn(List.of(pagoGuardado(PAGO_ID, "MULTA", "APROBADO", USUARIO_ID, null)));

        assertEquals(1, service.obtenerTodosLosPagos().size());
    }

    @Test
    @DisplayName("eliminarPago: elimina cuando existe")
    void eliminar_existente_elimina() {
        when(pagoDAO.deleteById(PAGO_ID)).thenReturn(true);

        service.eliminarPago(PAGO_ID);

        verify(pagoDAO).deleteById(PAGO_ID);
    }

    @Test
    @DisplayName("eliminarPago: inexistente lanza ResourceNotFoundException")
    void eliminar_inexistente_lanzaNotFound() {
        when(pagoDAO.deleteById(PAGO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarPago(PAGO_ID));
    }

    // ---------------------------------------------------------------- actualizarPago

    @Test
    @DisplayName("actualizarPago: pago inexistente lanza ResourceNotFoundException")
    void actualizar_inexistente_lanzaNotFound() {
        when(pagoDAO.existsById(PAGO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarPago(PAGO_ID, pagoMulta()));

        verify(pagoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarPago: referencia usada por otro pago lanza BusinessValidationException")
    void actualizar_referenciaDeOtroPago_lanzaBusinessValidation() {
        when(pagoDAO.existsById(PAGO_ID)).thenReturn(true);
        when(pagoDAO.findByReferencia("REF-1"))
                .thenReturn(Optional.of(pagoGuardado(999L, "MULTA", "APROBADO", USUARIO_ID, null)));

        assertThrows(BusinessValidationException.class, () -> service.actualizarPago(PAGO_ID, pagoMulta()));

        verify(pagoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarPago: con la misma referencia del propio pago actualiza")
    void actualizar_mismaReferencia_actualiza() {
        when(pagoDAO.existsById(PAGO_ID)).thenReturn(true);
        when(pagoDAO.findByReferencia("REF-1"))
                .thenReturn(Optional.of(pagoGuardado(PAGO_ID, "MULTA", "APROBADO", USUARIO_ID, null)));
        stubsValidarPagoMulta();
        when(pagoDAO.update(eq(PAGO_ID), any(PagoDTO.class))).thenAnswer(inv -> Optional.of(inv.getArgument(1)));

        PagoDTO resultado = service.actualizarPago(PAGO_ID, pagoMulta());

        assertEquals("MULTA", resultado.getConcepto());
        assertEquals("APROBADO", resultado.getEstado());
    }

    @Test
    @DisplayName("actualizarPago: si el DAO no devuelve el pago lanza ResourceNotFoundException")
    void actualizar_daoNoActualiza_lanzaNotFound() {
        when(pagoDAO.existsById(PAGO_ID)).thenReturn(true);
        stubsValidarPagoMulta();
        when(pagoDAO.update(eq(PAGO_ID), any(PagoDTO.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarPago(PAGO_ID, pagoMulta()));
    }

    // ---------------------------------------------------------------- procesarPagoSimulado: validaciones comunes

    @Test
    @DisplayName("procesarPagoSimulado: usuario inexistente lanza ResourceNotFoundException")
    void procesar_usuarioInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> service.procesarPagoSimulado(solicitud("MULTA", "1000", TARJETA_APROBADA)));

        verify(pagoDAO, never()).save(any());
    }

    @Test
    @DisplayName("procesarPagoSimulado: concepto no soportado con tarjeta aprobada lanza BusinessValidationException")
    void procesar_conceptoNoSoportado_lanzaBusinessValidation() {
        usuarioExiste();
        guardarAsignandoId();

        assertThrows(BusinessValidationException.class,
                () -> service.procesarPagoSimulado(solicitud("OTRO", "1000", TARJETA_APROBADA)));
    }

    // ---------------------------------------------------------------- procesarPagoSimulado: multa / recargo

    @Test
    @DisplayName("procesarPagoSimulado: multa con tarjeta aprobada marca la multa PAGADA y notifica")
    void procesar_multaAprobada_marcaPagadaYNotifica() {
        usuarioExiste();
        MultaDTO multa = multa("PENDIENTE", USUARIO_ID, "1000");
        when(multaDAO.findById(MULTA_ID)).thenReturn(Optional.of(multa));
        guardarAsignandoId();
        SolicitudPagoDTO solicitud = solicitud("multa", "1000", TARJETA_APROBADA);
        solicitud.setMultaId(MULTA_ID);

        ComprobantePagoDTO comprobante = service.procesarPagoSimulado(solicitud);

        assertEquals("PAGADA", multa.getEstado());
        verify(multaDAO).update(MULTA_ID, multa);
        assertEquals(PAGO_ID, comprobante.getId());
        assertEquals("APROBADO", comprobante.getEstado());
        assertEquals("MULTA", comprobante.getConcepto());
        assertEquals("1111", comprobante.getUltimosCuatroDigitos());
        assertEquals("Pago procesado y aprobado exitosamente.", comprobante.getMensaje());
        assertTrue(comprobante.getNumeroReferencia().startsWith("REF-"));

        ArgumentCaptor<PagoDTO> pagoCaptor = ArgumentCaptor.forClass(PagoDTO.class);
        verify(pagoDAO).save(pagoCaptor.capture());
        assertEquals(MULTA_ID, pagoCaptor.getValue().getMultaId());
        assertEquals("APROBADO", pagoCaptor.getValue().getEstado());

        ArgumentCaptor<NotificacionDTO> notiCaptor = ArgumentCaptor.forClass(NotificacionDTO.class);
        verify(notificacionDAO).save(notiCaptor.capture());
        assertEquals("PAGO_APROBADO", notiCaptor.getValue().getTipo());
        assertEquals(USUARIO_ID, notiCaptor.getValue().getUsuarioId());
    }

    @Test
    @DisplayName("procesarPagoSimulado: recargo se procesa igual que una multa")
    void procesar_recargoAprobado_marcaPagada() {
        usuarioExiste();
        MultaDTO multa = multa("PENDIENTE", USUARIO_ID, "1000");
        when(multaDAO.findById(MULTA_ID)).thenReturn(Optional.of(multa));
        guardarAsignandoId();
        SolicitudPagoDTO solicitud = solicitud("RECARGO", "1000", "5555555555554444");
        solicitud.setMultaId(MULTA_ID);

        ComprobantePagoDTO comprobante = service.procesarPagoSimulado(solicitud);

        assertEquals("RECARGO", comprobante.getConcepto());
        assertEquals("4444", comprobante.getUltimosCuatroDigitos());
        assertEquals("PAGADA", multa.getEstado());
    }

    @Test
    @DisplayName("procesarPagoSimulado: tarjeta rechazada conocida guarda RECHAZADO, notifica y no toca la multa")
    void procesar_tarjetaRechazada_noPagaLaMulta() {
        usuarioExiste();
        MultaDTO multa = multa("PENDIENTE", USUARIO_ID, "1000");
        when(multaDAO.findById(MULTA_ID)).thenReturn(Optional.of(multa));
        guardarAsignandoId();
        SolicitudPagoDTO solicitud = solicitud("MULTA", "1000", TARJETA_RECHAZADA);
        solicitud.setMultaId(MULTA_ID);

        ComprobantePagoDTO comprobante = service.procesarPagoSimulado(solicitud);

        assertEquals("RECHAZADO", comprobante.getEstado());
        assertTrue(comprobante.getMensaje().contains("rechazada por la entidad bancaria"));
        assertEquals("PENDIENTE", multa.getEstado());
        verify(multaDAO, never()).update(any(), any());

        ArgumentCaptor<NotificacionDTO> captor = ArgumentCaptor.forClass(NotificacionDTO.class);
        verify(notificacionDAO).save(captor.capture());
        assertEquals("PAGO_RECHAZADO", captor.getValue().getTipo());
    }

    @Test
    @DisplayName("procesarPagoSimulado: tarjeta desconocida se rechaza con mensaje de tarjeta no reconocida")
    void procesar_tarjetaDesconocida_mensajeNoReconocida() {
        usuarioExiste();
        when(multaDAO.findById(MULTA_ID)).thenReturn(Optional.of(multa("PENDIENTE", USUARIO_ID, "1000")));
        guardarAsignandoId();
        SolicitudPagoDTO solicitud = solicitud("MULTA", "1000", TARJETA_DESCONOCIDA);
        solicitud.setMultaId(MULTA_ID);

        ComprobantePagoDTO comprobante = service.procesarPagoSimulado(solicitud);

        assertEquals("RECHAZADO", comprobante.getEstado());
        assertTrue(comprobante.getMensaje().contains("Tarjeta no reconocida"));
    }

    @Test
    @DisplayName("procesarPagoSimulado: multa sin multaId lanza BusinessValidationException")
    void procesar_multaSinId_lanzaBusinessValidation() {
        usuarioExiste();

        assertThrows(BusinessValidationException.class,
                () -> service.procesarPagoSimulado(solicitud("MULTA", "1000", TARJETA_APROBADA)));

        verify(pagoDAO, never()).save(any());
    }

    @Test
    @DisplayName("procesarPagoSimulado: multa inexistente lanza ResourceNotFoundException")
    void procesar_multaInexistente_lanzaNotFound() {
        usuarioExiste();
        when(multaDAO.findById(MULTA_ID)).thenReturn(Optional.empty());
        SolicitudPagoDTO solicitud = solicitud("MULTA", "1000", TARJETA_APROBADA);
        solicitud.setMultaId(MULTA_ID);

        assertThrows(ResourceNotFoundException.class, () -> service.procesarPagoSimulado(solicitud));
    }

    @Test
    @DisplayName("procesarPagoSimulado: multa de otro usuario lanza BusinessValidationException")
    void procesar_multaAjena_lanzaBusinessValidation() {
        usuarioExiste();
        when(multaDAO.findById(MULTA_ID)).thenReturn(Optional.of(multa("PENDIENTE", 99L, "1000")));
        SolicitudPagoDTO solicitud = solicitud("MULTA", "1000", TARJETA_APROBADA);
        solicitud.setMultaId(MULTA_ID);

        assertThrows(BusinessValidationException.class, () -> service.procesarPagoSimulado(solicitud));
    }

    @Test
    @DisplayName("procesarPagoSimulado: multa que no esta PENDIENTE lanza BusinessValidationException")
    void procesar_multaYaPagada_lanzaBusinessValidation() {
        usuarioExiste();
        when(multaDAO.findById(MULTA_ID)).thenReturn(Optional.of(multa("PAGADA", USUARIO_ID, "1000")));
        SolicitudPagoDTO solicitud = solicitud("MULTA", "1000", TARJETA_APROBADA);
        solicitud.setMultaId(MULTA_ID);

        assertThrows(BusinessValidationException.class, () -> service.procesarPagoSimulado(solicitud));
    }

    @Test
    @DisplayName("procesarPagoSimulado: monto distinto al de la multa lanza BusinessValidationException")
    void procesar_multaMontoDistinto_lanzaBusinessValidation() {
        usuarioExiste();
        when(multaDAO.findById(MULTA_ID)).thenReturn(Optional.of(multa("PENDIENTE", USUARIO_ID, "1000")));
        SolicitudPagoDTO solicitud = solicitud("MULTA", "500", TARJETA_APROBADA);
        solicitud.setMultaId(MULTA_ID);

        assertThrows(BusinessValidationException.class, () -> service.procesarPagoSimulado(solicitud));

        verify(pagoDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- procesarPagoSimulado: suscripcion

    @Test
    @DisplayName("procesarPagoSimulado: suscripcion aprobada activa la suscripcion pendiente")
    void procesar_suscripcionAprobada_activaSuscripcion() {
        usuarioExiste();
        when(suscripcionDAO.findByUsuarioIdAndEstado(USUARIO_ID, "PENDIENTE_PAGO"))
                .thenReturn(Optional.of(suscripcionPendiente(PLAN_ID)));
        when(planSuscripcionDAO.findById(PLAN_ID)).thenReturn(Optional.of(plan(true, "30000")));
        guardarAsignandoId();
        SolicitudPagoDTO solicitud = solicitud("SUSCRIPCION", "30000", TARJETA_APROBADA);
        solicitud.setPlanSuscripcionId(PLAN_ID);

        ComprobantePagoDTO comprobante = service.procesarPagoSimulado(solicitud);

        assertEquals("APROBADO", comprobante.getEstado());
        verify(suscripcionService).activarSuscripcion(SUSCRIPCION_ID);

        ArgumentCaptor<PagoDTO> captor = ArgumentCaptor.forClass(PagoDTO.class);
        verify(pagoDAO).save(captor.capture());
        assertEquals(SUSCRIPCION_ID, captor.getValue().getSuscripcionId());
    }

    @Test
    @DisplayName("procesarPagoSimulado: suscripcion rechazada no activa la suscripcion")
    void procesar_suscripcionRechazada_noActiva() {
        usuarioExiste();
        when(suscripcionDAO.findByUsuarioIdAndEstado(USUARIO_ID, "PENDIENTE_PAGO"))
                .thenReturn(Optional.of(suscripcionPendiente(PLAN_ID)));
        when(planSuscripcionDAO.findById(PLAN_ID)).thenReturn(Optional.of(plan(true, "30000")));
        guardarAsignandoId();
        SolicitudPagoDTO solicitud = solicitud("SUSCRIPCION", "30000", TARJETA_RECHAZADA);
        solicitud.setPlanSuscripcionId(PLAN_ID);

        ComprobantePagoDTO comprobante = service.procesarPagoSimulado(solicitud);

        assertEquals("RECHAZADO", comprobante.getEstado());
        verifyNoInteractions(suscripcionService);
    }

    @Test
    @DisplayName("procesarPagoSimulado: suscripcion sin planSuscripcionId lanza BusinessValidationException")
    void procesar_suscripcionSinPlan_lanzaBusinessValidation() {
        usuarioExiste();

        assertThrows(BusinessValidationException.class,
                () -> service.procesarPagoSimulado(solicitud("SUSCRIPCION", "30000", TARJETA_APROBADA)));
    }

    @Test
    @DisplayName("procesarPagoSimulado: sin compra pendiente de pago lanza BusinessValidationException")
    void procesar_suscripcionSinPendiente_lanzaBusinessValidation() {
        usuarioExiste();
        when(suscripcionDAO.findByUsuarioIdAndEstado(USUARIO_ID, "PENDIENTE_PAGO")).thenReturn(Optional.empty());
        SolicitudPagoDTO solicitud = solicitud("SUSCRIPCION", "30000", TARJETA_APROBADA);
        solicitud.setPlanSuscripcionId(PLAN_ID);

        assertThrows(BusinessValidationException.class, () -> service.procesarPagoSimulado(solicitud));
    }

    @Test
    @DisplayName("procesarPagoSimulado: plan distinto al de la compra pendiente lanza BusinessValidationException")
    void procesar_suscripcionPlanDistinto_lanzaBusinessValidation() {
        usuarioExiste();
        when(suscripcionDAO.findByUsuarioIdAndEstado(USUARIO_ID, "PENDIENTE_PAGO"))
                .thenReturn(Optional.of(suscripcionPendiente(77L)));
        SolicitudPagoDTO solicitud = solicitud("SUSCRIPCION", "30000", TARJETA_APROBADA);
        solicitud.setPlanSuscripcionId(PLAN_ID);

        assertThrows(BusinessValidationException.class, () -> service.procesarPagoSimulado(solicitud));
    }

    @Test
    @DisplayName("procesarPagoSimulado: plan inexistente lanza ResourceNotFoundException")
    void procesar_suscripcionPlanInexistente_lanzaNotFound() {
        usuarioExiste();
        when(suscripcionDAO.findByUsuarioIdAndEstado(USUARIO_ID, "PENDIENTE_PAGO"))
                .thenReturn(Optional.of(suscripcionPendiente(PLAN_ID)));
        when(planSuscripcionDAO.findById(PLAN_ID)).thenReturn(Optional.empty());
        SolicitudPagoDTO solicitud = solicitud("SUSCRIPCION", "30000", TARJETA_APROBADA);
        solicitud.setPlanSuscripcionId(PLAN_ID);

        assertThrows(ResourceNotFoundException.class, () -> service.procesarPagoSimulado(solicitud));
    }

    @Test
    @DisplayName("procesarPagoSimulado: plan inactivo lanza BusinessValidationException")
    void procesar_suscripcionPlanInactivo_lanzaBusinessValidation() {
        usuarioExiste();
        when(suscripcionDAO.findByUsuarioIdAndEstado(USUARIO_ID, "PENDIENTE_PAGO"))
                .thenReturn(Optional.of(suscripcionPendiente(PLAN_ID)));
        when(planSuscripcionDAO.findById(PLAN_ID)).thenReturn(Optional.of(plan(false, "30000")));
        SolicitudPagoDTO solicitud = solicitud("SUSCRIPCION", "30000", TARJETA_APROBADA);
        solicitud.setPlanSuscripcionId(PLAN_ID);

        assertThrows(BusinessValidationException.class, () -> service.procesarPagoSimulado(solicitud));
    }

    @Test
    @DisplayName("procesarPagoSimulado: monto distinto al precio del plan lanza BusinessValidationException")
    void procesar_suscripcionMontoDistinto_lanzaBusinessValidation() {
        usuarioExiste();
        when(suscripcionDAO.findByUsuarioIdAndEstado(USUARIO_ID, "PENDIENTE_PAGO"))
                .thenReturn(Optional.of(suscripcionPendiente(PLAN_ID)));
        when(planSuscripcionDAO.findById(PLAN_ID)).thenReturn(Optional.of(plan(true, "30000")));
        SolicitudPagoDTO solicitud = solicitud("SUSCRIPCION", "100", TARJETA_APROBADA);
        solicitud.setPlanSuscripcionId(PLAN_ID);

        assertThrows(BusinessValidationException.class, () -> service.procesarPagoSimulado(solicitud));
    }

    // ---------------------------------------------------------------- procesarPagoSimulado: reserva de sala

    @Test
    @DisplayName("procesarPagoSimulado: reserva de sala aprobada queda CONFIRMADA")
    void procesar_reservaSalaAprobada_quedaConfirmada() {
        usuarioExiste();
        ReservaSalaDTO reserva = reservaSala("PENDIENTE_PAGO", USUARIO_ID, "50000");
        when(reservaSalaDAO.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
        guardarAsignandoId();
        SolicitudPagoDTO solicitud = solicitud("RESERVA_SALA", "50000", TARJETA_APROBADA);
        solicitud.setReservaSalaId(RESERVA_ID);

        ComprobantePagoDTO comprobante = service.procesarPagoSimulado(solicitud);

        assertEquals("APROBADO", comprobante.getEstado());
        assertEquals("CONFIRMADA", reserva.getEstado());
        verify(reservaSalaDAO).update(RESERVA_ID, reserva);

        ArgumentCaptor<PagoDTO> captor = ArgumentCaptor.forClass(PagoDTO.class);
        verify(pagoDAO).save(captor.capture());
        assertEquals(RESERVA_ID, captor.getValue().getReservaSalaId());
    }

    @Test
    @DisplayName("procesarPagoSimulado: reserva de sala rechazada no cambia la reserva")
    void procesar_reservaSalaRechazada_noConfirma() {
        usuarioExiste();
        ReservaSalaDTO reserva = reservaSala("PENDIENTE_PAGO", USUARIO_ID, "50000");
        when(reservaSalaDAO.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
        guardarAsignandoId();
        SolicitudPagoDTO solicitud = solicitud("RESERVA_SALA", "50000", TARJETA_RECHAZADA);
        solicitud.setReservaSalaId(RESERVA_ID);

        service.procesarPagoSimulado(solicitud);

        assertEquals("PENDIENTE_PAGO", reserva.getEstado());
        verify(reservaSalaDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("procesarPagoSimulado: reserva de sala sin reservaSalaId lanza BusinessValidationException")
    void procesar_reservaSalaSinId_lanzaBusinessValidation() {
        usuarioExiste();

        assertThrows(BusinessValidationException.class,
                () -> service.procesarPagoSimulado(solicitud("RESERVA_SALA", "50000", TARJETA_APROBADA)));
    }

    @Test
    @DisplayName("procesarPagoSimulado: reserva de sala inexistente lanza ResourceNotFoundException")
    void procesar_reservaSalaInexistente_lanzaNotFound() {
        usuarioExiste();
        when(reservaSalaDAO.findById(RESERVA_ID)).thenReturn(Optional.empty());
        SolicitudPagoDTO solicitud = solicitud("RESERVA_SALA", "50000", TARJETA_APROBADA);
        solicitud.setReservaSalaId(RESERVA_ID);

        assertThrows(ResourceNotFoundException.class, () -> service.procesarPagoSimulado(solicitud));
    }

    @Test
    @DisplayName("procesarPagoSimulado: reserva de sala de otro usuario lanza BusinessValidationException")
    void procesar_reservaSalaAjena_lanzaBusinessValidation() {
        usuarioExiste();
        when(reservaSalaDAO.findById(RESERVA_ID))
                .thenReturn(Optional.of(reservaSala("PENDIENTE_PAGO", 99L, "50000")));
        SolicitudPagoDTO solicitud = solicitud("RESERVA_SALA", "50000", TARJETA_APROBADA);
        solicitud.setReservaSalaId(RESERVA_ID);

        assertThrows(BusinessValidationException.class, () -> service.procesarPagoSimulado(solicitud));
    }

    @Test
    @DisplayName("procesarPagoSimulado: reserva de sala que no esta PENDIENTE_PAGO lanza BusinessValidationException")
    void procesar_reservaSalaYaConfirmada_lanzaBusinessValidation() {
        usuarioExiste();
        when(reservaSalaDAO.findById(RESERVA_ID))
                .thenReturn(Optional.of(reservaSala("CONFIRMADA", USUARIO_ID, "50000")));
        SolicitudPagoDTO solicitud = solicitud("RESERVA_SALA", "50000", TARJETA_APROBADA);
        solicitud.setReservaSalaId(RESERVA_ID);

        assertThrows(BusinessValidationException.class, () -> service.procesarPagoSimulado(solicitud));
    }

    @Test
    @DisplayName("procesarPagoSimulado: monto distinto al valorTotal de la reserva lanza BusinessValidationException")
    void procesar_reservaSalaMontoDistinto_lanzaBusinessValidation() {
        usuarioExiste();
        when(reservaSalaDAO.findById(RESERVA_ID))
                .thenReturn(Optional.of(reservaSala("PENDIENTE_PAGO", USUARIO_ID, "50000")));
        SolicitudPagoDTO solicitud = solicitud("RESERVA_SALA", "100", TARJETA_APROBADA);
        solicitud.setReservaSalaId(RESERVA_ID);

        assertThrows(BusinessValidationException.class, () -> service.procesarPagoSimulado(solicitud));
    }

    // ---------------------------------------------------------------- procesarPagoSimulado: envio interciudad

    @Test
    @DisplayName("procesarPagoSimulado: envio aprobado confirma el pedido (SOLICITADO) y envia dos notificaciones")
    void procesar_envioAprobado_confirmaPedido() {
        usuarioExiste();
        PedidoInterciudadDTO pedido = pedido("PENDIENTE_PAGO", USUARIO_ID, 8000.0);
        when(pedidoInterciudadDAO.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        guardarAsignandoId();
        SolicitudPagoDTO solicitud = solicitud("ENVIO_INTERCIUDAD", "8000", TARJETA_APROBADA);
        solicitud.setPedidoInterciudadId(PEDIDO_ID);

        ComprobantePagoDTO comprobante = service.procesarPagoSimulado(solicitud);

        assertEquals("APROBADO", comprobante.getEstado());
        assertEquals("SOLICITADO", pedido.getEstado());
        verify(pedidoInterciudadDAO).update(PEDIDO_ID, pedido);

        ArgumentCaptor<NotificacionDTO> captor = ArgumentCaptor.forClass(NotificacionDTO.class);
        verify(notificacionDAO, times(2)).save(captor.capture());
        assertEquals("PEDIDO_CONFIRMADO", captor.getAllValues().get(0).getTipo());
        assertEquals("PAGO_APROBADO", captor.getAllValues().get(1).getTipo());

        ArgumentCaptor<PagoDTO> pagoCaptor = ArgumentCaptor.forClass(PagoDTO.class);
        verify(pagoDAO).save(pagoCaptor.capture());
        assertEquals(PEDIDO_ID, pagoCaptor.getValue().getPedidoInterciudadId());
    }

    @Test
    @DisplayName("procesarPagoSimulado: envio rechazado deja el pedido sin cambios")
    void procesar_envioRechazado_noCambiaPedido() {
        usuarioExiste();
        PedidoInterciudadDTO pedido = pedido("PENDIENTE_PAGO", USUARIO_ID, 8000.0);
        when(pedidoInterciudadDAO.findById(PEDIDO_ID)).thenReturn(Optional.of(pedido));
        guardarAsignandoId();
        SolicitudPagoDTO solicitud = solicitud("ENVIO_INTERCIUDAD", "8000", TARJETA_RECHAZADA);
        solicitud.setPedidoInterciudadId(PEDIDO_ID);

        ComprobantePagoDTO comprobante = service.procesarPagoSimulado(solicitud);

        assertEquals("RECHAZADO", comprobante.getEstado());
        assertEquals("PENDIENTE_PAGO", pedido.getEstado());
        verify(pedidoInterciudadDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("procesarPagoSimulado: envio sin pedidoInterciudadId lanza BusinessValidationException")
    void procesar_envioSinPedido_lanzaBusinessValidation() {
        usuarioExiste();

        assertThrows(BusinessValidationException.class,
                () -> service.procesarPagoSimulado(solicitud("ENVIO_INTERCIUDAD", "8000", TARJETA_APROBADA)));
    }

    @Test
    @DisplayName("procesarPagoSimulado: envio con pedido inexistente lanza ResourceNotFoundException")
    void procesar_envioPedidoInexistente_lanzaNotFound() {
        usuarioExiste();
        when(pedidoInterciudadDAO.findById(PEDIDO_ID)).thenReturn(Optional.empty());
        SolicitudPagoDTO solicitud = solicitud("ENVIO_INTERCIUDAD", "8000", TARJETA_APROBADA);
        solicitud.setPedidoInterciudadId(PEDIDO_ID);

        assertThrows(ResourceNotFoundException.class, () -> service.procesarPagoSimulado(solicitud));
    }

    @Test
    @DisplayName("procesarPagoSimulado: envio de un pedido que no esta PENDIENTE_PAGO lanza BusinessValidationException")
    void procesar_envioPedidoNoPendiente_lanzaBusinessValidation() {
        usuarioExiste();
        when(pedidoInterciudadDAO.findById(PEDIDO_ID))
                .thenReturn(Optional.of(pedido("SOLICITADO", USUARIO_ID, 8000.0)));
        SolicitudPagoDTO solicitud = solicitud("ENVIO_INTERCIUDAD", "8000", TARJETA_APROBADA);
        solicitud.setPedidoInterciudadId(PEDIDO_ID);

        assertThrows(BusinessValidationException.class, () -> service.procesarPagoSimulado(solicitud));
    }

    @Test
    @DisplayName("procesarPagoSimulado: envio que ya fue pagado lanza BusinessValidationException")
    void procesar_envioYaPagado_lanzaBusinessValidation() {
        usuarioExiste();
        when(pedidoInterciudadDAO.findById(PEDIDO_ID))
                .thenReturn(Optional.of(pedido("PENDIENTE_PAGO", USUARIO_ID, 8000.0)));
        when(pagoDAO.findEnvioPagadoPorPedido(PEDIDO_ID)).thenReturn(Optional.of(new PagoDTO()));
        SolicitudPagoDTO solicitud = solicitud("ENVIO_INTERCIUDAD", "8000", TARJETA_APROBADA);
        solicitud.setPedidoInterciudadId(PEDIDO_ID);

        assertThrows(BusinessValidationException.class, () -> service.procesarPagoSimulado(solicitud));

        verify(pagoDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- comprobantes

    @Test
    @DisplayName("obtenerComprobantePorId: arma el comprobante con digitos ocultos")
    void obtenerComprobante_existente_armaComprobante() {
        PagoDTO pago = pagoGuardado(PAGO_ID, "MULTA", "APROBADO", USUARIO_ID, LocalDateTime.of(2026, 3, 10, 10, 0));
        when(pagoDAO.findById(PAGO_ID)).thenReturn(Optional.of(pago));

        ComprobantePagoDTO comprobante = service.obtenerComprobantePorId(PAGO_ID);

        assertEquals(PAGO_ID, comprobante.getId());
        assertEquals("REF-" + PAGO_ID, comprobante.getNumeroReferencia());
        assertEquals("****", comprobante.getUltimosCuatroDigitos());
        assertEquals("Consulta de comprobante", comprobante.getMensaje());
    }

    @Test
    @DisplayName("obtenerComprobantePorId: pago inexistente lanza ResourceNotFoundException")
    void obtenerComprobante_inexistente_lanzaNotFound() {
        when(pagoDAO.findById(PAGO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerComprobantePorId(PAGO_ID));
    }

    @Test
    @DisplayName("obtenerPagosPorUsuario: devuelve los comprobantes del historial")
    void obtenerPagosPorUsuario_existente_devuelveComprobantes() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(pagoDAO.findByUsuarioIdOrderByFechaPagoDesc(USUARIO_ID)).thenReturn(List.of(
                pagoGuardado(1L, "MULTA", "APROBADO", USUARIO_ID, null),
                pagoGuardado(2L, "SUSCRIPCION", "RECHAZADO", USUARIO_ID, null)));

        List<ComprobantePagoDTO> resultado = service.obtenerPagosPorUsuario(USUARIO_ID);

        assertEquals(2, resultado.size());
        assertEquals("Historial de transacciones de usuario", resultado.get(0).getMensaje());
    }

    @Test
    @DisplayName("obtenerPagosPorUsuario: usuario inexistente lanza ResourceNotFoundException")
    void obtenerPagosPorUsuario_usuarioInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerPagosPorUsuario(USUARIO_ID));

        verify(pagoDAO, never()).findByUsuarioIdOrderByFechaPagoDesc(any());
    }

    // ---------------------------------------------------------------- filtrarPagos

    private List<PagoDTO> pagosParaFiltrar() {
        return List.of(
                pagoGuardado(1L, "MULTA", "APROBADO", 1L, LocalDateTime.of(2026, 1, 10, 9, 0)),
                pagoGuardado(2L, "ENVIO_INTERCIUDAD", "PENDIENTE", 2L, LocalDateTime.of(2026, 2, 10, 9, 0)),
                pagoGuardado(3L, "MULTA", "RECHAZADO", 1L, LocalDateTime.of(2026, 3, 10, 9, 0)),
                pagoGuardado(4L, "SUSCRIPCION", "APROBADO", 3L, null));
    }

    @Test
    @DisplayName("filtrarPagos: fecha final anterior a la inicial lanza BusinessValidationException")
    void filtrar_rangoInvalido_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class, () -> service.filtrarPagos(
                null, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 2, 1), null));

        verify(pagoDAO, never()).findAll();
    }

    @Test
    @DisplayName("filtrarPagos: sin filtros devuelve todo, con los de fecha nula al final y el mas reciente primero")
    void filtrar_sinFiltros_ordenaPorFechaDesc() {
        when(pagoDAO.findAll()).thenReturn(pagosParaFiltrar());

        List<PagoDTO> resultado = service.filtrarPagos(null, null, null, null, null);

        assertEquals(4, resultado.size());
        assertEquals(3L, resultado.get(0).getId());
        assertEquals(2L, resultado.get(1).getId());
        assertEquals(1L, resultado.get(2).getId());
        assertEquals(4L, resultado.get(3).getId());
    }

    @Test
    @DisplayName("filtrarPagos: el concepto ENVIO se normaliza a ENVIO_INTERCIUDAD")
    void filtrar_conceptoEnvio_seNormaliza() {
        when(pagoDAO.findAll()).thenReturn(pagosParaFiltrar());

        List<PagoDTO> resultado = service.filtrarPagos("envio", null, null, null, null);

        assertEquals(1, resultado.size());
        assertEquals(2L, resultado.get(0).getId());
    }

    @Test
    @DisplayName("filtrarPagos: filtra por concepto, estado y usuario")
    void filtrar_conceptoEstadoUsuario_filtra() {
        when(pagoDAO.findAll()).thenReturn(pagosParaFiltrar());

        List<PagoDTO> resultado = service.filtrarPagos("multa", "aprobado", null, null, 1L);

        assertEquals(1, resultado.size());
        assertEquals(1L, resultado.get(0).getId());
    }

    @Test
    @DisplayName("filtrarPagos: los filtros en blanco se ignoran")
    void filtrar_filtrosEnBlanco_seIgnoran() {
        when(pagoDAO.findAll()).thenReturn(pagosParaFiltrar());

        assertEquals(4, service.filtrarPagos(" ", " ", null, null, null).size());
    }

    @Test
    @DisplayName("filtrarPagos: filtra por rango de fechas y excluye los pagos sin fecha")
    void filtrar_porFechas_filtraRango() {
        when(pagoDAO.findAll()).thenReturn(pagosParaFiltrar());

        List<PagoDTO> resultado = service.filtrarPagos(
                null, null, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31), null);

        assertEquals(2, resultado.size());
        assertFalse(resultado.stream().anyMatch(p -> p.getFechaPago() == null));
        assertNotNull(resultado.get(0).getFechaPago());
    }
}