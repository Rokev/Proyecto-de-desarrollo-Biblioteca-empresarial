package bussinesLayer.service.impl;

import bussinesLayer.dto.EjemplarDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.EjemplarDAO;
import persistanceLayer.dao.LibroDAO;
import persistanceLayer.dao.SedeDAO;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EjemplarServiceImpl")
class EjemplarServiceImplTest {

    private static final Long EJEMPLAR_ID = 1L;
    private static final Long OTRO_ID = 2L;
    private static final Long LIBRO_ID = 5L;
    private static final Long SEDE_ID = 7L;
    private static final String CODIGO = "BC-0001";

    @Mock private EjemplarDAO ejemplarDAO;
    @Mock private LibroDAO libroDAO;
    @Mock private SedeDAO sedeDAO;

    @InjectMocks
    private EjemplarServiceImpl service;

    private EjemplarDTO dto;

    @BeforeEach
    void setUp() {
        dto = new EjemplarDTO();
        dto.setCodigoBarras(CODIGO);
        dto.setLibroId(LIBRO_ID);
        dto.setSedeId(SEDE_ID);
    }

    private EjemplarDTO ejemplarConId(Long id) {
        EjemplarDTO e = new EjemplarDTO();
        e.setId(id);
        return e;
    }

    // ---------------------------------------------------------------- crearEjemplar

    @Test
    @DisplayName("crearEjemplar: sin estado asigna DISPONIBLE")
    void crearEjemplar_sinEstado_asignaDisponible() {
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(true);
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(ejemplarDAO.save(dto)).thenAnswer(inv -> {
            EjemplarDTO e = inv.getArgument(0);
            e.setId(EJEMPLAR_ID);
            return e;
        });

        EjemplarDTO resultado = service.crearEjemplar(dto);

        assertEquals(EJEMPLAR_ID, resultado.getId());
        assertEquals("DISPONIBLE", resultado.getEstado());
    }

    @Test
    @DisplayName("crearEjemplar: estado en blanco asigna DISPONIBLE")
    void crearEjemplar_estadoEnBlanco_asignaDisponible() {
        dto.setEstado("   ");
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(true);
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(ejemplarDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("DISPONIBLE", service.crearEjemplar(dto).getEstado());
    }

    @Test
    @DisplayName("crearEjemplar: conserva el estado si ya viene informado")
    void crearEjemplar_conEstado_loConserva() {
        dto.setEstado("MANTENIMIENTO");
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(true);
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(ejemplarDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("MANTENIMIENTO", service.crearEjemplar(dto).getEstado());
    }

    @Test
    @DisplayName("crearEjemplar: codigo de barras duplicado lanza BusinessValidationException")
    void crearEjemplar_codigoDuplicado_lanzaBusinessValidation() {
        when(ejemplarDAO.existsByCodigoBarras(CODIGO)).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearEjemplar(dto));

        verify(ejemplarDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearEjemplar: libro inexistente lanza ResourceNotFoundException")
    void crearEjemplar_libroInexistente_lanzaNotFound() {
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearEjemplar(dto));

        verify(ejemplarDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearEjemplar: sede inexistente lanza ResourceNotFoundException")
    void crearEjemplar_sedeInexistente_lanzaNotFound() {
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(true);
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearEjemplar(dto));

        verify(ejemplarDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerEjemplarPorId: devuelve el ejemplar cuando existe")
    void obtenerEjemplarPorId_existente_loDevuelve() {
        EjemplarDTO existente = ejemplarConId(EJEMPLAR_ID);
        when(ejemplarDAO.findById(EJEMPLAR_ID)).thenReturn(Optional.of(existente));

        assertSame(existente, service.obtenerEjemplarPorId(EJEMPLAR_ID));
    }

    @Test
    @DisplayName("obtenerEjemplarPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerEjemplarPorId_inexistente_lanzaNotFound() {
        when(ejemplarDAO.findById(EJEMPLAR_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerEjemplarPorId(EJEMPLAR_ID));
    }

    @Test
    @DisplayName("obtenerTodosLosEjemplares: delega en el DAO")
    void obtenerTodosLosEjemplares_devuelveLista() {
        when(ejemplarDAO.findAll()).thenReturn(List.of(ejemplarConId(1L), ejemplarConId(2L)));

        assertEquals(2, service.obtenerTodosLosEjemplares().size());
    }

    // ---------------------------------------------------------------- actualizarEjemplar

    @Test
    @DisplayName("actualizarEjemplar: ejemplar inexistente lanza ResourceNotFoundException")
    void actualizarEjemplar_inexistente_lanzaNotFound() {
        when(ejemplarDAO.existsById(EJEMPLAR_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarEjemplar(EJEMPLAR_ID, dto));

        verify(ejemplarDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarEjemplar: codigo propio o libre actualiza sin conflicto")
    void actualizarEjemplar_valido_actualiza() {
        when(ejemplarDAO.existsById(EJEMPLAR_ID)).thenReturn(true);
        // El mismo ejemplar aparece como "dueño" del codigo: no es conflicto.
        when(ejemplarDAO.findByCodigoBarras(CODIGO)).thenReturn(Optional.of(ejemplarConId(EJEMPLAR_ID)));
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(true);
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(ejemplarDAO.update(EJEMPLAR_ID, dto)).thenReturn(Optional.of(dto));

        assertSame(dto, service.actualizarEjemplar(EJEMPLAR_ID, dto));
    }

    @Test
    @DisplayName("actualizarEjemplar: codigo de otro ejemplar lanza BusinessValidationException")
    void actualizarEjemplar_codigoDeOtro_lanzaBusinessValidation() {
        when(ejemplarDAO.existsById(EJEMPLAR_ID)).thenReturn(true);
        when(ejemplarDAO.findByCodigoBarras(CODIGO)).thenReturn(Optional.of(ejemplarConId(OTRO_ID)));

        assertThrows(BusinessValidationException.class, () -> service.actualizarEjemplar(EJEMPLAR_ID, dto));

        verify(ejemplarDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarEjemplar: libro inexistente lanza ResourceNotFoundException")
    void actualizarEjemplar_libroInexistente_lanzaNotFound() {
        when(ejemplarDAO.existsById(EJEMPLAR_ID)).thenReturn(true);
        when(ejemplarDAO.findByCodigoBarras(CODIGO)).thenReturn(Optional.empty());
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarEjemplar(EJEMPLAR_ID, dto));

        verify(ejemplarDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarEjemplar: sede inexistente lanza ResourceNotFoundException")
    void actualizarEjemplar_sedeInexistente_lanzaNotFound() {
        when(ejemplarDAO.existsById(EJEMPLAR_ID)).thenReturn(true);
        when(ejemplarDAO.findByCodigoBarras(CODIGO)).thenReturn(Optional.empty());
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(true);
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarEjemplar(EJEMPLAR_ID, dto));

        verify(ejemplarDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarEjemplar: si el DAO no devuelve el ejemplar actualizado lanza ResourceNotFoundException")
    void actualizarEjemplar_daoNoActualiza_lanzaNotFound() {
        when(ejemplarDAO.existsById(EJEMPLAR_ID)).thenReturn(true);
        when(ejemplarDAO.findByCodigoBarras(CODIGO)).thenReturn(Optional.empty());
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(true);
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(ejemplarDAO.update(EJEMPLAR_ID, dto)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarEjemplar(EJEMPLAR_ID, dto));
    }

    // ---------------------------------------------------------------- eliminarEjemplar

    @Test
    @DisplayName("eliminarEjemplar: elimina cuando el ejemplar existe")
    void eliminarEjemplar_existente_elimina() {
        when(ejemplarDAO.deleteById(EJEMPLAR_ID)).thenReturn(true);

        service.eliminarEjemplar(EJEMPLAR_ID);

        verify(ejemplarDAO).deleteById(EJEMPLAR_ID);
    }

    @Test
    @DisplayName("eliminarEjemplar: ejemplar inexistente lanza ResourceNotFoundException")
    void eliminarEjemplar_inexistente_lanzaNotFound() {
        when(ejemplarDAO.deleteById(EJEMPLAR_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarEjemplar(EJEMPLAR_ID));
    }
}
