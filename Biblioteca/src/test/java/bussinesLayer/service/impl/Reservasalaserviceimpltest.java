package bussinesLayer.service.impl;

import bussinesLayer.dto.ReservaSalaDTO;
import bussinesLayer.dto.SalaEstudioDTO;
import bussinesLayer.dto.SuscripcionDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.SuscripcionService;
import bussinesLayer.service.ValidadorUsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.ReservaSalaDAO;
import persistanceLayer.dao.SalaEstudioDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservaSalaServiceImpl")
class ReservaSalaServiceImplTest {

    private static final Long RESERVA_ID = 1L;
    private static final Long USUARIO_ID = 10L;
    private static final Long OTRO_USUARIO_ID = 11L;
    private static final Long SALA_ID = 3L;
    private static final Long OTRA_SALA_ID = 4L;

    @Mock private ReservaSalaDAO reservaSalaDAO;
    @Mock private SalaEstudioDAO salaEstudioDAO;
    @Mock private UsuarioDAO usuarioDAO;
    @Mock private SuscripcionService suscripcionService;
    @Mock private ValidadorUsuarioService validadorUsuarioService;

    @InjectMocks
    private ReservaSalaServiceImpl service;

    private ReservaSalaDTO dto;
    private LocalDateTime inicio;
    private LocalDateTime fin;

    @BeforeEach
    void setUp() {
        inicio = LocalDateTime.now().plusDays(1).withNano(0);
        fin = inicio.plusHours(2);
        dto = new ReservaSalaDTO();
        dto.setUsuarioId(USUARIO_ID);
        dto.setSalaId(SALA_ID);
        dto.setFechaInicio(inicio);
        dto.setFechaFin(fin);
    }

    private SalaEstudioDTO sala(Boolean activa) {
        SalaEstudioDTO s = new SalaEstudioDTO();
        s.setId(SALA_ID);
        s.setActiva(activa);
        s.setTarifaPorHora(new BigDecimal("10000"));
        return s;
    }

    private SuscripcionDTO suscripcionConDescuento(String descuento) {
        SuscripcionDTO s = new SuscripcionDTO();
        s.setDescuentoSalas(new BigDecimal(descuento));
        return s;
    }

    private ReservaSalaDTO reservaActual(String estado) {
        ReservaSalaDTO r = new ReservaSalaDTO();
        r.setId(RESERVA_ID);
        r.setUsuarioId(USUARIO_ID);
        r.setSalaId(SALA_ID);
        r.setFechaInicio(inicio);
        r.setFechaFin(fin);
        r.setEstado(estado);
        r.setValorBase(new BigDecimal("20000.00"));
        r.setDescuento(new BigDecimal("2000.00"));
        r.setValorTotal(new BigDecimal("18000.00"));
        r.setFechaCreacion(LocalDateTime.of(2026, 1, 1, 8, 0));
        return r;
    }

    private void prepararReferenciasValidas() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(salaEstudioDAO.existsById(SALA_ID)).thenReturn(true);
    }

    private void assertMonto(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real),
                "Se esperaba " + esperado + " pero fue " + real);
    }

    // ---------------------------------------------------------------- crearReservaSala

    @Test
    @DisplayName("crearReservaSala: sin suscripcion calcula valorBase por duracion, descuento 0 y estado PENDIENTE_PAGO")
    void crearReserva_sinSuscripcion_calculaValores() {
        prepararReferenciasValidas();
        when(salaEstudioDAO.findById(SALA_ID)).thenReturn(Optional.of(sala(true)));
        when(reservaSalaDAO.existeSolape(SALA_ID, inicio, fin)).thenReturn(false);
        when(suscripcionService.buscarSuscripcionVigente(USUARIO_ID)).thenReturn(Optional.empty());
        when(reservaSalaDAO.save(dto)).thenAnswer(inv -> {
            ReservaSalaDTO r = inv.getArgument(0);
            r.setId(RESERVA_ID);
            return r;
        });

        ReservaSalaDTO resultado = service.crearReservaSala(dto);

        assertEquals(RESERVA_ID, resultado.getId());
        assertMonto("20000", resultado.getValorBase());
        assertMonto("0", resultado.getDescuento());
        assertMonto("20000", resultado.getValorTotal());
        assertEquals("PENDIENTE_PAGO", resultado.getEstado());
        assertNotNull(resultado.getFechaCreacion());
        verify(validadorUsuarioService).validarUsuarioHabilitadoParaOperaciones(USUARIO_ID);
    }

    @Test
    @DisplayName("crearReservaSala: con suscripcion vigente aplica el porcentaje de descuento de salas")
    void crearReserva_conSuscripcion_aplicaDescuento() {
        prepararReferenciasValidas();
        when(salaEstudioDAO.findById(SALA_ID)).thenReturn(Optional.of(sala(true)));
        when(suscripcionService.buscarSuscripcionVigente(USUARIO_ID))
                .thenReturn(Optional.of(suscripcionConDescuento("0.10")));
        when(reservaSalaDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        ReservaSalaDTO resultado = service.crearReservaSala(dto);

        assertMonto("20000", resultado.getValorBase());
        assertMonto("2000", resultado.getDescuento());
        assertMonto("18000", resultado.getValorTotal());
    }

    @Test
    @DisplayName("crearReservaSala: calcula el valor proporcional a los minutos reservados")
    void crearReserva_noventaMinutos_calculaProporcional() {
        dto.setFechaFin(inicio.plusMinutes(90));
        prepararReferenciasValidas();
        when(salaEstudioDAO.findById(SALA_ID)).thenReturn(Optional.of(sala(true)));
        when(suscripcionService.buscarSuscripcionVigente(USUARIO_ID)).thenReturn(Optional.empty());
        when(reservaSalaDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        ReservaSalaDTO resultado = service.crearReservaSala(dto);

        assertMonto("15000", resultado.getValorBase());
        assertMonto("15000", resultado.getValorTotal());
    }

    @Test
    @DisplayName("crearReservaSala: conserva el estado si ya viene informado")
    void crearReserva_conEstado_loConserva() {
        dto.setEstado("CONFIRMADA");
        prepararReferenciasValidas();
        when(salaEstudioDAO.findById(SALA_ID)).thenReturn(Optional.of(sala(true)));
        when(suscripcionService.buscarSuscripcionVigente(USUARIO_ID)).thenReturn(Optional.empty());
        when(reservaSalaDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("CONFIRMADA", service.crearReservaSala(dto).getEstado());
    }

    @Test
    @DisplayName("crearReservaSala: usuario con multas o sanciones lanza BusinessValidationException")
    void crearReserva_usuarioNoHabilitado_lanzaBusinessValidation() {
        doThrow(new BusinessValidationException("Usuario bloqueado"))
                .when(validadorUsuarioService).validarUsuarioHabilitadoParaOperaciones(USUARIO_ID);

        assertThrows(BusinessValidationException.class, () -> service.crearReservaSala(dto));

        verify(reservaSalaDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearReservaSala: usuario inexistente lanza ResourceNotFoundException")
    void crearReserva_usuarioInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearReservaSala(dto));

        verify(reservaSalaDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearReservaSala: sala inexistente lanza ResourceNotFoundException")
    void crearReserva_salaInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(salaEstudioDAO.existsById(SALA_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearReservaSala(dto));

        verify(reservaSalaDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearReservaSala: fechaFin no posterior a fechaInicio lanza BusinessValidationException")
    void crearReserva_finNoPosterior_lanzaBusinessValidation() {
        dto.setFechaFin(inicio);
        prepararReferenciasValidas();

        assertThrows(BusinessValidationException.class, () -> service.crearReservaSala(dto));

        verify(reservaSalaDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearReservaSala: fechaInicio en el pasado lanza BusinessValidationException")
    void crearReserva_inicioEnPasado_lanzaBusinessValidation() {
        dto.setFechaInicio(LocalDateTime.now().minusDays(1));
        dto.setFechaFin(LocalDateTime.now().minusDays(1).plusHours(1));
        prepararReferenciasValidas();

        assertThrows(BusinessValidationException.class, () -> service.crearReservaSala(dto));

        verify(reservaSalaDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearReservaSala: si la sala no se encuentra al consultarla lanza ResourceNotFoundException")
    void crearReserva_salaNoEncontradaAlConsultar_lanzaNotFound() {
        prepararReferenciasValidas();
        when(salaEstudioDAO.findById(SALA_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.crearReservaSala(dto));

        verify(reservaSalaDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearReservaSala: sala inactiva lanza BusinessValidationException")
    void crearReserva_salaInactiva_lanzaBusinessValidation() {
        prepararReferenciasValidas();
        when(salaEstudioDAO.findById(SALA_ID)).thenReturn(Optional.of(sala(false)));

        assertThrows(BusinessValidationException.class, () -> service.crearReservaSala(dto));

        verify(reservaSalaDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearReservaSala: sala sin estado activo definido lanza BusinessValidationException")
    void crearReserva_salaActivaNula_lanzaBusinessValidation() {
        prepararReferenciasValidas();
        when(salaEstudioDAO.findById(SALA_ID)).thenReturn(Optional.of(sala(null)));

        assertThrows(BusinessValidationException.class, () -> service.crearReservaSala(dto));

        verify(reservaSalaDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearReservaSala: horario solapado con otra reserva lanza BusinessValidationException")
    void crearReserva_conSolape_lanzaBusinessValidation() {
        prepararReferenciasValidas();
        when(salaEstudioDAO.findById(SALA_ID)).thenReturn(Optional.of(sala(true)));
        when(reservaSalaDAO.existeSolape(SALA_ID, inicio, fin)).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearReservaSala(dto));

        verify(reservaSalaDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerReservaSalaPorId: devuelve la reserva cuando existe")
    void obtenerReservaPorId_existente_laDevuelve() {
        ReservaSalaDTO existente = reservaActual("PENDIENTE_PAGO");
        when(reservaSalaDAO.findById(RESERVA_ID)).thenReturn(Optional.of(existente));

        assertSame(existente, service.obtenerReservaSalaPorId(RESERVA_ID));
    }

    @Test
    @DisplayName("obtenerReservaSalaPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerReservaPorId_inexistente_lanzaNotFound() {
        when(reservaSalaDAO.findById(RESERVA_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerReservaSalaPorId(RESERVA_ID));
    }

    @Test
    @DisplayName("obtenerTodasLasReservasSala: delega en el DAO")
    void obtenerTodasLasReservas_devuelveLista() {
        when(reservaSalaDAO.findAll()).thenReturn(List.of(reservaActual("A"), reservaActual("B")));

        assertEquals(2, service.obtenerTodasLasReservasSala().size());
    }

    // ---------------------------------------------------------------- actualizarReservaSala

    @Test
    @DisplayName("actualizarReservaSala: reserva inexistente lanza ResourceNotFoundException")
    void actualizarReserva_inexistente_lanzaNotFound() {
        when(reservaSalaDAO.findById(RESERVA_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarReservaSala(RESERVA_ID, dto));

        verify(reservaSalaDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarReservaSala: usuario inexistente lanza ResourceNotFoundException")
    void actualizarReserva_usuarioInexistente_lanzaNotFound() {
        when(reservaSalaDAO.findById(RESERVA_ID)).thenReturn(Optional.of(reservaActual("PENDIENTE_PAGO")));
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarReservaSala(RESERVA_ID, dto));

        verify(reservaSalaDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarReservaSala: fechaFin no posterior a fechaInicio lanza BusinessValidationException")
    void actualizarReserva_fechasInvalidas_lanzaBusinessValidation() {
        dto.setFechaFin(inicio);
        when(reservaSalaDAO.findById(RESERVA_ID)).thenReturn(Optional.of(reservaActual("PENDIENTE_PAGO")));
        prepararReferenciasValidas();

        assertThrows(BusinessValidationException.class, () -> service.actualizarReservaSala(RESERVA_ID, dto));

        verify(reservaSalaDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarReservaSala: cambiar el usuario lanza BusinessValidationException")
    void actualizarReserva_cambiaUsuario_lanzaBusinessValidation() {
        dto.setUsuarioId(OTRO_USUARIO_ID);
        when(reservaSalaDAO.findById(RESERVA_ID)).thenReturn(Optional.of(reservaActual("PENDIENTE_PAGO")));
        when(usuarioDAO.existsById(OTRO_USUARIO_ID)).thenReturn(true);
        when(salaEstudioDAO.existsById(SALA_ID)).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.actualizarReservaSala(RESERVA_ID, dto));

        verify(reservaSalaDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarReservaSala: cambiar la sala lanza BusinessValidationException")
    void actualizarReserva_cambiaSala_lanzaBusinessValidation() {
        dto.setSalaId(OTRA_SALA_ID);
        when(reservaSalaDAO.findById(RESERVA_ID)).thenReturn(Optional.of(reservaActual("PENDIENTE_PAGO")));
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(salaEstudioDAO.existsById(OTRA_SALA_ID)).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.actualizarReservaSala(RESERVA_ID, dto));

        verify(reservaSalaDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarReservaSala: cambiar la fechaInicio lanza BusinessValidationException")
    void actualizarReserva_cambiaInicio_lanzaBusinessValidation() {
        dto.setFechaInicio(inicio.plusMinutes(30));
        when(reservaSalaDAO.findById(RESERVA_ID)).thenReturn(Optional.of(reservaActual("PENDIENTE_PAGO")));
        prepararReferenciasValidas();

        assertThrows(BusinessValidationException.class, () -> service.actualizarReservaSala(RESERVA_ID, dto));

        verify(reservaSalaDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarReservaSala: cambiar la fechaFin lanza BusinessValidationException")
    void actualizarReserva_cambiaFin_lanzaBusinessValidation() {
        dto.setFechaFin(fin.plusHours(1));
        when(reservaSalaDAO.findById(RESERVA_ID)).thenReturn(Optional.of(reservaActual("PENDIENTE_PAGO")));
        prepararReferenciasValidas();

        assertThrows(BusinessValidationException.class, () -> service.actualizarReservaSala(RESERVA_ID, dto));

        verify(reservaSalaDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarReservaSala: conserva los valores y la fechaCreacion originales aunque el cliente envie otros")
    void actualizarReserva_valida_conservaValoresOriginales() {
        dto.setEstado("CONFIRMADA");
        dto.setValorBase(new BigDecimal("1"));
        dto.setDescuento(new BigDecimal("1"));
        dto.setValorTotal(new BigDecimal("1"));
        ReservaSalaDTO actual = reservaActual("PENDIENTE_PAGO");
        when(reservaSalaDAO.findById(RESERVA_ID)).thenReturn(Optional.of(actual));
        prepararReferenciasValidas();
        when(reservaSalaDAO.update(RESERVA_ID, dto)).thenReturn(Optional.of(dto));

        ReservaSalaDTO resultado = service.actualizarReservaSala(RESERVA_ID, dto);

        assertMonto("20000", resultado.getValorBase());
        assertMonto("2000", resultado.getDescuento());
        assertMonto("18000", resultado.getValorTotal());
        assertEquals(actual.getFechaCreacion(), resultado.getFechaCreacion());
        assertNull(resultado.getFechaCancelacion());
    }

    @Test
    @DisplayName("actualizarReservaSala: cancelar una reserva fija fechaCancelacion")
    void actualizarReserva_cancelar_fijaFechaCancelacion() {
        dto.setEstado("cancelada");
        when(reservaSalaDAO.findById(RESERVA_ID)).thenReturn(Optional.of(reservaActual("CONFIRMADA")));
        prepararReferenciasValidas();
        when(reservaSalaDAO.update(RESERVA_ID, dto)).thenReturn(Optional.of(dto));

        assertNotNull(service.actualizarReservaSala(RESERVA_ID, dto).getFechaCancelacion());
    }

    @Test
    @DisplayName("actualizarReservaSala: cancelar conserva la fechaCancelacion si ya viene informada")
    void actualizarReserva_cancelarConFecha_laConserva() {
        LocalDateTime fecha = LocalDateTime.of(2026, 7, 7, 7, 0);
        dto.setEstado("CANCELADA");
        dto.setFechaCancelacion(fecha);
        when(reservaSalaDAO.findById(RESERVA_ID)).thenReturn(Optional.of(reservaActual("CONFIRMADA")));
        prepararReferenciasValidas();
        when(reservaSalaDAO.update(RESERVA_ID, dto)).thenReturn(Optional.of(dto));

        assertEquals(fecha, service.actualizarReservaSala(RESERVA_ID, dto).getFechaCancelacion());
    }

    @Test
    @DisplayName("actualizarReservaSala: una reserva ya cancelada no recibe nueva fechaCancelacion")
    void actualizarReserva_yaCancelada_noFijaFecha() {
        dto.setEstado("CANCELADA");
        when(reservaSalaDAO.findById(RESERVA_ID)).thenReturn(Optional.of(reservaActual("CANCELADA")));
        prepararReferenciasValidas();
        when(reservaSalaDAO.update(RESERVA_ID, dto)).thenReturn(Optional.of(dto));

        assertNull(service.actualizarReservaSala(RESERVA_ID, dto).getFechaCancelacion());
    }

    @Test
    @DisplayName("actualizarReservaSala: si el DAO no devuelve la reserva actualizada lanza ResourceNotFoundException")
    void actualizarReserva_daoNoActualiza_lanzaNotFound() {
        when(reservaSalaDAO.findById(RESERVA_ID)).thenReturn(Optional.of(reservaActual("PENDIENTE_PAGO")));
        prepararReferenciasValidas();
        when(reservaSalaDAO.update(RESERVA_ID, dto)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarReservaSala(RESERVA_ID, dto));
    }

    // ---------------------------------------------------------------- eliminarReservaSala

    @Test
    @DisplayName("eliminarReservaSala: elimina cuando la reserva existe")
    void eliminarReserva_existente_elimina() {
        when(reservaSalaDAO.deleteById(RESERVA_ID)).thenReturn(true);

        service.eliminarReservaSala(RESERVA_ID);

        verify(reservaSalaDAO).deleteById(RESERVA_ID);
    }

    @Test
    @DisplayName("eliminarReservaSala: reserva inexistente lanza ResourceNotFoundException")
    void eliminarReserva_inexistente_lanzaNotFound() {
        when(reservaSalaDAO.deleteById(RESERVA_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarReservaSala(RESERVA_ID));
    }
}