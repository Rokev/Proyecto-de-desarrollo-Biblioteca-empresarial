package bussinesLayer.service.impl;

import bussinesLayer.dto.DevolucionDTO;
import bussinesLayer.dto.EjemplarDTO;
import bussinesLayer.dto.PedidoInterciudadDTO;
import bussinesLayer.dto.PrestamoDTO;
import bussinesLayer.dto.SedeDTO;
import bussinesLayer.dto.SolicitudDevolucionDTO;
import bussinesLayer.dto.TrasladoRetornoDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.AutomatizacionService;
import bussinesLayer.service.NotificacionService;
import bussinesLayer.service.ReservaLibroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.DevolucionDAO;
import persistanceLayer.dao.EjemplarDAO;
import persistanceLayer.dao.PedidoInterciudadDAO;
import persistanceLayer.dao.PrestamoDAO;
import persistanceLayer.dao.SedeDAO;
import persistanceLayer.dao.TrasladoRetornoDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DevolucionServiceImpl")
class DevolucionServiceImplTest {

    private static final Long PRESTAMO_ID = 1L;
    private static final Long LIBRO_ID = 2L;
    private static final Long SEDE_DEVOLUCION_ID = 3L;
    private static final Long SEDE_ORIGEN_ID = 5L;
    private static final Long USUARIO_ID = 7L;
    private static final Long BIBLIOTECARIO_ID = 9L;
    private static final Long EJEMPLAR_ID = 20L;
    private static final Long DEVOLUCION_ID = 100L;
    private static final String CODIGO_QR = "DEV-ABC";

    @Mock private DevolucionDAO devolucionDAO;
    @Mock private PrestamoDAO prestamoDAO;
    @Mock private SedeDAO sedeDAO;
    @Mock private UsuarioDAO usuarioDAO;
    @Mock private EjemplarDAO ejemplarDAO;
    @Mock private TrasladoRetornoDAO trasladoRetornoDAO;
    @Mock private PedidoInterciudadDAO pedidoInterciudadDAO;
    @Mock private ReservaLibroService reservaLibroService;
    @Mock private NotificacionService notificacionService;
    @Mock private AutomatizacionService automatizacionService;

    @InjectMocks
    private DevolucionServiceImpl service;

    private DevolucionDTO dto;

    @BeforeEach
    void setUp() {
        dto = new DevolucionDTO();
        dto.setCodigoQr(CODIGO_QR);
        dto.setPrestamoId(PRESTAMO_ID);
        dto.setSedeDevolucionId(SEDE_DEVOLUCION_ID);
    }

    // ------------------------------------------------------------ helpers

    private DevolucionDTO devolucion(String estado) {
        DevolucionDTO d = new DevolucionDTO();
        d.setId(DEVOLUCION_ID);
        d.setCodigoQr(CODIGO_QR);
        d.setPrestamoId(PRESTAMO_ID);
        d.setSedeDevolucionId(SEDE_DEVOLUCION_ID);
        d.setEstado(estado);
        return d;
    }

    private PrestamoDTO prestamo(String estado) {
        PrestamoDTO p = new PrestamoDTO();
        p.setId(PRESTAMO_ID);
        p.setUsuarioId(USUARIO_ID);
        p.setEjemplarId(EJEMPLAR_ID);
        p.setEstado(estado);
        return p;
    }

    private SedeDTO sede(Long id, String nombre, boolean enOperacion) {
        SedeDTO s = new SedeDTO();
        s.setId(id);
        s.setNombre(nombre);
        s.setEnOperacion(enOperacion);
        return s;
    }

    private EjemplarDTO ejemplar(Long sedeId) {
        EjemplarDTO e = new EjemplarDTO();
        e.setId(EJEMPLAR_ID);
        e.setLibroId(LIBRO_ID);
        e.setSedeId(sedeId);
        e.setEstado("PRESTADO");
        return e;
    }

    private PedidoInterciudadDTO pedido(Long sedeDevolucionId) {
        PedidoInterciudadDTO p = new PedidoInterciudadDTO();
        p.setId(50L);
        p.setPrestamoId(PRESTAMO_ID);
        p.setSedeDevolucionId(sedeDevolucionId);
        p.setSedeDevolucionNombre("Sede Pedido");
        return p;
    }

    private SolicitudDevolucionDTO solicitud() {
        SolicitudDevolucionDTO s = new SolicitudDevolucionDTO();
        s.setPrestamoId(PRESTAMO_ID);
        s.setUsuarioId(USUARIO_ID);
        s.setSedeDevolucionId(SEDE_DEVOLUCION_ID);
        return s;
    }

    private void referenciasValidas() {
        when(prestamoDAO.existsById(PRESTAMO_ID)).thenReturn(true);
        when(sedeDAO.existsById(SEDE_DEVOLUCION_ID)).thenReturn(true);
    }

    private void devolucionUpdateDevuelveElArgumento() {
        when(devolucionDAO.update(eq(DEVOLUCION_ID), any(DevolucionDTO.class)))
                .thenAnswer(inv -> Optional.of(inv.getArgument(1)));
    }

    // ---------------------------------------------------------------- crearDevolucion

    @Test
    @DisplayName("crearDevolucion: sin estado queda PENDIENTE y se guarda")
    void crear_valida_quedaPendiente() {
        referenciasValidas();
        when(devolucionDAO.save(any(DevolucionDTO.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("PENDIENTE", service.crearDevolucion(dto).getEstado());
    }

    @Test
    @DisplayName("crearDevolucion: conserva el estado si ya viene informado")
    void crear_conEstado_loConserva() {
        referenciasValidas();
        dto.setEstado("CONFIRMADA");
        when(devolucionDAO.save(any(DevolucionDTO.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("CONFIRMADA", service.crearDevolucion(dto).getEstado());
    }

    @Test
    @DisplayName("crearDevolucion: prestamo inexistente lanza ResourceNotFoundException")
    void crear_prestamoInexistente_lanzaNotFound() {
        when(prestamoDAO.existsById(PRESTAMO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearDevolucion(dto));

        verify(devolucionDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearDevolucion: sede inexistente lanza ResourceNotFoundException")
    void crear_sedeInexistente_lanzaNotFound() {
        when(prestamoDAO.existsById(PRESTAMO_ID)).thenReturn(true);
        when(sedeDAO.existsById(SEDE_DEVOLUCION_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearDevolucion(dto));
    }

    @Test
    @DisplayName("crearDevolucion: bibliotecario informado e inexistente lanza ResourceNotFoundException")
    void crear_bibliotecarioInexistente_lanzaNotFound() {
        referenciasValidas();
        dto.setBibliotecarioId(BIBLIOTECARIO_ID);
        when(usuarioDAO.existsById(BIBLIOTECARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearDevolucion(dto));
    }

    @Test
    @DisplayName("crearDevolucion: prestamo con devolucion previa lanza BusinessValidationException")
    void crear_prestamoYaTieneDevolucion_lanzaBusinessValidation() {
        referenciasValidas();
        when(devolucionDAO.existsByPrestamoId(PRESTAMO_ID)).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearDevolucion(dto));

        verify(devolucionDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearDevolucion: codigo QR duplicado lanza BusinessValidationException")
    void crear_qrDuplicado_lanzaBusinessValidation() {
        referenciasValidas();
        when(devolucionDAO.findByCodigoQr(CODIGO_QR)).thenReturn(Optional.of(devolucion("PENDIENTE")));

        assertThrows(BusinessValidationException.class, () -> service.crearDevolucion(dto));

        verify(devolucionDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- consultas y eliminar

    @Test
    @DisplayName("obtenerDevolucionPorId: devuelve la devolucion cuando existe")
    void obtenerPorId_existente_laDevuelve() {
        DevolucionDTO d = devolucion("PENDIENTE");
        when(devolucionDAO.findById(DEVOLUCION_ID)).thenReturn(Optional.of(d));

        assertSame(d, service.obtenerDevolucionPorId(DEVOLUCION_ID));
    }

    @Test
    @DisplayName("obtenerDevolucionPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerPorId_inexistente_lanzaNotFound() {
        when(devolucionDAO.findById(DEVOLUCION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerDevolucionPorId(DEVOLUCION_ID));
    }

    @Test
    @DisplayName("obtenerTodasLasDevoluciones: delega en el DAO")
    void obtenerTodas_devuelveLista() {
        when(devolucionDAO.findAll()).thenReturn(List.of(devolucion("PENDIENTE")));

        assertEquals(1, service.obtenerTodasLasDevoluciones().size());
    }

    @Test
    @DisplayName("eliminarDevolucion: elimina cuando existe")
    void eliminar_existente_elimina() {
        when(devolucionDAO.deleteById(DEVOLUCION_ID)).thenReturn(true);

        service.eliminarDevolucion(DEVOLUCION_ID);

        verify(devolucionDAO).deleteById(DEVOLUCION_ID);
    }

    @Test
    @DisplayName("eliminarDevolucion: inexistente lanza ResourceNotFoundException")
    void eliminar_inexistente_lanzaNotFound() {
        when(devolucionDAO.deleteById(DEVOLUCION_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarDevolucion(DEVOLUCION_ID));
    }

    // ---------------------------------------------------------------- actualizarDevolucion

    @Test
    @DisplayName("actualizarDevolucion: devolucion inexistente lanza ResourceNotFoundException")
    void actualizar_inexistente_lanzaNotFound() {
        when(devolucionDAO.existsById(DEVOLUCION_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarDevolucion(DEVOLUCION_ID, dto));
    }

    @Test
    @DisplayName("actualizarDevolucion: prestamo asignado a otra devolucion lanza BusinessValidationException")
    void actualizar_prestamoDeOtraDevolucion_lanzaBusinessValidation() {
        when(devolucionDAO.existsById(DEVOLUCION_ID)).thenReturn(true);
        referenciasValidas();
        DevolucionDTO otra = devolucion("PENDIENTE");
        otra.setId(999L);
        when(devolucionDAO.findByPrestamoId(PRESTAMO_ID)).thenReturn(Optional.of(otra));

        assertThrows(BusinessValidationException.class, () -> service.actualizarDevolucion(DEVOLUCION_ID, dto));

        verify(devolucionDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarDevolucion: codigo QR de otra devolucion lanza BusinessValidationException")
    void actualizar_qrDeOtraDevolucion_lanzaBusinessValidation() {
        when(devolucionDAO.existsById(DEVOLUCION_ID)).thenReturn(true);
        referenciasValidas();
        DevolucionDTO otra = devolucion("PENDIENTE");
        otra.setId(999L);
        when(devolucionDAO.findByCodigoQr(CODIGO_QR)).thenReturn(Optional.of(otra));

        assertThrows(BusinessValidationException.class, () -> service.actualizarDevolucion(DEVOLUCION_ID, dto));

        verify(devolucionDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarDevolucion: los datos de la misma devolucion no cuentan como conflicto")
    void actualizar_mismaDevolucion_actualiza() {
        when(devolucionDAO.existsById(DEVOLUCION_ID)).thenReturn(true);
        referenciasValidas();
        when(devolucionDAO.findByPrestamoId(PRESTAMO_ID)).thenReturn(Optional.of(devolucion("PENDIENTE")));
        when(devolucionDAO.findByCodigoQr(CODIGO_QR)).thenReturn(Optional.of(devolucion("PENDIENTE")));
        devolucionUpdateDevuelveElArgumento();

        assertSame(dto, service.actualizarDevolucion(DEVOLUCION_ID, dto));
    }

    @Test
    @DisplayName("actualizarDevolucion: si el DAO no devuelve la devolucion lanza ResourceNotFoundException")
    void actualizar_daoNoActualiza_lanzaNotFound() {
        when(devolucionDAO.existsById(DEVOLUCION_ID)).thenReturn(true);
        referenciasValidas();
        when(devolucionDAO.update(eq(DEVOLUCION_ID), any(DevolucionDTO.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarDevolucion(DEVOLUCION_ID, dto));
    }

    // ---------------------------------------------------------------- sugerirSedeCercana

    @Test
    @DisplayName("sugerirSedeCercana: devuelve la sede mas cercana")
    void sugerirSede_existente_laDevuelve() {
        SedeDTO s = sede(SEDE_DEVOLUCION_ID, "Sede Norte", true);
        when(sedeDAO.findMasCercana(4.5, -75.7)).thenReturn(Optional.of(s));

        assertSame(s, service.sugerirSedeCercana(4.5, -75.7));
    }

    @Test
    @DisplayName("sugerirSedeCercana: sin sedes en operacion lanza ResourceNotFoundException")
    void sugerirSede_sinSedes_lanzaNotFound() {
        when(sedeDAO.findMasCercana(4.5, -75.7)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.sugerirSedeCercana(4.5, -75.7));
    }

    // ---------------------------------------------------------------- solicitarDevolucion

    @Test
    @DisplayName("solicitarDevolucion: prestamo inexistente lanza ResourceNotFoundException")
    void solicitar_prestamoInexistente_lanzaNotFound() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.solicitarDevolucion(solicitud()));
    }

    @Test
    @DisplayName("solicitarDevolucion: prestamo de otro usuario lanza BusinessValidationException")
    void solicitar_prestamoDeOtroUsuario_lanzaBusinessValidation() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo("ACTIVO")));
        SolicitudDevolucionDTO s = solicitud();
        s.setUsuarioId(99L);

        assertThrows(BusinessValidationException.class, () -> service.solicitarDevolucion(s));
    }

    @Test
    @DisplayName("solicitarDevolucion: prestamo que no esta ACTIVO ni VENCIDO lanza BusinessValidationException")
    void solicitar_prestamoNoDevolvible_lanzaBusinessValidation() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo("DEVUELTO")));

        assertThrows(BusinessValidationException.class, () -> service.solicitarDevolucion(solicitud()));
    }

    @Test
    @DisplayName("solicitarDevolucion: pedido interciudad en una sede distinta a la elegida lanza BusinessValidationException")
    void solicitar_pedidoInterciudadSedeDistinta_lanzaBusinessValidation() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo("ACTIVO")));
        when(pedidoInterciudadDAO.findByPrestamoId(PRESTAMO_ID)).thenReturn(Optional.of(pedido(77L)));

        assertThrows(BusinessValidationException.class, () -> service.solicitarDevolucion(solicitud()));

        verify(devolucionDAO, never()).save(any());
    }

    @Test
    @DisplayName("solicitarDevolucion: pedido interciudad sin sede indicada fuerza la sede del pedido")
    void solicitar_pedidoInterciudad_fuerzaSedeDelPedido() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo("ACTIVO")));
        when(pedidoInterciudadDAO.findByPrestamoId(PRESTAMO_ID)).thenReturn(Optional.of(pedido(SEDE_DEVOLUCION_ID)));
        when(sedeDAO.findById(SEDE_DEVOLUCION_ID))
                .thenReturn(Optional.of(sede(SEDE_DEVOLUCION_ID, "Sede Norte", true)));
        when(devolucionDAO.save(any(DevolucionDTO.class))).thenAnswer(inv -> inv.getArgument(0));
        SolicitudDevolucionDTO s = solicitud();
        s.setSedeDevolucionId(null);

        DevolucionDTO resultado = service.solicitarDevolucion(s);

        assertEquals(SEDE_DEVOLUCION_ID, resultado.getSedeDevolucionId());
    }

    @Test
    @DisplayName("solicitarDevolucion: sede inexistente lanza ResourceNotFoundException")
    void solicitar_sedeInexistente_lanzaNotFound() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo("ACTIVO")));
        when(sedeDAO.findById(SEDE_DEVOLUCION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.solicitarDevolucion(solicitud()));
    }

    @Test
    @DisplayName("solicitarDevolucion: sede fuera de operacion lanza BusinessValidationException")
    void solicitar_sedeFueraDeOperacion_lanzaBusinessValidation() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo("ACTIVO")));
        when(sedeDAO.findById(SEDE_DEVOLUCION_ID))
                .thenReturn(Optional.of(sede(SEDE_DEVOLUCION_ID, "Sede Norte", false)));

        assertThrows(BusinessValidationException.class, () -> service.solicitarDevolucion(solicitud()));
    }

    @Test
    @DisplayName("solicitarDevolucion: sin sede ni coordenadas lanza BusinessValidationException")
    void solicitar_sinSedeNiCoordenadas_lanzaBusinessValidation() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo("ACTIVO")));
        SolicitudDevolucionDTO s = solicitud();
        s.setSedeDevolucionId(null);

        assertThrows(BusinessValidationException.class, () -> service.solicitarDevolucion(s));
    }

    @Test
    @DisplayName("solicitarDevolucion: con coordenadas usa la sede mas cercana")
    void solicitar_conCoordenadas_usaSedeMasCercana() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo("ACTIVO")));
        when(sedeDAO.findMasCercana(4.5, -75.7))
                .thenReturn(Optional.of(sede(SEDE_DEVOLUCION_ID, "Sede Norte", true)));
        when(devolucionDAO.save(any(DevolucionDTO.class))).thenAnswer(inv -> inv.getArgument(0));
        SolicitudDevolucionDTO s = solicitud();
        s.setSedeDevolucionId(null);
        s.setLatitud(4.5);
        s.setLongitud(-75.7);

        assertEquals(SEDE_DEVOLUCION_ID, service.solicitarDevolucion(s).getSedeDevolucionId());
    }

    @Test
    @DisplayName("solicitarDevolucion: con coordenadas y ninguna sede cercana lanza BusinessValidationException")
    void solicitar_coordenadasSinSedeCercana_lanzaBusinessValidation() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo("ACTIVO")));
        when(sedeDAO.findMasCercana(4.5, -75.7)).thenReturn(Optional.empty());
        SolicitudDevolucionDTO s = solicitud();
        s.setSedeDevolucionId(null);
        s.setLatitud(4.5);
        s.setLongitud(-75.7);

        assertThrows(BusinessValidationException.class, () -> service.solicitarDevolucion(s));
    }

    @Test
    @DisplayName("solicitarDevolucion: con una devolucion vigente para el prestamo lanza BusinessValidationException")
    void solicitar_yaExisteDevolucionVigente_lanzaBusinessValidation() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo("ACTIVO")));
        when(sedeDAO.findById(SEDE_DEVOLUCION_ID))
                .thenReturn(Optional.of(sede(SEDE_DEVOLUCION_ID, "Sede Norte", true)));
        when(devolucionDAO.findByPrestamoId(PRESTAMO_ID)).thenReturn(Optional.of(devolucion("PENDIENTE")));

        assertThrows(BusinessValidationException.class, () -> service.solicitarDevolucion(solicitud()));

        verify(devolucionDAO, never()).save(any());
    }

    @Test
    @DisplayName("solicitarDevolucion: crea una devolucion PENDIENTE con codigo QR nuevo")
    void solicitar_valida_creaDevolucionPendiente() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo("VENCIDO")));
        when(sedeDAO.findById(SEDE_DEVOLUCION_ID))
                .thenReturn(Optional.of(sede(SEDE_DEVOLUCION_ID, "Sede Norte", true)));
        when(devolucionDAO.save(any(DevolucionDTO.class))).thenAnswer(inv -> inv.getArgument(0));

        DevolucionDTO resultado = service.solicitarDevolucion(solicitud());

        assertEquals("PENDIENTE", resultado.getEstado());
        assertEquals(PRESTAMO_ID, resultado.getPrestamoId());
        assertEquals(SEDE_DEVOLUCION_ID, resultado.getSedeDevolucionId());
        assertTrue(resultado.getCodigoQr().startsWith("DEV-"));
        assertNotNull(resultado.getFechaSolicitud());
    }

    @Test
    @DisplayName("solicitarDevolucion: si la anterior fue RECHAZADA la reabre con un QR nuevo")
    void solicitar_devolucionRechazada_laReabre() {
        DevolucionDTO rechazada = devolucion("RECHAZADA");
        rechazada.setFechaConfirmacion(LocalDateTime.now().minusDays(1));
        rechazada.setBibliotecarioId(BIBLIOTECARIO_ID);
        rechazada.setObservaciones("Libro danado");
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo("ACTIVO")));
        when(sedeDAO.findById(SEDE_DEVOLUCION_ID))
                .thenReturn(Optional.of(sede(SEDE_DEVOLUCION_ID, "Sede Norte", true)));
        when(devolucionDAO.findByPrestamoId(PRESTAMO_ID)).thenReturn(Optional.of(rechazada));
        devolucionUpdateDevuelveElArgumento();

        DevolucionDTO resultado = service.solicitarDevolucion(solicitud());

        assertEquals("PENDIENTE", resultado.getEstado());
        assertNull(resultado.getFechaConfirmacion());
        assertNull(resultado.getBibliotecarioId());
        assertNull(resultado.getObservaciones());
        assertTrue(resultado.getCodigoQr().startsWith("DEV-"));
        verify(devolucionDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- confirmarDevolucion

    @Test
    @DisplayName("confirmarDevolucion: bibliotecario inexistente lanza ResourceNotFoundException")
    void confirmar_bibliotecarioInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(BIBLIOTECARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.confirmarDevolucion(CODIGO_QR, BIBLIOTECARIO_ID));
    }

    @Test
    @DisplayName("confirmarDevolucion: codigo QR inexistente lanza ResourceNotFoundException")
    void confirmar_qrInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(BIBLIOTECARIO_ID)).thenReturn(true);
        when(devolucionDAO.findByCodigoQr(CODIGO_QR)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.confirmarDevolucion(CODIGO_QR, BIBLIOTECARIO_ID));
    }

    @Test
    @DisplayName("confirmarDevolucion: devolucion que no esta PENDIENTE lanza BusinessValidationException")
    void confirmar_noPendiente_lanzaBusinessValidation() {
        when(usuarioDAO.existsById(BIBLIOTECARIO_ID)).thenReturn(true);
        when(devolucionDAO.findByCodigoQr(CODIGO_QR)).thenReturn(Optional.of(devolucion("CONFIRMADA")));

        assertThrows(BusinessValidationException.class, () -> service.confirmarDevolucion(CODIGO_QR, BIBLIOTECARIO_ID));
    }

    @Test
    @DisplayName("confirmarDevolucion: prestamo inexistente lanza ResourceNotFoundException")
    void confirmar_prestamoInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(BIBLIOTECARIO_ID)).thenReturn(true);
        when(devolucionDAO.findByCodigoQr(CODIGO_QR)).thenReturn(Optional.of(devolucion("PENDIENTE")));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.confirmarDevolucion(CODIGO_QR, BIBLIOTECARIO_ID));
    }

    @Test
    @DisplayName("confirmarDevolucion: prestamo ya devuelto lanza BusinessValidationException")
    void confirmar_prestamoYaDevuelto_lanzaBusinessValidation() {
        when(usuarioDAO.existsById(BIBLIOTECARIO_ID)).thenReturn(true);
        when(devolucionDAO.findByCodigoQr(CODIGO_QR)).thenReturn(Optional.of(devolucion("PENDIENTE")));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo("DEVUELTO")));

        assertThrows(BusinessValidationException.class, () -> service.confirmarDevolucion(CODIGO_QR, BIBLIOTECARIO_ID));
    }

    @Test
    @DisplayName("confirmarDevolucion: pedido interciudad devuelto en otra sede lanza BusinessValidationException")
    void confirmar_pedidoInterciudadSedeDistinta_lanzaBusinessValidation() {
        when(usuarioDAO.existsById(BIBLIOTECARIO_ID)).thenReturn(true);
        when(devolucionDAO.findByCodigoQr(CODIGO_QR)).thenReturn(Optional.of(devolucion("PENDIENTE")));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo("ACTIVO")));
        when(pedidoInterciudadDAO.findByPrestamoId(PRESTAMO_ID)).thenReturn(Optional.of(pedido(77L)));

        assertThrows(BusinessValidationException.class, () -> service.confirmarDevolucion(CODIGO_QR, BIBLIOTECARIO_ID));

        verify(pedidoInterciudadDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("confirmarDevolucion: ejemplar inexistente lanza ResourceNotFoundException")
    void confirmar_ejemplarInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(BIBLIOTECARIO_ID)).thenReturn(true);
        when(devolucionDAO.findByCodigoQr(CODIGO_QR)).thenReturn(Optional.of(devolucion("PENDIENTE")));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo("ACTIVO")));
        when(ejemplarDAO.findById(EJEMPLAR_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.confirmarDevolucion(CODIGO_QR, BIBLIOTECARIO_ID));
    }

    @Test
    @DisplayName("confirmarDevolucion: si el ejemplar no estaba PRESTADO lanza BusinessValidationException")
    void confirmar_ejemplarNoPrestado_lanzaBusinessValidation() {
        when(usuarioDAO.existsById(BIBLIOTECARIO_ID)).thenReturn(true);
        when(devolucionDAO.findByCodigoQr(CODIGO_QR)).thenReturn(Optional.of(devolucion("PENDIENTE")));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo("ACTIVO")));
        when(ejemplarDAO.findById(EJEMPLAR_ID)).thenReturn(Optional.of(ejemplar(SEDE_DEVOLUCION_ID)));
        devolucionUpdateDevuelveElArgumento();
        when(ejemplarDAO.cambiarEstadoSiEs(EJEMPLAR_ID, "PRESTADO", "DISPONIBLE")).thenReturn(0);

        assertThrows(BusinessValidationException.class, () -> service.confirmarDevolucion(CODIGO_QR, BIBLIOTECARIO_ID));
    }

    @Test
    @DisplayName("confirmarDevolucion: en la sede de origen deja el ejemplar DISPONIBLE y atiende reservas en espera")
    void confirmar_enSedeOrigen_dejaEjemplarDisponible() {
        PrestamoDTO prestamo = prestamo("ACTIVO");
        when(usuarioDAO.existsById(BIBLIOTECARIO_ID)).thenReturn(true);
        when(devolucionDAO.findByCodigoQr(CODIGO_QR)).thenReturn(Optional.of(devolucion("PENDIENTE")));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo));
        when(ejemplarDAO.findById(EJEMPLAR_ID)).thenReturn(Optional.of(ejemplar(SEDE_DEVOLUCION_ID)));
        devolucionUpdateDevuelveElArgumento();
        when(ejemplarDAO.cambiarEstadoSiEs(EJEMPLAR_ID, "PRESTADO", "DISPONIBLE")).thenReturn(1);

        DevolucionDTO resultado = service.confirmarDevolucion(CODIGO_QR, BIBLIOTECARIO_ID);

        assertEquals("CONFIRMADA", resultado.getEstado());
        assertEquals(BIBLIOTECARIO_ID, resultado.getBibliotecarioId());
        assertNotNull(resultado.getFechaConfirmacion());
        assertEquals("DEVUELTO", prestamo.getEstado());
        assertNotNull(prestamo.getFechaDevolucion());
        verify(prestamoDAO).update(PRESTAMO_ID, prestamo);
        verify(automatizacionService).evaluarRetrasoAlDevolver(PRESTAMO_ID);
        verify(reservaLibroService).asignarEjemplarAReservaEnEspera(EJEMPLAR_ID);
        verify(trasladoRetornoDAO, never()).save(any());
        verify(notificacionService, never()).notificarSede(any(), any(), any(), any());
    }

    @Test
    @DisplayName("confirmarDevolucion: en otra sede deja el ejemplar EN_TRANSITO, crea el traslado y avisa")
    void confirmar_enOtraSede_creaTrasladoYNotifica() {
        when(usuarioDAO.existsById(BIBLIOTECARIO_ID)).thenReturn(true);
        when(devolucionDAO.findByCodigoQr(CODIGO_QR)).thenReturn(Optional.of(devolucion("PENDIENTE")));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo("ACTIVO")));
        when(ejemplarDAO.findById(EJEMPLAR_ID)).thenReturn(Optional.of(ejemplar(SEDE_ORIGEN_ID)));
        devolucionUpdateDevuelveElArgumento();
        when(ejemplarDAO.cambiarEstadoSiEs(EJEMPLAR_ID, "PRESTADO", "EN_TRANSITO")).thenReturn(1);
        when(sedeDAO.findById(SEDE_DEVOLUCION_ID))
                .thenReturn(Optional.of(sede(SEDE_DEVOLUCION_ID, "Sede Norte", true)));

        service.confirmarDevolucion(CODIGO_QR, BIBLIOTECARIO_ID);

        ArgumentCaptor<TrasladoRetornoDTO> captor = ArgumentCaptor.forClass(TrasladoRetornoDTO.class);
        verify(trasladoRetornoDAO).save(captor.capture());
        TrasladoRetornoDTO traslado = captor.getValue();
        assertEquals("EN_TRANSITO", traslado.getEstado());
        assertEquals(EJEMPLAR_ID, traslado.getEjemplarId());
        assertEquals(SEDE_DEVOLUCION_ID, traslado.getSedeActualId());
        assertEquals(SEDE_ORIGEN_ID, traslado.getSedeDestinoId());
        assertEquals(DEVOLUCION_ID, traslado.getDevolucionId());

        verify(reservaLibroService).avisarDevolucionEnCamino(LIBRO_ID, SEDE_ORIGEN_ID, "Sede Norte");
        verify(notificacionService).notificarSede(eq(SEDE_ORIGEN_ID), eq("DEVOLUCION_EN_OTRA_SEDE"),
                anyString(), anyString());
        verify(reservaLibroService, never()).asignarEjemplarAReservaEnEspera(any());
    }

    @Test
    @DisplayName("confirmarDevolucion: si es un pedido interciudad lo pasa a DEVUELTO_PENDIENTE_TRASLADO")
    void confirmar_pedidoInterciudad_actualizaElPedido() {
        PedidoInterciudadDTO pedido = pedido(SEDE_DEVOLUCION_ID);
        when(usuarioDAO.existsById(BIBLIOTECARIO_ID)).thenReturn(true);
        when(devolucionDAO.findByCodigoQr(CODIGO_QR)).thenReturn(Optional.of(devolucion("PENDIENTE")));
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamo("ACTIVO")));
        when(pedidoInterciudadDAO.findByPrestamoId(PRESTAMO_ID)).thenReturn(Optional.of(pedido));
        when(ejemplarDAO.findById(EJEMPLAR_ID)).thenReturn(Optional.of(ejemplar(SEDE_ORIGEN_ID)));
        devolucionUpdateDevuelveElArgumento();
        when(ejemplarDAO.cambiarEstadoSiEs(EJEMPLAR_ID, "PRESTADO", "EN_TRANSITO")).thenReturn(1);

        service.confirmarDevolucion(CODIGO_QR, BIBLIOTECARIO_ID);

        assertEquals("DEVUELTO_PENDIENTE_TRASLADO", pedido.getEstado());
        assertEquals(BIBLIOTECARIO_ID, pedido.getBibliotecarioId());
        verify(pedidoInterciudadDAO).update(pedido.getId(), pedido);
    }
}