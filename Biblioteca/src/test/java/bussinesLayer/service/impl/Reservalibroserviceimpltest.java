package bussinesLayer.service.impl;

import bussinesLayer.dto.EjemplarDTO;
import bussinesLayer.dto.LibroDTO;
import bussinesLayer.dto.NotificacionDTO;
import bussinesLayer.dto.ReservaLibroDTO;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.EjemplarDAO;
import persistanceLayer.dao.LibroDAO;
import persistanceLayer.dao.NotificacionDAO;
import persistanceLayer.dao.ReservaLibroDAO;
import persistanceLayer.dao.SedeDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.util.List;
import java.util.Optional;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservaLibroServiceImpl")
class ReservaLibroServiceImplTest {

    private static final Long USUARIO_ID = 1L;
    private static final Long LIBRO_ID = 2L;
    private static final Long SEDE_ID = 3L;
    private static final Long EJEMPLAR_ID = 20L;
    private static final Long RESERVA_ID = 10L;

    @Mock private NotificacionDAO notificacionDAO;
    @Mock private ReservaLibroDAO reservaLibroDAO;
    @Mock private UsuarioDAO usuarioDAO;
    @Mock private LibroDAO libroDAO;
    @Mock private SedeDAO sedeDAO;
    @Mock private EjemplarDAO ejemplarDAO;
    @Mock private ValidadorUsuarioService validadorUsuarioService;
    @Mock private ConfiguracionSistemaService configuracionService;

    @InjectMocks
    private ReservaLibroServiceImpl service;

    private ReservaLibroDTO dto;

    @BeforeEach
    void setUp() {
        dto = new ReservaLibroDTO();
        dto.setUsuarioId(USUARIO_ID);
        dto.setLibroId(LIBRO_ID);
        dto.setSedeId(SEDE_ID);
    }

    private void referenciasValidas() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(true);
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
    }

    private ReservaLibroDTO reservaExistente(String estado) {
        ReservaLibroDTO r = new ReservaLibroDTO();
        r.setId(RESERVA_ID);
        r.setUsuarioId(USUARIO_ID);
        r.setLibroId(LIBRO_ID);
        r.setSedeId(SEDE_ID);
        r.setEstado(estado);
        return r;
    }

    private LibroDTO libro(String titulo) {
        LibroDTO l = new LibroDTO();
        l.setId(LIBRO_ID);
        l.setTitulo(titulo);
        return l;
    }

    // ---------------------------------------------------------------- crearReservaLibro

    @Test
    @DisplayName("crearReservaLibro: sin estado queda EN_ESPERA y se guarda")
    void crearReserva_valida_quedaEnEspera() {
        referenciasValidas();
        when(reservaLibroDAO.save(any(ReservaLibroDTO.class))).thenAnswer(inv -> {
            ReservaLibroDTO r = inv.getArgument(0);
            r.setId(RESERVA_ID);
            return r;
        });

        ReservaLibroDTO resultado = service.crearReservaLibro(dto);

        assertEquals(RESERVA_ID, resultado.getId());
        assertEquals("EN_ESPERA", resultado.getEstado());
    }

    @Test
    @DisplayName("crearReservaLibro: conserva el estado si ya viene informado")
    void crearReserva_conEstado_loConserva() {
        referenciasValidas();
        dto.setEstado("NOTIFICADA");
        when(reservaLibroDAO.save(any(ReservaLibroDTO.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("NOTIFICADA", service.crearReservaLibro(dto).getEstado());
    }

    @Test
    @DisplayName("crearReservaLibro: usuario con multas o sanciones propaga la excepcion y no sigue")
    void crearReserva_usuarioNoHabilitado_propagaExcepcion() {
        Mockito.doThrow(new BusinessValidationException("Usuario con multa pendiente"))
                .when(validadorUsuarioService).validarUsuarioHabilitadoParaOperaciones(USUARIO_ID);

        assertThrows(BusinessValidationException.class, () -> service.crearReservaLibro(dto));

        verifyNoInteractions(usuarioDAO, reservaLibroDAO);
    }

    @Test
    @DisplayName("crearReservaLibro: usuario inexistente lanza ResourceNotFoundException")
    void crearReserva_usuarioInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearReservaLibro(dto));

        verify(reservaLibroDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearReservaLibro: libro inexistente lanza ResourceNotFoundException")
    void crearReserva_libroInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearReservaLibro(dto));

        verify(reservaLibroDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearReservaLibro: sede inexistente lanza ResourceNotFoundException")
    void crearReserva_sedeInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(true);
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearReservaLibro(dto));

        verify(reservaLibroDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearReservaLibro: ejemplar informado e inexistente lanza ResourceNotFoundException")
    void crearReserva_ejemplarInexistente_lanzaNotFound() {
        referenciasValidas();
        dto.setEjemplarId(EJEMPLAR_ID);
        when(ejemplarDAO.existsById(EJEMPLAR_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearReservaLibro(dto));

        verify(reservaLibroDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearReservaLibro: reserva vigente para el mismo libro lanza BusinessValidationException")
    void crearReserva_yaTieneReservaVigente_lanzaBusinessValidation() {
        referenciasValidas();
        when(reservaLibroDAO.existsByUsuarioIdAndLibroIdAndEstadoIn(eq(USUARIO_ID), eq(LIBRO_ID), anyCollection()))
                .thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearReservaLibro(dto));

        verify(reservaLibroDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- consultas y eliminar

    @Test
    @DisplayName("obtenerReservaLibroPorId: devuelve la reserva cuando existe")
    void obtenerPorId_existente_laDevuelve() {
        ReservaLibroDTO existente = reservaExistente("EN_ESPERA");
        when(reservaLibroDAO.findById(RESERVA_ID)).thenReturn(Optional.of(existente));

        assertSame(existente, service.obtenerReservaLibroPorId(RESERVA_ID));
    }

    @Test
    @DisplayName("obtenerReservaLibroPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerPorId_inexistente_lanzaNotFound() {
        when(reservaLibroDAO.findById(RESERVA_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerReservaLibroPorId(RESERVA_ID));
    }

    @Test
    @DisplayName("obtenerTodasLasReservasLibro: delega en el DAO")
    void obtenerTodas_devuelveLista() {
        when(reservaLibroDAO.findAll()).thenReturn(List.of(reservaExistente("EN_ESPERA")));

        assertEquals(1, service.obtenerTodasLasReservasLibro().size());
    }

    @Test
    @DisplayName("eliminarReservaLibro: elimina cuando existe")
    void eliminar_existente_elimina() {
        when(reservaLibroDAO.findById(RESERVA_ID)).thenReturn(Optional.of(reservaExistente("EN_ESPERA")));
        when(reservaLibroDAO.deleteById(RESERVA_ID)).thenReturn(true);

        service.eliminarReservaLibro(RESERVA_ID);

        verify(reservaLibroDAO).deleteById(RESERVA_ID);
    }

    @Test
    @DisplayName("eliminarReservaLibro: inexistente lanza ResourceNotFoundException")
    void eliminar_inexistente_lanzaNotFound() {
        when(reservaLibroDAO.findById(RESERVA_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarReservaLibro(RESERVA_ID));

        verify(reservaLibroDAO, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminarReservaLibro: si el DAO no la elimina lanza ResourceNotFoundException")
    void eliminar_daoNoElimina_lanzaNotFound() {
        when(reservaLibroDAO.findById(RESERVA_ID)).thenReturn(Optional.of(reservaExistente("EN_ESPERA")));
        when(reservaLibroDAO.deleteById(RESERVA_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarReservaLibro(RESERVA_ID));
    }

    // ---------------------------------------------------------------- actualizarReservaLibro

    @Test
    @DisplayName("actualizarReservaLibro: reserva inexistente lanza ResourceNotFoundException")
    void actualizar_inexistente_lanzaNotFound() {
        when(reservaLibroDAO.findById(RESERVA_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarReservaLibro(RESERVA_ID, dto));
    }

    @Test
    @DisplayName("actualizarReservaLibro: no permite cambiar el usuario")
    void actualizar_cambiaUsuario_lanzaBusinessValidation() {
        when(reservaLibroDAO.findById(RESERVA_ID)).thenReturn(Optional.of(reservaExistente("EN_ESPERA")));
        dto.setUsuarioId(99L);

        assertThrows(BusinessValidationException.class, () -> service.actualizarReservaLibro(RESERVA_ID, dto));

        verify(reservaLibroDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarReservaLibro: no permite cambiar el libro")
    void actualizar_cambiaLibro_lanzaBusinessValidation() {
        when(reservaLibroDAO.findById(RESERVA_ID)).thenReturn(Optional.of(reservaExistente("EN_ESPERA")));
        dto.setLibroId(99L);

        assertThrows(BusinessValidationException.class, () -> service.actualizarReservaLibro(RESERVA_ID, dto));

        verify(reservaLibroDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarReservaLibro: referencia inexistente lanza ResourceNotFoundException")
    void actualizar_referenciaInexistente_lanzaNotFound() {
        when(reservaLibroDAO.findById(RESERVA_ID)).thenReturn(Optional.of(reservaExistente("EN_ESPERA")));
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarReservaLibro(RESERVA_ID, dto));
    }

    @Test
    @DisplayName("actualizarReservaLibro: al cancelar registra la fechaCancelacion")
    void actualizar_cancelar_registraFechaCancelacion() {
        when(reservaLibroDAO.findById(RESERVA_ID)).thenReturn(Optional.of(reservaExistente("EN_ESPERA")));
        referenciasValidas();
        when(reservaLibroDAO.update(eq(RESERVA_ID), any(ReservaLibroDTO.class)))
                .thenAnswer(inv -> Optional.of(inv.getArgument(1)));
        dto.setEstado("CANCELADA");

        ReservaLibroDTO resultado = service.actualizarReservaLibro(RESERVA_ID, dto);

        assertNotNull(resultado.getFechaCancelacion());
    }

    @Test
    @DisplayName("actualizarReservaLibro: no permite cancelar una reserva que ya estaba CANCELADA")
    void actualizar_yaCancelada_lanzaBusinessValidation() {
        when(reservaLibroDAO.findById(RESERVA_ID)).thenReturn(Optional.of(reservaExistente("CANCELADA")));
        referenciasValidas();
        dto.setEstado("CANCELADA");

        assertThrows(BusinessValidationException.class, () -> service.actualizarReservaLibro(RESERVA_ID, dto));

        verify(reservaLibroDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarReservaLibro: si el DAO no devuelve la reserva lanza ResourceNotFoundException")
    void actualizar_daoNoActualiza_lanzaNotFound() {
        when(reservaLibroDAO.findById(RESERVA_ID)).thenReturn(Optional.of(reservaExistente("EN_ESPERA")));
        referenciasValidas();
        when(reservaLibroDAO.update(eq(RESERVA_ID), any(ReservaLibroDTO.class))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarReservaLibro(RESERVA_ID, dto));
    }

    // ---------------------------------------------------------------- avisarDevolucionEnCamino

    @Test
    @DisplayName("avisarDevolucionEnCamino: notifica al primero en espera con el titulo y la sede")
    void avisarDevolucion_conReservaEnEspera_notifica() {
        when(reservaLibroDAO.findFirstByLibroIdAndSedeIdAndEstadoIgnoreCaseOrderByFechaReservaAsc(
                LIBRO_ID, SEDE_ID, "EN_ESPERA")).thenReturn(Optional.of(reservaExistente("EN_ESPERA")));
        when(libroDAO.findById(LIBRO_ID)).thenReturn(Optional.of(libro("Dune")));

        service.avisarDevolucionEnCamino(LIBRO_ID, SEDE_ID, "Sede Centro");

        ArgumentCaptor<NotificacionDTO> captor = ArgumentCaptor.forClass(NotificacionDTO.class);
        verify(notificacionDAO).save(captor.capture());
        NotificacionDTO n = captor.getValue();
        assertEquals("LIBRO_PRONTO_DISPONIBLE", n.getTipo());
        assertEquals(USUARIO_ID, n.getUsuarioId());
        assertFalse(n.getLeida());
        assertTrue(n.getMensaje().contains("Dune"));
        assertTrue(n.getMensaje().contains("Sede Centro"));
    }

    @Test
    @DisplayName("avisarDevolucionEnCamino: si no hay reservas en espera no notifica a nadie")
    void avisarDevolucion_sinReservas_noNotifica() {
        when(reservaLibroDAO.findFirstByLibroIdAndSedeIdAndEstadoIgnoreCaseOrderByFechaReservaAsc(
                LIBRO_ID, SEDE_ID, "EN_ESPERA")).thenReturn(Optional.empty());

        service.avisarDevolucionEnCamino(LIBRO_ID, SEDE_ID, "Sede Centro");

        verifyNoInteractions(notificacionDAO);
    }

    @Test
    @DisplayName("avisarDevolucionEnCamino: si el libro no se encuentra usa un titulo de respaldo")
    void avisarDevolucion_libroNoEncontrado_usaTituloDeRespaldo() {
        when(reservaLibroDAO.findFirstByLibroIdAndSedeIdAndEstadoIgnoreCaseOrderByFechaReservaAsc(
                LIBRO_ID, SEDE_ID, "EN_ESPERA")).thenReturn(Optional.of(reservaExistente("EN_ESPERA")));
        when(libroDAO.findById(LIBRO_ID)).thenReturn(Optional.empty());

        service.avisarDevolucionEnCamino(LIBRO_ID, SEDE_ID, "Sede Centro");

        ArgumentCaptor<NotificacionDTO> captor = ArgumentCaptor.forClass(NotificacionDTO.class);
        verify(notificacionDAO).save(captor.capture());
        assertTrue(captor.getValue().getMensaje().contains("(libro " + LIBRO_ID + ")"));
    }

    // ---------------------------------------------------------------- asignarEjemplarAReservaEnEspera

    private EjemplarDTO ejemplar() {
        EjemplarDTO e = new EjemplarDTO();
        e.setId(EJEMPLAR_ID);
        e.setLibroId(LIBRO_ID);
        e.setSedeId(SEDE_ID);
        e.setEstado("DISPONIBLE");
        return e;
    }

    @Test
    @DisplayName("asignarEjemplarAReservaEnEspera: ejemplar inexistente lanza ResourceNotFoundException")
    void asignar_ejemplarInexistente_lanzaNotFound() {
        when(ejemplarDAO.findById(EJEMPLAR_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.asignarEjemplarAReservaEnEspera(EJEMPLAR_ID));
    }

    @Test
    @DisplayName("asignarEjemplarAReservaEnEspera: sin reservas en espera devuelve false y no aparta el ejemplar")
    void asignar_sinReservas_devuelveFalse() {
        when(ejemplarDAO.findById(EJEMPLAR_ID)).thenReturn(Optional.of(ejemplar()));
        when(reservaLibroDAO.findFirstByLibroIdAndSedeIdAndEstadoIgnoreCaseOrderByFechaReservaAsc(
                LIBRO_ID, SEDE_ID, "EN_ESPERA")).thenReturn(Optional.empty());

        assertFalse(service.asignarEjemplarAReservaEnEspera(EJEMPLAR_ID));

        verify(ejemplarDAO, never()).cambiarEstadoSiEs(any(), any(), any());
    }

    @Test
    @DisplayName("asignarEjemplarAReservaEnEspera: si otro tomo el ejemplar devuelve false y no actualiza la reserva")
    void asignar_ejemplarYaTomado_devuelveFalse() {
        when(ejemplarDAO.findById(EJEMPLAR_ID)).thenReturn(Optional.of(ejemplar()));
        when(reservaLibroDAO.findFirstByLibroIdAndSedeIdAndEstadoIgnoreCaseOrderByFechaReservaAsc(
                LIBRO_ID, SEDE_ID, "EN_ESPERA")).thenReturn(Optional.of(reservaExistente("EN_ESPERA")));
        when(ejemplarDAO.cambiarEstadoSiEs(EJEMPLAR_ID, "DISPONIBLE", "RESERVADO")).thenReturn(0);

        assertFalse(service.asignarEjemplarAReservaEnEspera(EJEMPLAR_ID));

        verify(reservaLibroDAO, never()).update(any(), any());
        verifyNoInteractions(notificacionDAO);
    }

    @Test
    @DisplayName("asignarEjemplarAReservaEnEspera: asigna el ejemplar, marca NOTIFICADA con el plazo configurado (48 h) y notifica")
    void asignar_exitoso_notificaYActualizaReserva() {
        ReservaLibroDTO reserva = reservaExistente("EN_ESPERA");
        when(ejemplarDAO.findById(EJEMPLAR_ID)).thenReturn(Optional.of(ejemplar()));
        when(reservaLibroDAO.findFirstByLibroIdAndSedeIdAndEstadoIgnoreCaseOrderByFechaReservaAsc(
                LIBRO_ID, SEDE_ID, "EN_ESPERA")).thenReturn(Optional.of(reserva));
        when(ejemplarDAO.cambiarEstadoSiEs(EJEMPLAR_ID, "DISPONIBLE", "RESERVADO")).thenReturn(1);
        when(libroDAO.findById(LIBRO_ID)).thenReturn(Optional.of(libro("Dune")));
        when(configuracionService.plazo(ClavePlazo.HORAS_RETIRO_LIBRO)).thenReturn(48);

        boolean asignado = service.asignarEjemplarAReservaEnEspera(EJEMPLAR_ID);

        assertTrue(asignado);
        assertEquals("NOTIFICADA", reserva.getEstado());
        assertEquals(EJEMPLAR_ID, reserva.getEjemplarId());
        assertNotNull(reserva.getFechaNotificacion());
        assertEquals(reserva.getFechaNotificacion().plusHours(48), reserva.getFechaLimiteRetiro());
        verify(reservaLibroDAO).update(RESERVA_ID, reserva);

        ArgumentCaptor<NotificacionDTO> captor = ArgumentCaptor.forClass(NotificacionDTO.class);
        verify(notificacionDAO).save(captor.capture());
        assertEquals("LIBRO_DISPONIBLE", captor.getValue().getTipo());
        assertEquals(USUARIO_ID, captor.getValue().getUsuarioId());
    }
}