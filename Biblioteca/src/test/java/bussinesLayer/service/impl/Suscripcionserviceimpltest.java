package bussinesLayer.service.impl;

import bussinesLayer.dto.CompraSuscripcionDTO;
import bussinesLayer.dto.PlanSuscripcionDTO;
import bussinesLayer.dto.SuscripcionDTO;
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
import persistanceLayer.dao.PlanSuscripcionDAO;
import persistanceLayer.dao.SuscripcionDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SuscripcionServiceImpl")
class SuscripcionServiceImplTest {

    private static final Long USUARIO_ID = 1L;
    private static final Long PLAN_ID = 4L;
    private static final Long OTRO_PLAN_ID = 8L;
    private static final Long SUSCRIPCION_ID = 10L;

    @Mock private SuscripcionDAO suscripcionDAO;
    @Mock private UsuarioDAO usuarioDAO;
    @Mock private PlanSuscripcionDAO planSuscripcionDAO;

    @InjectMocks
    private SuscripcionServiceImpl service;

    private PlanSuscripcionDTO plan;
    private SuscripcionDTO dto;

    @BeforeEach
    void setUp() {
        plan = plan(PLAN_ID, new BigDecimal("10"), true);

        dto = new SuscripcionDTO();
        dto.setUsuarioId(USUARIO_ID);
        dto.setPlanId(PLAN_ID);
        dto.setFechaInicio(LocalDate.now());
        dto.setFechaFin(LocalDate.now().plusMonths(6));
    }

    private PlanSuscripcionDTO plan(Long id, BigDecimal descuento, boolean activo) {
        PlanSuscripcionDTO p = new PlanSuscripcionDTO();
        p.setId(id);
        p.setNombre("Plan " + id);
        p.setPrecio(new BigDecimal("120000"));
        p.setDuracionMeses(6);
        p.setDescuentoSalas(descuento);
        p.setActivo(activo);
        return p;
    }

    private SuscripcionDTO suscripcion(String estado, LocalDate fechaFin) {
        SuscripcionDTO s = new SuscripcionDTO();
        s.setId(SUSCRIPCION_ID);
        s.setUsuarioId(USUARIO_ID);
        s.setPlanId(PLAN_ID);
        s.setEstado(estado);
        s.setFechaInicio(LocalDate.now().minusMonths(1));
        s.setFechaFin(fechaFin);
        s.setDescuentoSalas(new BigDecimal("5"));
        s.setRenovacionAutomatica(true);
        return s;
    }

    private void stubVigente(Optional<SuscripcionDTO> resultado) {
        when(suscripcionDAO.findFirstByUsuarioIdAndEstadoInAndFechaFinGreaterThanEqualOrderByFechaFinDesc(
                eq(USUARIO_ID), anyCollection(), any(LocalDate.class))).thenReturn(resultado);
    }

    // ---------------------------------------------------------------- crearSuscripcion

    @Test
    @DisplayName("crearSuscripcion: aplica valores por defecto y copia el descuento del plan")
    void crear_valida_aplicaDefaultsYDescuentoDelPlan() {
        dto.setDescuentoSalas(new BigDecimal("99")); // el cliente no decide el descuento
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(planSuscripcionDAO.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(suscripcionDAO.save(any(SuscripcionDTO.class))).thenAnswer(inv -> inv.getArgument(0));

        SuscripcionDTO resultado = service.crearSuscripcion(dto);

        assertEquals("ACTIVA", resultado.getEstado());
        assertFalse(resultado.getRenovacionAutomatica());
        assertEquals(0, resultado.getMesesConsecutivos());
        assertEquals(new BigDecimal("10"), resultado.getDescuentoSalas());
    }

    @Test
    @DisplayName("crearSuscripcion: usuario inexistente lanza ResourceNotFoundException")
    void crear_usuarioInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearSuscripcion(dto));

        verify(suscripcionDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearSuscripcion: plan inexistente lanza ResourceNotFoundException")
    void crear_planInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(planSuscripcionDAO.findById(PLAN_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.crearSuscripcion(dto));
    }

    @Test
    @DisplayName("crearSuscripcion: plan inactivo lanza BusinessValidationException")
    void crear_planInactivo_lanzaBusinessValidation() {
        plan.setActivo(false);
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(planSuscripcionDAO.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        assertThrows(BusinessValidationException.class, () -> service.crearSuscripcion(dto));

        verify(suscripcionDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearSuscripcion: fechaFin que no es posterior a fechaInicio lanza BusinessValidationException")
    void crear_fechaFinNoPosterior_lanzaBusinessValidation() {
        dto.setFechaFin(dto.getFechaInicio());
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(planSuscripcionDAO.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        assertThrows(BusinessValidationException.class, () -> service.crearSuscripcion(dto));

        verify(suscripcionDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearSuscripcion: usuario con suscripcion activa o pendiente lanza BusinessValidationException")
    void crear_yaTieneSuscripcion_lanzaBusinessValidation() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(planSuscripcionDAO.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(suscripcionDAO.existsByUsuarioIdAndEstadoIn(eq(USUARIO_ID), anyCollection())).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearSuscripcion(dto));

        verify(suscripcionDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- consultas y eliminar

    @Test
    @DisplayName("obtenerSuscripcionPorId: devuelve la suscripcion cuando existe")
    void obtenerPorId_existente_laDevuelve() {
        SuscripcionDTO s = suscripcion("ACTIVA", LocalDate.now().plusMonths(1));
        when(suscripcionDAO.findById(SUSCRIPCION_ID)).thenReturn(Optional.of(s));

        assertSame(s, service.obtenerSuscripcionPorId(SUSCRIPCION_ID));
    }

    @Test
    @DisplayName("obtenerSuscripcionPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerPorId_inexistente_lanzaNotFound() {
        when(suscripcionDAO.findById(SUSCRIPCION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerSuscripcionPorId(SUSCRIPCION_ID));
    }

    @Test
    @DisplayName("obtenerTodasLasSuscripciones: delega en el DAO")
    void obtenerTodas_devuelveLista() {
        when(suscripcionDAO.findAll()).thenReturn(List.of(suscripcion("ACTIVA", LocalDate.now().plusDays(5))));

        assertEquals(1, service.obtenerTodasLasSuscripciones().size());
    }

    @Test
    @DisplayName("eliminarSuscripcion: elimina cuando existe")
    void eliminar_existente_elimina() {
        when(suscripcionDAO.deleteById(SUSCRIPCION_ID)).thenReturn(true);

        service.eliminarSuscripcion(SUSCRIPCION_ID);

        verify(suscripcionDAO).deleteById(SUSCRIPCION_ID);
    }

    @Test
    @DisplayName("eliminarSuscripcion: inexistente lanza ResourceNotFoundException")
    void eliminar_inexistente_lanzaNotFound() {
        when(suscripcionDAO.deleteById(SUSCRIPCION_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarSuscripcion(SUSCRIPCION_ID));
    }

    // ---------------------------------------------------------------- actualizarSuscripcion

    @Test
    @DisplayName("actualizarSuscripcion: suscripcion inexistente lanza ResourceNotFoundException")
    void actualizar_inexistente_lanzaNotFound() {
        when(suscripcionDAO.findById(SUSCRIPCION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarSuscripcion(SUSCRIPCION_ID, dto));
    }

    @Test
    @DisplayName("actualizarSuscripcion: no permite cambiar el usuario")
    void actualizar_cambiaUsuario_lanzaBusinessValidation() {
        when(suscripcionDAO.findById(SUSCRIPCION_ID))
                .thenReturn(Optional.of(suscripcion("ACTIVA", LocalDate.now().plusMonths(1))));
        dto.setUsuarioId(99L);

        assertThrows(BusinessValidationException.class, () -> service.actualizarSuscripcion(SUSCRIPCION_ID, dto));

        verify(suscripcionDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarSuscripcion: con el mismo plan conserva el descuento original")
    void actualizar_mismoPlan_conservaDescuento() {
        when(suscripcionDAO.findById(SUSCRIPCION_ID))
                .thenReturn(Optional.of(suscripcion("ACTIVA", LocalDate.now().plusMonths(1))));
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(planSuscripcionDAO.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(suscripcionDAO.update(eq(SUSCRIPCION_ID), any(SuscripcionDTO.class)))
                .thenAnswer(inv -> Optional.of(inv.getArgument(1)));
        dto.setDescuentoSalas(new BigDecimal("77")); // se ignora

        SuscripcionDTO resultado = service.actualizarSuscripcion(SUSCRIPCION_ID, dto);

        assertEquals(new BigDecimal("5"), resultado.getDescuentoSalas());
    }

    @Test
    @DisplayName("actualizarSuscripcion: al cambiar de plan toma el descuento del plan nuevo")
    void actualizar_cambiaPlan_tomaDescuentoDelPlanNuevo() {
        when(suscripcionDAO.findById(SUSCRIPCION_ID))
                .thenReturn(Optional.of(suscripcion("ACTIVA", LocalDate.now().plusMonths(1))));
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(planSuscripcionDAO.findById(OTRO_PLAN_ID))
                .thenReturn(Optional.of(plan(OTRO_PLAN_ID, new BigDecimal("25"), true)));
        when(suscripcionDAO.update(eq(SUSCRIPCION_ID), any(SuscripcionDTO.class)))
                .thenAnswer(inv -> Optional.of(inv.getArgument(1)));
        dto.setPlanId(OTRO_PLAN_ID);

        SuscripcionDTO resultado = service.actualizarSuscripcion(SUSCRIPCION_ID, dto);

        assertEquals(new BigDecimal("25"), resultado.getDescuentoSalas());
    }

    @Test
    @DisplayName("actualizarSuscripcion: al cambiar a un plan inactivo lanza BusinessValidationException")
    void actualizar_cambiaAPlanInactivo_lanzaBusinessValidation() {
        when(suscripcionDAO.findById(SUSCRIPCION_ID))
                .thenReturn(Optional.of(suscripcion("ACTIVA", LocalDate.now().plusMonths(1))));
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(planSuscripcionDAO.findById(OTRO_PLAN_ID))
                .thenReturn(Optional.of(plan(OTRO_PLAN_ID, new BigDecimal("25"), false)));
        dto.setPlanId(OTRO_PLAN_ID);

        assertThrows(BusinessValidationException.class, () -> service.actualizarSuscripcion(SUSCRIPCION_ID, dto));

        verify(suscripcionDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarSuscripcion: al cancelar registra la fechaCancelacion")
    void actualizar_cancelar_registraFechaCancelacion() {
        when(suscripcionDAO.findById(SUSCRIPCION_ID))
                .thenReturn(Optional.of(suscripcion("ACTIVA", LocalDate.now().plusMonths(1))));
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(planSuscripcionDAO.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(suscripcionDAO.update(eq(SUSCRIPCION_ID), any(SuscripcionDTO.class)))
                .thenAnswer(inv -> Optional.of(inv.getArgument(1)));
        dto.setEstado("CANCELADA");

        SuscripcionDTO resultado = service.actualizarSuscripcion(SUSCRIPCION_ID, dto);

        assertNotNull(resultado.getFechaCancelacion());
    }

    @Test
    @DisplayName("actualizarSuscripcion: si el DAO no devuelve la suscripcion lanza ResourceNotFoundException")
    void actualizar_daoNoActualiza_lanzaNotFound() {
        when(suscripcionDAO.findById(SUSCRIPCION_ID))
                .thenReturn(Optional.of(suscripcion("ACTIVA", LocalDate.now().plusMonths(1))));
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(planSuscripcionDAO.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(suscripcionDAO.update(eq(SUSCRIPCION_ID), any(SuscripcionDTO.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarSuscripcion(SUSCRIPCION_ID, dto));
    }

    // ---------------------------------------------------------------- comprarSuscripcion

    private CompraSuscripcionDTO compra(Boolean renovacion) {
        CompraSuscripcionDTO c = new CompraSuscripcionDTO();
        c.setUsuarioId(USUARIO_ID);
        c.setPlanId(PLAN_ID);
        c.setRenovacionAutomatica(renovacion);
        return c;
    }

    @Test
    @DisplayName("comprarSuscripcion: usuario inexistente lanza ResourceNotFoundException")
    void comprar_usuarioInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.comprarSuscripcion(compra(true)));
    }

    @Test
    @DisplayName("comprarSuscripcion: plan inexistente lanza ResourceNotFoundException")
    void comprar_planInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(planSuscripcionDAO.findById(PLAN_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.comprarSuscripcion(compra(true)));
    }

    @Test
    @DisplayName("comprarSuscripcion: plan inactivo lanza BusinessValidationException")
    void comprar_planInactivo_lanzaBusinessValidation() {
        plan.setActivo(false);
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(planSuscripcionDAO.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        assertThrows(BusinessValidationException.class, () -> service.comprarSuscripcion(compra(true)));
    }

    @Test
    @DisplayName("comprarSuscripcion: usuario con suscripcion vigente lanza BusinessValidationException")
    void comprar_yaTieneVigente_lanzaBusinessValidation() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(planSuscripcionDAO.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        stubVigente(Optional.of(suscripcion("ACTIVA", LocalDate.now().plusMonths(2))));

        assertThrows(BusinessValidationException.class, () -> service.comprarSuscripcion(compra(true)));

        verify(suscripcionDAO, never()).save(any());
    }

    @Test
    @DisplayName("comprarSuscripcion: crea una suscripcion PENDIENTE_PAGO con valor mensual, fechas y descuento del plan")
    void comprar_nueva_quedaPendienteDePago() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(planSuscripcionDAO.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(suscripcionDAO.save(any(SuscripcionDTO.class))).thenAnswer(inv -> inv.getArgument(0));

        SuscripcionDTO resultado = service.comprarSuscripcion(compra(true));

        assertEquals("PENDIENTE_PAGO", resultado.getEstado());
        assertEquals(USUARIO_ID, resultado.getUsuarioId());
        assertEquals(PLAN_ID, resultado.getPlanId());
        assertEquals(LocalDate.now(), resultado.getFechaInicio());
        assertEquals(LocalDate.now().plusMonths(6), resultado.getFechaFin());
        assertEquals(0, new BigDecimal("20000.00").compareTo(resultado.getValorMensual()));
        assertEquals(new BigDecimal("10"), resultado.getDescuentoSalas());
        assertTrue(resultado.getRenovacionAutomatica());
        assertEquals(0, resultado.getMesesConsecutivos());
    }

    @Test
    @DisplayName("comprarSuscripcion: renovacionAutomatica nula se interpreta como false")
    void comprar_sinRenovacion_quedaEnFalse() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(planSuscripcionDAO.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(suscripcionDAO.save(any(SuscripcionDTO.class))).thenAnswer(inv -> inv.getArgument(0));

        assertFalse(service.comprarSuscripcion(compra(null)).getRenovacionAutomatica());
    }

    @Test
    @DisplayName("comprarSuscripcion: reutiliza la compra pendiente de pago en lugar de crear otra")
    void comprar_conPendiente_laReutiliza() {
        SuscripcionDTO pendiente = suscripcion("PENDIENTE_PAGO", LocalDate.now().plusMonths(1));
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(planSuscripcionDAO.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(suscripcionDAO.findByUsuarioIdAndEstado(USUARIO_ID, "PENDIENTE_PAGO"))
                .thenReturn(Optional.of(pendiente));
        when(suscripcionDAO.update(eq(SUSCRIPCION_ID), any(SuscripcionDTO.class)))
                .thenAnswer(inv -> Optional.of(inv.getArgument(1)));

        SuscripcionDTO resultado = service.comprarSuscripcion(compra(false));

        assertEquals(SUSCRIPCION_ID, resultado.getId());
        assertEquals(LocalDate.now().plusMonths(6), resultado.getFechaFin());
        verify(suscripcionDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- activarSuscripcion

    @Test
    @DisplayName("activarSuscripcion: suscripcion inexistente lanza ResourceNotFoundException")
    void activar_inexistente_lanzaNotFound() {
        when(suscripcionDAO.findById(SUSCRIPCION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.activarSuscripcion(SUSCRIPCION_ID));
    }

    @Test
    @DisplayName("activarSuscripcion: si no esta pendiente de pago lanza BusinessValidationException")
    void activar_noPendiente_lanzaBusinessValidation() {
        when(suscripcionDAO.findById(SUSCRIPCION_ID))
                .thenReturn(Optional.of(suscripcion("ACTIVA", LocalDate.now().plusMonths(1))));

        assertThrows(BusinessValidationException.class, () -> service.activarSuscripcion(SUSCRIPCION_ID));

        verify(suscripcionDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("activarSuscripcion: plan inexistente lanza ResourceNotFoundException")
    void activar_planInexistente_lanzaNotFound() {
        when(suscripcionDAO.findById(SUSCRIPCION_ID))
                .thenReturn(Optional.of(suscripcion("PENDIENTE_PAGO", LocalDate.now().plusMonths(1))));
        when(planSuscripcionDAO.findById(PLAN_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.activarSuscripcion(SUSCRIPCION_ID));
    }

    @Test
    @DisplayName("activarSuscripcion: pasa a ACTIVA y el periodo empieza hoy")
    void activar_pendiente_quedaActiva() {
        when(suscripcionDAO.findById(SUSCRIPCION_ID))
                .thenReturn(Optional.of(suscripcion("PENDIENTE_PAGO", LocalDate.now().plusMonths(1))));
        when(planSuscripcionDAO.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(suscripcionDAO.update(eq(SUSCRIPCION_ID), any(SuscripcionDTO.class)))
                .thenAnswer(inv -> Optional.of(inv.getArgument(1)));

        SuscripcionDTO resultado = service.activarSuscripcion(SUSCRIPCION_ID);

        assertEquals("ACTIVA", resultado.getEstado());
        assertEquals(LocalDate.now(), resultado.getFechaInicio());
        assertEquals(LocalDate.now().plusMonths(6), resultado.getFechaFin());
    }

    // ---------------------------------------------------------------- cancelarSuscripcion

    @Test
    @DisplayName("cancelarSuscripcion: suscripcion inexistente lanza ResourceNotFoundException")
    void cancelar_inexistente_lanzaNotFound() {
        when(suscripcionDAO.findById(SUSCRIPCION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.cancelarSuscripcion(SUSCRIPCION_ID, USUARIO_ID));
    }

    @Test
    @DisplayName("cancelarSuscripcion: de otro usuario lanza BusinessValidationException")
    void cancelar_deOtroUsuario_lanzaBusinessValidation() {
        when(suscripcionDAO.findById(SUSCRIPCION_ID))
                .thenReturn(Optional.of(suscripcion("ACTIVA", LocalDate.now().plusMonths(1))));

        assertThrows(BusinessValidationException.class, () -> service.cancelarSuscripcion(SUSCRIPCION_ID, 99L));

        verify(suscripcionDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("cancelarSuscripcion: ya cancelada lanza BusinessValidationException")
    void cancelar_yaCancelada_lanzaBusinessValidation() {
        when(suscripcionDAO.findById(SUSCRIPCION_ID))
                .thenReturn(Optional.of(suscripcion("CANCELADA", LocalDate.now().plusMonths(1))));

        assertThrows(BusinessValidationException.class, () -> service.cancelarSuscripcion(SUSCRIPCION_ID, USUARIO_ID));
    }

    @Test
    @DisplayName("cancelarSuscripcion: si no esta ACTIVA lanza BusinessValidationException")
    void cancelar_noActiva_lanzaBusinessValidation() {
        when(suscripcionDAO.findById(SUSCRIPCION_ID))
                .thenReturn(Optional.of(suscripcion("PENDIENTE_PAGO", LocalDate.now().plusMonths(1))));

        assertThrows(BusinessValidationException.class, () -> service.cancelarSuscripcion(SUSCRIPCION_ID, USUARIO_ID));
    }

    @Test
    @DisplayName("cancelarSuscripcion: ya finalizada lanza BusinessValidationException")
    void cancelar_yaFinalizada_lanzaBusinessValidation() {
        when(suscripcionDAO.findById(SUSCRIPCION_ID))
                .thenReturn(Optional.of(suscripcion("ACTIVA", LocalDate.now().minusDays(1))));

        assertThrows(BusinessValidationException.class, () -> service.cancelarSuscripcion(SUSCRIPCION_ID, USUARIO_ID));
    }

    @Test
    @DisplayName("cancelarSuscripcion: queda CANCELADA, con fecha de cancelacion y sin renovacion automatica")
    void cancelar_activa_quedaCancelada() {
        when(suscripcionDAO.findById(SUSCRIPCION_ID))
                .thenReturn(Optional.of(suscripcion("ACTIVA", LocalDate.now().plusMonths(1))));
        when(suscripcionDAO.update(eq(SUSCRIPCION_ID), any(SuscripcionDTO.class)))
                .thenAnswer(inv -> Optional.of(inv.getArgument(1)));

        SuscripcionDTO resultado = service.cancelarSuscripcion(SUSCRIPCION_ID, USUARIO_ID);

        assertEquals("CANCELADA", resultado.getEstado());
        assertNotNull(resultado.getFechaCancelacion());
        assertFalse(resultado.getRenovacionAutomatica());
    }

    // ---------------------------------------------------------------- vigencia

    @Test
    @DisplayName("obtenerSuscripcionVigente: usuario inexistente lanza ResourceNotFoundException")
    void obtenerVigente_usuarioInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerSuscripcionVigente(USUARIO_ID));
    }

    @Test
    @DisplayName("obtenerSuscripcionVigente: sin suscripcion vigente lanza ResourceNotFoundException")
    void obtenerVigente_sinVigente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        stubVigente(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerSuscripcionVigente(USUARIO_ID));
    }

    @Test
    @DisplayName("obtenerSuscripcionVigente: devuelve la suscripcion vigente")
    void obtenerVigente_conVigente_laDevuelve() {
        SuscripcionDTO vigente = suscripcion("ACTIVA", LocalDate.now().plusMonths(1));
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        stubVigente(Optional.of(vigente));

        assertSame(vigente, service.obtenerSuscripcionVigente(USUARIO_ID));
    }

    @Test
    @DisplayName("tieneSuscripcionVigente: true cuando hay una vigente")
    void tieneVigente_conVigente_devuelveTrue() {
        stubVigente(Optional.of(suscripcion("CANCELADA", LocalDate.now().plusDays(3))));

        assertTrue(service.tieneSuscripcionVigente(USUARIO_ID));
    }

    @Test
    @DisplayName("tieneSuscripcionVigente: false cuando no hay ninguna")
    void tieneVigente_sinVigente_devuelveFalse() {
        stubVigente(Optional.empty());

        assertFalse(service.tieneSuscripcionVigente(USUARIO_ID));
    }

    @Test
    @DisplayName("buscarSuscripcionVigente: devuelve el Optional del DAO")
    void buscarVigente_devuelveOptional() {
        SuscripcionDTO vigente = suscripcion("ACTIVA", LocalDate.now().plusMonths(1));
        stubVigente(Optional.of(vigente));

        assertEquals(Optional.of(vigente), service.buscarSuscripcionVigente(USUARIO_ID));
    }

    @Test
    @DisplayName("exigirSuscripcionVigente: usuario inexistente lanza ResourceNotFoundException")
    void exigirVigente_usuarioInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.exigirSuscripcionVigente(USUARIO_ID));
    }

    @Test
    @DisplayName("exigirSuscripcionVigente: sin suscripcion vigente lanza BusinessValidationException")
    void exigirVigente_sinVigente_lanzaBusinessValidation() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        stubVigente(Optional.empty());

        assertThrows(BusinessValidationException.class, () -> service.exigirSuscripcionVigente(USUARIO_ID));
    }

    @Test
    @DisplayName("exigirSuscripcionVigente: con suscripcion vigente no lanza excepcion")
    void exigirVigente_conVigente_noLanza() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        stubVigente(Optional.of(suscripcion("ACTIVA", LocalDate.now().plusMonths(1))));

        assertDoesNotThrow(() -> service.exigirSuscripcionVigente(USUARIO_ID));
    }

    // ---------------------------------------------------------------- marcarSuscripcionesVencidas

    @Test
    @DisplayName("marcarSuscripcionesVencidas: marca cada ACTIVA vencida como VENCIDA y devuelve la cantidad")
    void marcarVencidas_conVencidas_lasMarca() {
        SuscripcionDTO a = suscripcion("ACTIVA", LocalDate.now().minusDays(2));
        a.setId(1L);
        SuscripcionDTO b = suscripcion("ACTIVA", LocalDate.now().minusDays(1));
        b.setId(2L);
        when(suscripcionDAO.findByEstadoIgnoreCaseAndFechaFinBefore(eq("ACTIVA"), any(LocalDate.class)))
                .thenReturn(List.of(a, b));

        int cantidad = service.marcarSuscripcionesVencidas();

        assertEquals(2, cantidad);
        assertEquals("VENCIDA", a.getEstado());
        assertEquals("VENCIDA", b.getEstado());
        ArgumentCaptor<Long> ids = ArgumentCaptor.forClass(Long.class);
        verify(suscripcionDAO, times(2)).update(ids.capture(), any(SuscripcionDTO.class));
        assertEquals(List.of(1L, 2L), ids.getAllValues());
    }

    @Test
    @DisplayName("marcarSuscripcionesVencidas: sin vencidas devuelve 0 y no actualiza nada")
    void marcarVencidas_sinVencidas_devuelveCero() {
        when(suscripcionDAO.findByEstadoIgnoreCaseAndFechaFinBefore(eq("ACTIVA"), any(LocalDate.class)))
                .thenReturn(List.of());

        assertEquals(0, service.marcarSuscripcionesVencidas());

        verify(suscripcionDAO, never()).update(any(), any());
    }
}
