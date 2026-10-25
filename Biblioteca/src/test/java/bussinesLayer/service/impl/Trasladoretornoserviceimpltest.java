package bussinesLayer.service.impl;

import bussinesLayer.dto.TrasladoRetornoDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.ReservaLibroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.EjemplarDAO;
import persistanceLayer.dao.SedeDAO;
import persistanceLayer.dao.TrasladoRetornoDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrasladoRetornoServiceImpl")
class TrasladoRetornoServiceImplTest {

    private static final Long TRASLADO_ID = 1L;
    private static final Long SEDE_ID = 7L;
    private static final Long BIBLIOTECARIO_ID = 30L;
    private static final Long EJEMPLAR_ID = 50L;

    @Mock private TrasladoRetornoDAO trasladoDAO;
    @Mock private UsuarioDAO usuarioDAO;
    @Mock private SedeDAO sedeDAO;
    @Mock private EjemplarDAO ejemplarDAO;
    @Mock private ReservaLibroService reservaLibroService;

    @InjectMocks
    private TrasladoRetornoServiceImpl service;

    private TrasladoRetornoDTO traslado;

    @BeforeEach
    void setUp() {
        traslado = new TrasladoRetornoDTO();
        traslado.setId(TRASLADO_ID);
        traslado.setEstado("EN_TRANSITO");
        traslado.setEjemplarId(EJEMPLAR_ID);
        traslado.setSedeDestinoId(SEDE_ID);
    }

    // ---------------------------------------------------------------- obtenerTrasladoPorId

    @Test
    @DisplayName("obtenerTrasladoPorId: devuelve el traslado cuando existe")
    void obtenerTrasladoPorId_existente_loDevuelve() {
        when(trasladoDAO.findById(TRASLADO_ID)).thenReturn(Optional.of(traslado));

        assertSame(traslado, service.obtenerTrasladoPorId(TRASLADO_ID));
    }

    @Test
    @DisplayName("obtenerTrasladoPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerTrasladoPorId_inexistente_lanzaNotFound() {
        when(trasladoDAO.findById(TRASLADO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerTrasladoPorId(TRASLADO_ID));
    }

    // ---------------------------------------------------------------- obtenerTrasladosHaciaSede

    @Test
    @DisplayName("obtenerTrasladosHaciaSede: sede inexistente lanza ResourceNotFoundException")
    void obtenerTrasladosHaciaSede_sedeInexistente_lanzaNotFound() {
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerTrasladosHaciaSede(SEDE_ID, null));
    }

    @Test
    @DisplayName("obtenerTrasladosHaciaSede: sin estado devuelve todos los traslados de la sede")
    void obtenerTrasladosHaciaSede_sinEstado_devuelveTodos() {
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(trasladoDAO.findBySedeDestinoIdOrderByFechaCreacionAsc(SEDE_ID)).thenReturn(List.of(traslado));

        List<TrasladoRetornoDTO> resultado = service.obtenerTrasladosHaciaSede(SEDE_ID, null);

        assertEquals(1, resultado.size());
        verify(trasladoDAO, never())
                .findBySedeDestinoIdAndEstadoIgnoreCaseOrderByFechaCreacionAsc(any(), anyString());
    }

    @Test
    @DisplayName("obtenerTrasladosHaciaSede: estado en blanco devuelve todos los traslados de la sede")
    void obtenerTrasladosHaciaSede_estadoEnBlanco_devuelveTodos() {
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(trasladoDAO.findBySedeDestinoIdOrderByFechaCreacionAsc(SEDE_ID)).thenReturn(List.of(traslado));

        assertEquals(1, service.obtenerTrasladosHaciaSede(SEDE_ID, "  ").size());
    }

    @Test
    @DisplayName("obtenerTrasladosHaciaSede: estado valido en minusculas se normaliza a mayusculas")
    void obtenerTrasladosHaciaSede_estadoValido_filtraNormalizado() {
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(trasladoDAO.findBySedeDestinoIdAndEstadoIgnoreCaseOrderByFechaCreacionAsc(SEDE_ID, "EN_TRANSITO"))
                .thenReturn(List.of(traslado));

        List<TrasladoRetornoDTO> resultado = service.obtenerTrasladosHaciaSede(SEDE_ID, " en_transito ");

        assertEquals(1, resultado.size());
        verify(trasladoDAO, never()).findBySedeDestinoIdOrderByFechaCreacionAsc(any());
    }

    @Test
    @DisplayName("obtenerTrasladosHaciaSede: estado RECIBIDO es valido")
    void obtenerTrasladosHaciaSede_estadoRecibido_filtra() {
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(trasladoDAO.findBySedeDestinoIdAndEstadoIgnoreCaseOrderByFechaCreacionAsc(SEDE_ID, "RECIBIDO"))
                .thenReturn(List.of());

        assertEquals(0, service.obtenerTrasladosHaciaSede(SEDE_ID, "Recibido").size());
    }

    @Test
    @DisplayName("obtenerTrasladosHaciaSede: estado invalido lanza BusinessValidationException")
    void obtenerTrasladosHaciaSede_estadoInvalido_lanzaBusinessValidation() {
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.obtenerTrasladosHaciaSede(SEDE_ID, "PERDIDO"));
    }

    // ---------------------------------------------------------------- recibirTraslado

    @Test
    @DisplayName("recibirTraslado: bibliotecario inexistente lanza ResourceNotFoundException")
    void recibirTraslado_bibliotecarioInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(BIBLIOTECARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.recibirTraslado(TRASLADO_ID, BIBLIOTECARIO_ID));

        verify(trasladoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("recibirTraslado: traslado inexistente lanza ResourceNotFoundException")
    void recibirTraslado_trasladoInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(BIBLIOTECARIO_ID)).thenReturn(true);
        when(trasladoDAO.findById(TRASLADO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.recibirTraslado(TRASLADO_ID, BIBLIOTECARIO_ID));

        verify(trasladoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("recibirTraslado: traslado que no esta EN_TRANSITO lanza BusinessValidationException")
    void recibirTraslado_noEnTransito_lanzaBusinessValidation() {
        traslado.setEstado("RECIBIDO");
        when(usuarioDAO.existsById(BIBLIOTECARIO_ID)).thenReturn(true);
        when(trasladoDAO.findById(TRASLADO_ID)).thenReturn(Optional.of(traslado));

        assertThrows(BusinessValidationException.class, () -> service.recibirTraslado(TRASLADO_ID, BIBLIOTECARIO_ID));

        verify(ejemplarDAO, never()).cambiarEstadoSiEs(any(), anyString(), anyString());
        verify(trasladoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("recibirTraslado: si el ejemplar ya no esta en transito lanza BusinessValidationException")
    void recibirTraslado_ejemploNoEnTransito_lanzaBusinessValidation() {
        when(usuarioDAO.existsById(BIBLIOTECARIO_ID)).thenReturn(true);
        when(trasladoDAO.findById(TRASLADO_ID)).thenReturn(Optional.of(traslado));
        when(ejemplarDAO.cambiarEstadoSiEs(EJEMPLAR_ID, "EN_TRANSITO", "DISPONIBLE")).thenReturn(0);

        assertThrows(BusinessValidationException.class, () -> service.recibirTraslado(TRASLADO_ID, BIBLIOTECARIO_ID));

        verify(trasladoDAO, never()).update(any(), any());
        verify(reservaLibroService, never()).asignarEjemplarAReservaEnEspera(any());
    }

    @Test
    @DisplayName("recibirTraslado: marca el traslado RECIBIDO, libera el ejemplar y avisa a las reservas en espera")
    void recibirTraslado_valido_recibeYAsignaReserva() {
        when(usuarioDAO.existsById(BIBLIOTECARIO_ID)).thenReturn(true);
        when(trasladoDAO.findById(TRASLADO_ID)).thenReturn(Optional.of(traslado));
        when(ejemplarDAO.cambiarEstadoSiEs(EJEMPLAR_ID, "EN_TRANSITO", "DISPONIBLE")).thenReturn(1);
        when(trasladoDAO.update(TRASLADO_ID, traslado)).thenReturn(Optional.of(traslado));

        TrasladoRetornoDTO resultado = service.recibirTraslado(TRASLADO_ID, BIBLIOTECARIO_ID);

        assertEquals("RECIBIDO", resultado.getEstado());
        assertEquals(BIBLIOTECARIO_ID, resultado.getBibliotecarioId());
        assertNotNull(resultado.getFechaRecepcion());
        verify(reservaLibroService).asignarEjemplarAReservaEnEspera(EJEMPLAR_ID);
    }

    @Test
    @DisplayName("recibirTraslado: acepta el estado EN_TRANSITO sin importar mayusculas")
    void recibirTraslado_estadoEnMinusculas_recibe() {
        traslado.setEstado("en_transito");
        when(usuarioDAO.existsById(BIBLIOTECARIO_ID)).thenReturn(true);
        when(trasladoDAO.findById(TRASLADO_ID)).thenReturn(Optional.of(traslado));
        when(ejemplarDAO.cambiarEstadoSiEs(EJEMPLAR_ID, "EN_TRANSITO", "DISPONIBLE")).thenReturn(1);
        when(trasladoDAO.update(TRASLADO_ID, traslado)).thenReturn(Optional.of(traslado));

        assertEquals("RECIBIDO", service.recibirTraslado(TRASLADO_ID, BIBLIOTECARIO_ID).getEstado());
    }

    @Test
    @DisplayName("recibirTraslado: si el DAO no devuelve el traslado actualizado lanza ResourceNotFoundException")
    void recibirTraslado_daoNoActualiza_lanzaNotFound() {
        when(usuarioDAO.existsById(BIBLIOTECARIO_ID)).thenReturn(true);
        when(trasladoDAO.findById(TRASLADO_ID)).thenReturn(Optional.of(traslado));
        when(ejemplarDAO.cambiarEstadoSiEs(EJEMPLAR_ID, "EN_TRANSITO", "DISPONIBLE")).thenReturn(1);
        when(trasladoDAO.update(TRASLADO_ID, traslado)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.recibirTraslado(TRASLADO_ID, BIBLIOTECARIO_ID));

        verify(reservaLibroService, never()).asignarEjemplarAReservaEnEspera(any());
    }
}
