package bussinesLayer.service.impl;

import bussinesLayer.dto.EjemplarDTO;
import bussinesLayer.dto.LibroDTO;
import bussinesLayer.dto.PrestamoDTO;
import bussinesLayer.dto.PrestamoDetalleDTO;
import bussinesLayer.dto.SedeDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.ConfiguracionSistemaService;
import bussinesLayer.service.ValidadorUsuarioService;
import bussinesLayer.util.ClavePlazo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.EjemplarDAO;
import persistanceLayer.dao.LibroDAO;
import persistanceLayer.dao.PrestamoDAO;
import persistanceLayer.dao.SancionDAO;
import persistanceLayer.dao.SedeDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrestamoServiceImpl")
class PrestamoServiceImplTest {

    private static final Long USUARIO_ID = 1L;
    private static final Long EJEMPLAR_ID = 2L;
    private static final Long SEDE_ID = 3L;
    private static final Long PRESTAMO_ID = 10L;

    @Mock private PrestamoDAO prestamoDAO;
    @Mock private UsuarioDAO usuarioDAO;
    @Mock private EjemplarDAO ejemplarDAO;
    @Mock private SedeDAO sedeDAO;
    @Mock private LibroDAO libroDAO;
    @Mock private SancionDAO sancionDAO;
    @Mock private ValidadorUsuarioService validadorUsuarioService;
    @Mock private ConfiguracionSistemaService configuracionService;

    @InjectMocks
    private PrestamoServiceImpl service;

    private PrestamoDTO dto;
    private EjemplarDTO ejemplar;

    @BeforeEach
    void setUp() {
        dto = new PrestamoDTO();
        dto.setUsuarioId(USUARIO_ID);
        dto.setEjemplarId(EJEMPLAR_ID);
        dto.setSedeRetiroId(SEDE_ID);
        dto.setFechaSolicitud(LocalDateTime.now());

        ejemplar = new EjemplarDTO();
        ejemplar.setId(EJEMPLAR_ID);
        ejemplar.setEstado("DISPONIBLE");
    }

    /** Deja listos los mocks para el "camino feliz" de crearPrestamo. */
    private void prepararCreacionValida() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(ejemplarDAO.findById(EJEMPLAR_ID)).thenReturn(Optional.of(ejemplar));
        when(prestamoDAO.countByUsuarioIdAndEstadoIn(eq(USUARIO_ID), anyCollection())).thenReturn(0L);
        when(prestamoDAO.existsByEjemplarIdAndEstadoIn(eq(EJEMPLAR_ID), anyCollection())).thenReturn(false);
    }

    /** El plazo de prestamo ya no es fijo: sale de la configuracion del sistema. */
    private void plazoPrestamo14Dias() {
        when(configuracionService.plazo(ClavePlazo.DIAS_PRESTAMO)).thenReturn(14);
    }

    private PrestamoDTO prestamoExistente(String estado) {
        PrestamoDTO p = new PrestamoDTO();
        p.setId(PRESTAMO_ID);
        p.setUsuarioId(USUARIO_ID);
        p.setEjemplarId(EJEMPLAR_ID);
        p.setSedeRetiroId(SEDE_ID);
        p.setEstado(estado);
        p.setVecesRenovado(1);
        return p;
    }

    // ---------------------------------------------------------------- crearPrestamo

    @Test
    @DisplayName("crearPrestamo: guarda el prestamo con valores por defecto (plazo configurado) y marca el ejemplar como PRESTADO")
    void crearPrestamo_valido_aplicaDefaultsYReservaEjemplar() {
        prepararCreacionValida();
        plazoPrestamo14Dias();
        when(prestamoDAO.save(any(PrestamoDTO.class))).thenAnswer(inv -> {
            PrestamoDTO p = inv.getArgument(0);
            p.setId(PRESTAMO_ID);
            return p;
        });

        PrestamoDTO resultado = service.crearPrestamo(dto);

        assertEquals(PRESTAMO_ID, resultado.getId());
        assertEquals("SOLICITADO", resultado.getEstado());
        assertEquals("SEDE", resultado.getModalidad());
        assertEquals(0, resultado.getVecesRenovado());
        assertNull(resultado.getFechaDevolucion());
        assertEquals(LocalDate.now().plusDays(14), resultado.getFechaVencimiento());

        ArgumentCaptor<EjemplarDTO> captor = ArgumentCaptor.forClass(EjemplarDTO.class);
        verify(ejemplarDAO).update(eq(EJEMPLAR_ID), captor.capture());
        assertEquals("PRESTADO", captor.getValue().getEstado());
    }

    @Test
    @DisplayName("crearPrestamo: respeta una fechaVencimiento valida enviada por el usuario")
    void crearPrestamo_conFechaVencimientoValida_laConserva() {
        prepararCreacionValida();
        LocalDate vencimiento = LocalDate.now().plusDays(5);
        dto.setFechaVencimiento(vencimiento);
        when(prestamoDAO.save(any(PrestamoDTO.class))).thenAnswer(inv -> inv.getArgument(0));

        PrestamoDTO resultado = service.crearPrestamo(dto);

        assertEquals(vencimiento, resultado.getFechaVencimiento());
    }

    @Test
    @DisplayName("crearPrestamo: normaliza la modalidad a mayusculas")
    void crearPrestamo_modalidadDomicilioConDireccion_laNormaliza() {
        prepararCreacionValida();
        plazoPrestamo14Dias();
        dto.setModalidad("domicilio");
        dto.setDireccionEntrega("Calle 35 # 34-57");
        when(prestamoDAO.save(any(PrestamoDTO.class))).thenAnswer(inv -> inv.getArgument(0));

        PrestamoDTO resultado = service.crearPrestamo(dto);

        assertEquals("DOMICILIO", resultado.getModalidad());
    }

    @Test
    @DisplayName("crearPrestamo: si el usuario no esta habilitado propaga la excepcion y no consulta nada mas")
    void crearPrestamo_usuarioNoHabilitado_propagaExcepcion() {
        org.mockito.Mockito.doThrow(new BusinessValidationException("Usuario con sancion activa"))
                .when(validadorUsuarioService).validarUsuarioHabilitadoParaOperaciones(USUARIO_ID);

        assertThrows(BusinessValidationException.class, () -> service.crearPrestamo(dto));

        verifyNoInteractions(usuarioDAO, ejemplarDAO, prestamoDAO);
    }

    @Test
    @DisplayName("crearPrestamo: usuario inexistente lanza ResourceNotFoundException")
    void crearPrestamo_usuarioInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearPrestamo(dto));

        verify(prestamoDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearPrestamo: sede inexistente lanza ResourceNotFoundException")
    void crearPrestamo_sedeInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearPrestamo(dto));

        verify(prestamoDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearPrestamo: ejemplar inexistente lanza ResourceNotFoundException")
    void crearPrestamo_ejemplarInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(ejemplarDAO.findById(EJEMPLAR_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.crearPrestamo(dto));

        verify(prestamoDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearPrestamo: con 3 prestamos vigentes lanza BusinessValidationException")
    void crearPrestamo_alcanzoMaximoVigentes_lanzaBusinessValidation() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(ejemplarDAO.findById(EJEMPLAR_ID)).thenReturn(Optional.of(ejemplar));
        when(prestamoDAO.countByUsuarioIdAndEstadoIn(eq(USUARIO_ID), anyCollection())).thenReturn(3L);

        assertThrows(BusinessValidationException.class, () -> service.crearPrestamo(dto));

        verify(prestamoDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearPrestamo: con 2 prestamos vigentes todavia permite crear uno nuevo")
    void crearPrestamo_conDosVigentes_aunPermite() {
        plazoPrestamo14Dias();
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(ejemplarDAO.findById(EJEMPLAR_ID)).thenReturn(Optional.of(ejemplar));
        when(prestamoDAO.countByUsuarioIdAndEstadoIn(eq(USUARIO_ID), anyCollection())).thenReturn(2L);
        when(prestamoDAO.existsByEjemplarIdAndEstadoIn(eq(EJEMPLAR_ID), anyCollection())).thenReturn(false);
        when(prestamoDAO.save(any(PrestamoDTO.class))).thenAnswer(inv -> inv.getArgument(0));

        assertNotNull(service.crearPrestamo(dto));
    }
    @Test
    @DisplayName("crearPrestamo: ejemplar que no esta DISPONIBLE lanza BusinessValidationException")
    void crearPrestamo_ejemplarNoDisponible_lanzaBusinessValidation() {
        ejemplar.setEstado("PRESTADO");
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(ejemplarDAO.findById(EJEMPLAR_ID)).thenReturn(Optional.of(ejemplar));
        when(prestamoDAO.countByUsuarioIdAndEstadoIn(eq(USUARIO_ID), anyCollection())).thenReturn(0L);

        assertThrows(BusinessValidationException.class, () -> service.crearPrestamo(dto));

        verify(prestamoDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearPrestamo: ejemplar DISPONIBLE pero con prestamo vigente lanza BusinessValidationException")
    void crearPrestamo_ejemplarConPrestamoVigente_lanzaBusinessValidation() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(ejemplarDAO.findById(EJEMPLAR_ID)).thenReturn(Optional.of(ejemplar));
        when(prestamoDAO.countByUsuarioIdAndEstadoIn(eq(USUARIO_ID), anyCollection())).thenReturn(0L);
        when(prestamoDAO.existsByEjemplarIdAndEstadoIn(eq(EJEMPLAR_ID), anyCollection())).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearPrestamo(dto));

        verify(prestamoDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearPrestamo: modalidad invalida lanza BusinessValidationException")
    void crearPrestamo_modalidadInvalida_lanzaBusinessValidation() {
        prepararCreacionValida();
        dto.setModalidad("DRON");

        assertThrows(BusinessValidationException.class, () -> service.crearPrestamo(dto));

        verify(prestamoDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearPrestamo: modalidad DOMICILIO sin direccion lanza BusinessValidationException")
    void crearPrestamo_domicilioSinDireccion_lanzaBusinessValidation() {
        prepararCreacionValida();
        dto.setModalidad("DOMICILIO");
        dto.setDireccionEntrega("  ");

        assertThrows(BusinessValidationException.class, () -> service.crearPrestamo(dto));

        verify(prestamoDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearPrestamo: fechaVencimiento anterior a la solicitud lanza BusinessValidationException")
    void crearPrestamo_vencimientoAnteriorASolicitud_lanzaBusinessValidation() {
        prepararCreacionValida();
        dto.setFechaVencimiento(LocalDate.now().minusDays(1));

        assertThrows(BusinessValidationException.class, () -> service.crearPrestamo(dto));

        verify(prestamoDAO, never()).save(any());
        verify(ejemplarDAO, never()).update(any(), any());
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerPrestamoPorId: devuelve el prestamo cuando existe")
    void obtenerPrestamoPorId_existente_loDevuelve() {
        PrestamoDTO existente = prestamoExistente("ACTIVO");
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(existente));

        assertSame(existente, service.obtenerPrestamoPorId(PRESTAMO_ID));
    }

    @Test
    @DisplayName("obtenerPrestamoPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerPrestamoPorId_inexistente_lanzaNotFound() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerPrestamoPorId(PRESTAMO_ID));
    }

    @Test
    @DisplayName("obtenerTodosLosPrestamos: delega en el DAO")
    void obtenerTodosLosPrestamos_devuelveLista() {
        List<PrestamoDTO> lista = List.of(prestamoExistente("ACTIVO"), prestamoExistente("DEVUELTO"));
        when(prestamoDAO.findAll()).thenReturn(lista);

        assertEquals(2, service.obtenerTodosLosPrestamos().size());
    }

    // ---------------------------------------------------------------- actualizarPrestamo

    @Test
    @DisplayName("actualizarPrestamo: prestamo inexistente lanza ResourceNotFoundException")
    void actualizarPrestamo_inexistente_lanzaNotFound() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarPrestamo(PRESTAMO_ID, dto));
    }

    @Test
    @DisplayName("actualizarPrestamo: no permite cambiar el usuario del prestamo")
    void actualizarPrestamo_cambiaUsuario_lanzaBusinessValidation() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamoExistente("ACTIVO")));
        dto.setUsuarioId(99L);

        assertThrows(BusinessValidationException.class, () -> service.actualizarPrestamo(PRESTAMO_ID, dto));

        verify(prestamoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarPrestamo: no permite cambiar el ejemplar del prestamo")
    void actualizarPrestamo_cambiaEjemplar_lanzaBusinessValidation() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamoExistente("ACTIVO")));
        dto.setEjemplarId(99L);

        assertThrows(BusinessValidationException.class, () -> service.actualizarPrestamo(PRESTAMO_ID, dto));

        verify(prestamoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarPrestamo: sede inexistente lanza ResourceNotFoundException")
    void actualizarPrestamo_sedeInexistente_lanzaNotFound() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamoExistente("ACTIVO")));
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarPrestamo(PRESTAMO_ID, dto));
    }

    @Test
    @DisplayName("actualizarPrestamo: al pasar a DEVUELTO fija fechaDevolucion y libera el ejemplar")
    void actualizarPrestamo_devolver_liberaEjemplar() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamoExistente("ACTIVO")));
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        ejemplar.setEstado("PRESTADO");
        when(ejemplarDAO.findById(EJEMPLAR_ID)).thenReturn(Optional.of(ejemplar));
        when(prestamoDAO.update(eq(PRESTAMO_ID), any(PrestamoDTO.class)))
                .thenAnswer(inv -> Optional.of(inv.getArgument(1)));
        dto.setEstado("DEVUELTO");

        PrestamoDTO resultado = service.actualizarPrestamo(PRESTAMO_ID, dto);

        assertEquals("DEVUELTO", resultado.getEstado());
        assertNotNull(resultado.getFechaDevolucion());

        ArgumentCaptor<EjemplarDTO> captor = ArgumentCaptor.forClass(EjemplarDTO.class);
        verify(ejemplarDAO).update(eq(EJEMPLAR_ID), captor.capture());
        assertEquals("DISPONIBLE", captor.getValue().getEstado());
    }

    @Test
    @DisplayName("actualizarPrestamo: al pasar a CANCELADO libera el ejemplar")
    void actualizarPrestamo_cancelar_liberaEjemplar() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamoExistente("SOLICITADO")));
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(ejemplarDAO.findById(EJEMPLAR_ID)).thenReturn(Optional.of(ejemplar));
        when(prestamoDAO.update(eq(PRESTAMO_ID), any(PrestamoDTO.class)))
                .thenAnswer(inv -> Optional.of(inv.getArgument(1)));
        dto.setEstado("CANCELADO");

        service.actualizarPrestamo(PRESTAMO_ID, dto);

        verify(ejemplarDAO).update(eq(EJEMPLAR_ID), any(EjemplarDTO.class));
    }

    @Test
    @DisplayName("actualizarPrestamo: al pasar a ACTIVO registra la fechaEntrega")
    void actualizarPrestamo_activar_registraFechaEntrega() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamoExistente("SOLICITADO")));
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(prestamoDAO.update(eq(PRESTAMO_ID), any(PrestamoDTO.class)))
                .thenAnswer(inv -> Optional.of(inv.getArgument(1)));
        dto.setEstado("ACTIVO");

        PrestamoDTO resultado = service.actualizarPrestamo(PRESTAMO_ID, dto);

        assertNotNull(resultado.getFechaEntrega());
        verify(ejemplarDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarPrestamo: sin estado ni vecesRenovado conserva los valores actuales")
    void actualizarPrestamo_sinEstadoNiRenovaciones_conservaActuales() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamoExistente("ACTIVO")));
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(prestamoDAO.update(eq(PRESTAMO_ID), any(PrestamoDTO.class)))
                .thenAnswer(inv -> Optional.of(inv.getArgument(1)));
        dto.setEstado(null);
        dto.setVecesRenovado(null);

        PrestamoDTO resultado = service.actualizarPrestamo(PRESTAMO_ID, dto);

        assertEquals("ACTIVO", resultado.getEstado());
        assertEquals(1, resultado.getVecesRenovado());
    }

    @Test
    @DisplayName("actualizarPrestamo: si el DAO no devuelve el prestamo actualizado lanza ResourceNotFoundException")
    void actualizarPrestamo_daoNoActualiza_lanzaNotFound() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamoExistente("ACTIVO")));
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(prestamoDAO.update(eq(PRESTAMO_ID), any(PrestamoDTO.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarPrestamo(PRESTAMO_ID, dto));
    }

    // ---------------------------------------------------------------- eliminarPrestamo

    @Test
    @DisplayName("eliminarPrestamo: prestamo inexistente lanza ResourceNotFoundException")
    void eliminarPrestamo_inexistente_lanzaNotFound() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarPrestamo(PRESTAMO_ID));

        verify(prestamoDAO, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminarPrestamo: si estaba vigente libera el ejemplar antes de borrar")
    void eliminarPrestamo_vigente_liberaEjemplar() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamoExistente("ACTIVO")));
        ejemplar.setEstado("PRESTADO");
        when(ejemplarDAO.findById(EJEMPLAR_ID)).thenReturn(Optional.of(ejemplar));

        service.eliminarPrestamo(PRESTAMO_ID);

        ArgumentCaptor<EjemplarDTO> captor = ArgumentCaptor.forClass(EjemplarDTO.class);
        verify(ejemplarDAO).update(eq(EJEMPLAR_ID), captor.capture());
        assertEquals("DISPONIBLE", captor.getValue().getEstado());
        verify(prestamoDAO).deleteById(PRESTAMO_ID);
    }

    @Test
    @DisplayName("eliminarPrestamo: si ya estaba DEVUELTO borra sin tocar el ejemplar")
    void eliminarPrestamo_devuelto_noTocaEjemplar() {
        when(prestamoDAO.findById(PRESTAMO_ID)).thenReturn(Optional.of(prestamoExistente("DEVUELTO")));

        service.eliminarPrestamo(PRESTAMO_ID);

        verify(ejemplarDAO, never()).update(any(), any());
        verify(prestamoDAO).deleteById(PRESTAMO_ID);
    }

    // ---------------------------------------------------------------- consultas con detalle

    @Test
    @DisplayName("crearPrestamo: usa el plazo configurado por el administrador, no un valor fijo")
    void crearPrestamo_usaElPlazoConfigurado() {
        prepararCreacionValida();
        when(configuracionService.plazo(ClavePlazo.DIAS_PRESTAMO)).thenReturn(30);
        when(prestamoDAO.save(any(PrestamoDTO.class))).thenAnswer(inv -> inv.getArgument(0));

        PrestamoDTO resultado = service.crearPrestamo(dto);

        assertEquals(LocalDate.now().plusDays(30), resultado.getFechaVencimiento());
    }

    @Test
    @DisplayName("obtenerPrestamosDeUsuario: usuario inexistente lanza ResourceNotFoundException")
    void obtenerPrestamosDeUsuario_usuarioInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerPrestamosDeUsuario(USUARIO_ID, null));
    }

    @Test
    @DisplayName("obtenerPrestamosDeUsuario: sin estado lista todos y arma el detalle con titulo y sede")
    void obtenerPrestamosDeUsuario_sinEstado_armaElDetalle() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(prestamoDAO.findByUsuarioIdOrderByFechaSolicitudDesc(USUARIO_ID))
                .thenReturn(List.of(prestamoExistente("ACTIVO")));
        EjemplarDTO ej = new EjemplarDTO();
        ej.setId(EJEMPLAR_ID);
        ej.setLibroId(77L);
        when(ejemplarDAO.findById(EJEMPLAR_ID)).thenReturn(Optional.of(ej));
        LibroDTO libro = new LibroDTO();
        libro.setId(77L);
        libro.setTitulo("Dune");
        when(libroDAO.findById(77L)).thenReturn(Optional.of(libro));
        SedeDTO sede = new SedeDTO();
        sede.setId(SEDE_ID);
        sede.setNombre("Sede Norte");
        when(sedeDAO.findById(SEDE_ID)).thenReturn(Optional.of(sede));

        List<PrestamoDetalleDTO> resultado = service.obtenerPrestamosDeUsuario(USUARIO_ID, " ");

        assertEquals(1, resultado.size());
        assertEquals("ACTIVO", resultado.get(0).getEstado());
        assertEquals("Dune", resultado.get(0).getLibroTitulo());
        assertEquals("Sede Norte", resultado.get(0).getSedeNombre());
    }

    @Test
    @DisplayName("obtenerPrestamosDeUsuario: con estado filtra por ese estado")
    void obtenerPrestamosDeUsuario_conEstado_filtraPorEstado() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(prestamoDAO.findByUsuarioIdAndEstadoIgnoreCase(USUARIO_ID, "VENCIDO"))
                .thenReturn(List.of(prestamoExistente("VENCIDO")));

        List<PrestamoDetalleDTO> resultado = service.obtenerPrestamosDeUsuario(USUARIO_ID, "VENCIDO");

        assertEquals(1, resultado.size());
        verify(prestamoDAO, never()).findByUsuarioIdOrderByFechaSolicitudDesc(any());
    }

    @Test
    @DisplayName("obtenerPrestamosDeSede: sede inexistente lanza ResourceNotFoundException")
    void obtenerPrestamosDeSede_sedeInexistente_lanzaNotFound() {
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> service.obtenerPrestamosDeSede(SEDE_ID, null, null, null));
    }

    @Test
    @DisplayName("obtenerPrestamosDeSede: filtra por estado ignorando mayusculas")
    void obtenerPrestamosDeSede_filtraPorEstado() {
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(prestamoDAO.findBySedeRetiroId(SEDE_ID))
                .thenReturn(List.of(prestamoExistente("ACTIVO"), prestamoExistente("DEVUELTO")));

        List<PrestamoDetalleDTO> resultado = service.obtenerPrestamosDeSede(SEDE_ID, "activo", null, null);

        assertEquals(1, resultado.size());
        assertEquals("ACTIVO", resultado.get(0).getEstado());
    }

    @Test
    @DisplayName("obtenerPrestamosDeSede: filtra por usuario y por libro")
    void obtenerPrestamosDeSede_filtraPorUsuarioYLibro() {
        PrestamoDTO deOtro = prestamoExistente("ACTIVO");
        deOtro.setUsuarioId(99L);
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(prestamoDAO.findBySedeRetiroId(SEDE_ID)).thenReturn(List.of(prestamoExistente("ACTIVO"), deOtro));
        EjemplarDTO ej = new EjemplarDTO();
        ej.setId(EJEMPLAR_ID);
        ej.setLibroId(77L);
        when(ejemplarDAO.findById(EJEMPLAR_ID)).thenReturn(Optional.of(ej));

        assertEquals(1, service.obtenerPrestamosDeSede(SEDE_ID, null, USUARIO_ID, 77L).size());
        assertEquals(0, service.obtenerPrestamosDeSede(SEDE_ID, null, USUARIO_ID, 88L).size());
    }
}