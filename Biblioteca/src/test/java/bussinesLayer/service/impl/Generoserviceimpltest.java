package bussinesLayer.service.impl;

import bussinesLayer.dto.GeneroDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.GeneroDAO;

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
@DisplayName("GeneroServiceImpl")
class GeneroServiceImplTest {

    private static final Long GENERO_ID = 1L;
    private static final Long OTRO_ID = 2L;

    @Mock private GeneroDAO generoDAO;

    @InjectMocks
    private GeneroServiceImpl service;

    private GeneroDTO dto;

    @BeforeEach
    void setUp() {
        dto = new GeneroDTO();
        dto.setNombre("Novela");
        dto.setDescripcion("Narrativa extensa");
    }

    private GeneroDTO generoConId(Long id) {
        GeneroDTO g = new GeneroDTO();
        g.setId(id);
        return g;
    }

    // ---------------------------------------------------------------- crearGenero

    @Test
    @DisplayName("crearGenero: nombre libre guarda y devuelve el genero con ID")
    void crearGenero_valido_guarda() {
        when(generoDAO.existsByNombreIgnoreCase("Novela")).thenReturn(false);
        when(generoDAO.save(dto)).thenAnswer(inv -> {
            GeneroDTO g = inv.getArgument(0);
            g.setId(GENERO_ID);
            return g;
        });

        GeneroDTO resultado = service.crearGenero(dto);

        assertEquals(GENERO_ID, resultado.getId());
        verify(generoDAO).save(dto);
    }

    @Test
    @DisplayName("crearGenero: nombre duplicado lanza BusinessValidationException")
    void crearGenero_nombreDuplicado_lanzaBusinessValidation() {
        when(generoDAO.existsByNombreIgnoreCase("Novela")).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearGenero(dto));

        verify(generoDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerGeneroPorId: devuelve el genero cuando existe")
    void obtenerGeneroPorId_existente_loDevuelve() {
        GeneroDTO existente = generoConId(GENERO_ID);
        when(generoDAO.findById(GENERO_ID)).thenReturn(Optional.of(existente));

        assertSame(existente, service.obtenerGeneroPorId(GENERO_ID));
    }

    @Test
    @DisplayName("obtenerGeneroPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerGeneroPorId_inexistente_lanzaNotFound() {
        when(generoDAO.findById(GENERO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerGeneroPorId(GENERO_ID));
    }

    @Test
    @DisplayName("obtenerTodosLosGeneros: delega en el DAO")
    void obtenerTodosLosGeneros_devuelveLista() {
        when(generoDAO.findAll()).thenReturn(List.of(generoConId(1L), generoConId(2L)));

        assertEquals(2, service.obtenerTodosLosGeneros().size());
    }

    // ---------------------------------------------------------------- actualizarGenero

    @Test
    @DisplayName("actualizarGenero: genero inexistente lanza ResourceNotFoundException")
    void actualizarGenero_inexistente_lanzaNotFound() {
        when(generoDAO.existsById(GENERO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarGenero(GENERO_ID, dto));

        verify(generoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarGenero: nombre propio o libre actualiza sin conflicto")
    void actualizarGenero_valido_actualiza() {
        when(generoDAO.existsById(GENERO_ID)).thenReturn(true);
        // El mismo genero aparece como "dueño" del nombre: no es conflicto.
        when(generoDAO.findByNombreIgnoreCase("Novela")).thenReturn(Optional.of(generoConId(GENERO_ID)));
        when(generoDAO.update(GENERO_ID, dto)).thenReturn(Optional.of(dto));

        assertSame(dto, service.actualizarGenero(GENERO_ID, dto));
    }

    @Test
    @DisplayName("actualizarGenero: nombre de otro genero lanza BusinessValidationException")
    void actualizarGenero_nombreDeOtro_lanzaBusinessValidation() {
        when(generoDAO.existsById(GENERO_ID)).thenReturn(true);
        when(generoDAO.findByNombreIgnoreCase("Novela")).thenReturn(Optional.of(generoConId(OTRO_ID)));

        assertThrows(BusinessValidationException.class, () -> service.actualizarGenero(GENERO_ID, dto));

        verify(generoDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarGenero: si el DAO no devuelve el genero actualizado lanza ResourceNotFoundException")
    void actualizarGenero_daoNoActualiza_lanzaNotFound() {
        when(generoDAO.existsById(GENERO_ID)).thenReturn(true);
        when(generoDAO.findByNombreIgnoreCase("Novela")).thenReturn(Optional.empty());
        when(generoDAO.update(GENERO_ID, dto)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarGenero(GENERO_ID, dto));
    }

    // ---------------------------------------------------------------- eliminarGenero

    @Test
    @DisplayName("eliminarGenero: elimina cuando el genero existe")
    void eliminarGenero_existente_elimina() {
        when(generoDAO.deleteById(GENERO_ID)).thenReturn(true);

        service.eliminarGenero(GENERO_ID);

        verify(generoDAO).deleteById(GENERO_ID);
    }

    @Test
    @DisplayName("eliminarGenero: genero inexistente lanza ResourceNotFoundException")
    void eliminarGenero_inexistente_lanzaNotFound() {
        when(generoDAO.deleteById(GENERO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarGenero(GENERO_ID));
    }
}