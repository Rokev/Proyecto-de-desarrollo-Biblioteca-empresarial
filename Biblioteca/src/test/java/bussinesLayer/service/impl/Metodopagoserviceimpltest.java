package bussinesLayer.service.impl;

import bussinesLayer.dto.MetodoPagoDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.MetodoPagoDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MetodoPagoServiceImpl")
class MetodoPagoServiceImplTest {

    private static final Long METODO_ID = 1L;
    private static final Long USUARIO_ID = 10L;
    private static final Long OTRO_USUARIO_ID = 11L;

    @Mock private MetodoPagoDAO metodoPagoDAO;
    @Mock private UsuarioDAO usuarioDAO;

    @InjectMocks
    private MetodoPagoServiceImpl service;

    private MetodoPagoDTO dto;

    @BeforeEach
    void setUp() {
        dto = new MetodoPagoDTO();
        dto.setTipo("TARJETA_CREDITO");
        dto.setMarca("VISA");
        dto.setUltimosCuatroDigitos("4242");
        dto.setTitular("Maria Perez");
        dto.setUsuarioId(USUARIO_ID);
    }

    private MetodoPagoDTO metodoExistente(Boolean predeterminado) {
        MetodoPagoDTO m = new MetodoPagoDTO();
        m.setId(METODO_ID);
        m.setUsuarioId(USUARIO_ID);
        m.setPredeterminado(predeterminado);
        return m;
    }

    // ---------------------------------------------------------------- crearMetodoPago

    @Test
    @DisplayName("crearMetodoPago: usuario inexistente lanza ResourceNotFoundException")
    void crearMetodoPago_usuarioInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearMetodoPago(dto));

        verify(metodoPagoDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearMetodoPago: el primer metodo del usuario queda como predeterminado")
    void crearMetodoPago_primerMetodo_quedaPredeterminado() {
        dto.setPredeterminado(false);
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(metodoPagoDAO.countByUsuarioId(USUARIO_ID)).thenReturn(0L);
        when(metodoPagoDAO.save(dto)).thenAnswer(inv -> {
            MetodoPagoDTO m = inv.getArgument(0);
            m.setId(METODO_ID);
            return m;
        });

        MetodoPagoDTO resultado = service.crearMetodoPago(dto);

        assertEquals(METODO_ID, resultado.getId());
        assertTrue(resultado.getPredeterminado());
        verify(metodoPagoDAO, never()).quitarPredeterminado(any());
    }

    @Test
    @DisplayName("crearMetodoPago: no primero y predeterminado=true quita el predeterminado anterior")
    void crearMetodoPago_nuevoPredeterminado_quitaAnterior() {
        dto.setPredeterminado(true);
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(metodoPagoDAO.countByUsuarioId(USUARIO_ID)).thenReturn(2L);
        when(metodoPagoDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        MetodoPagoDTO resultado = service.crearMetodoPago(dto);

        assertTrue(resultado.getPredeterminado());
        verify(metodoPagoDAO).quitarPredeterminado(USUARIO_ID);
    }

    @Test
    @DisplayName("crearMetodoPago: no primero y predeterminado vacio queda en false")
    void crearMetodoPago_noPrimeroSinPredeterminado_quedaFalse() {
        dto.setPredeterminado(null);
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(metodoPagoDAO.countByUsuarioId(USUARIO_ID)).thenReturn(1L);
        when(metodoPagoDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        MetodoPagoDTO resultado = service.crearMetodoPago(dto);

        assertFalse(resultado.getPredeterminado());
        verify(metodoPagoDAO, never()).quitarPredeterminado(any());
    }

    @Test
    @DisplayName("crearMetodoPago: no primero y predeterminado=false no toca el predeterminado actual")
    void crearMetodoPago_noPrimeroPredeterminadoFalse_noQuitaAnterior() {
        dto.setPredeterminado(false);
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(metodoPagoDAO.countByUsuarioId(USUARIO_ID)).thenReturn(1L);
        when(metodoPagoDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        MetodoPagoDTO resultado = service.crearMetodoPago(dto);

        assertFalse(resultado.getPredeterminado());
        verify(metodoPagoDAO, never()).quitarPredeterminado(any());
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerMetodoPagoPorId: devuelve el metodo cuando existe")
    void obtenerMetodoPagoPorId_existente_loDevuelve() {
        MetodoPagoDTO existente = metodoExistente(true);
        when(metodoPagoDAO.findById(METODO_ID)).thenReturn(Optional.of(existente));

        assertSame(existente, service.obtenerMetodoPagoPorId(METODO_ID));
    }

    @Test
    @DisplayName("obtenerMetodoPagoPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerMetodoPagoPorId_inexistente_lanzaNotFound() {
        when(metodoPagoDAO.findById(METODO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerMetodoPagoPorId(METODO_ID));
    }

    @Test
    @DisplayName("obtenerTodosLosMetodosPago: delega en el DAO")
    void obtenerTodosLosMetodosPago_devuelveLista() {
        when(metodoPagoDAO.findAll()).thenReturn(List.of(metodoExistente(true), metodoExistente(false)));

        assertEquals(2, service.obtenerTodosLosMetodosPago().size());
    }

    // ---------------------------------------------------------------- actualizarMetodoPago

    @Test
    @DisplayName("actualizarMetodoPago: metodo inexistente lanza ResourceNotFoundException")
    void actualizarMetodoPago_inexistente_lanzaNotFound() {
        when(metodoPagoDAO.findById(METODO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarMetodoPago(METODO_ID, dto));

        verify(metodoPagoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarMetodoPago: cambiar el usuario propietario lanza BusinessValidationException")
    void actualizarMetodoPago_cambiaUsuario_lanzaBusinessValidation() {
        dto.setUsuarioId(OTRO_USUARIO_ID);
        when(metodoPagoDAO.findById(METODO_ID)).thenReturn(Optional.of(metodoExistente(false)));

        assertThrows(BusinessValidationException.class, () -> service.actualizarMetodoPago(METODO_ID, dto));

        verify(metodoPagoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarMetodoPago: marcar como predeterminado uno que no lo era quita el anterior")
    void actualizarMetodoPago_pasaAPredeterminado_quitaAnterior() {
        dto.setPredeterminado(true);
        when(metodoPagoDAO.findById(METODO_ID)).thenReturn(Optional.of(metodoExistente(false)));
        when(metodoPagoDAO.update(METODO_ID, dto)).thenReturn(Optional.of(dto));

        assertSame(dto, service.actualizarMetodoPago(METODO_ID, dto));

        verify(metodoPagoDAO).quitarPredeterminado(USUARIO_ID);
    }

    @Test
    @DisplayName("actualizarMetodoPago: seguir siendo predeterminado no quita nada")
    void actualizarMetodoPago_siguePredeterminado_noQuitaAnterior() {
        dto.setPredeterminado(true);
        when(metodoPagoDAO.findById(METODO_ID)).thenReturn(Optional.of(metodoExistente(true)));
        when(metodoPagoDAO.update(METODO_ID, dto)).thenReturn(Optional.of(dto));

        assertSame(dto, service.actualizarMetodoPago(METODO_ID, dto));

        verify(metodoPagoDAO, never()).quitarPredeterminado(any());
    }

    @Test
    @DisplayName("actualizarMetodoPago: quitar el predeterminado actual lanza BusinessValidationException")
    void actualizarMetodoPago_quitaPredeterminadoActual_lanzaBusinessValidation() {
        dto.setPredeterminado(false);
        when(metodoPagoDAO.findById(METODO_ID)).thenReturn(Optional.of(metodoExistente(true)));

        assertThrows(BusinessValidationException.class, () -> service.actualizarMetodoPago(METODO_ID, dto));

        verify(metodoPagoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarMetodoPago: sin indicar predeterminado actualiza sin tocar el predeterminado")
    void actualizarMetodoPago_sinPredeterminado_actualiza() {
        dto.setPredeterminado(null);
        when(metodoPagoDAO.findById(METODO_ID)).thenReturn(Optional.of(metodoExistente(true)));
        when(metodoPagoDAO.update(METODO_ID, dto)).thenReturn(Optional.of(dto));

        assertSame(dto, service.actualizarMetodoPago(METODO_ID, dto));

        verify(metodoPagoDAO, never()).quitarPredeterminado(any());
    }

    @Test
    @DisplayName("actualizarMetodoPago: si el DAO no devuelve el metodo actualizado lanza ResourceNotFoundException")
    void actualizarMetodoPago_daoNoActualiza_lanzaNotFound() {
        dto.setPredeterminado(false);
        when(metodoPagoDAO.findById(METODO_ID)).thenReturn(Optional.of(metodoExistente(false)));
        when(metodoPagoDAO.update(METODO_ID, dto)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarMetodoPago(METODO_ID, dto));
    }

    // ---------------------------------------------------------------- eliminarMetodoPago

    @Test
    @DisplayName("eliminarMetodoPago: elimina cuando el metodo existe")
    void eliminarMetodoPago_existente_elimina() {
        when(metodoPagoDAO.deleteById(METODO_ID)).thenReturn(true);

        service.eliminarMetodoPago(METODO_ID);

        verify(metodoPagoDAO).deleteById(METODO_ID);
    }

    @Test
    @DisplayName("eliminarMetodoPago: metodo inexistente lanza ResourceNotFoundException")
    void eliminarMetodoPago_inexistente_lanzaNotFound() {
        when(metodoPagoDAO.deleteById(METODO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarMetodoPago(METODO_ID));
    }
}
