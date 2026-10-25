package bussinesLayer.service.impl;

import bussinesLayer.dto.EjemplarDTO;
import bussinesLayer.dto.LibroDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.AutorDAO;
import persistanceLayer.dao.EjemplarDAO;
import persistanceLayer.dao.GeneroDAO;
import persistanceLayer.dao.LibroDAO;
import persistanceLayer.dao.PrestamoDAO;
import persistanceLayer.dao.ReservaLibroDAO;

import java.time.Year;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LibroServiceImpl")
class LibroServiceImplTest {

    private static final Long LIBRO_ID = 1L;
    private static final Long OTRO_ID = 2L;
    private static final Long GENERO_ID = 3L;
    private static final Long AUTOR_1 = 11L;
    private static final Long AUTOR_2 = 12L;
    private static final String ISBN = "978-3-16-148410-0";

    @Mock private LibroDAO libroDAO;
    @Mock private GeneroDAO generoDAO;
    @Mock private AutorDAO autorDAO;
    @Mock private EjemplarDAO ejemplarDAO;
    @Mock private PrestamoDAO prestamoDAO;
    @Mock private ReservaLibroDAO reservaLibroDAO;

    @InjectMocks
    private LibroServiceImpl service;

    private LibroDTO dto;

    @BeforeEach
    void setUp() {
        dto = new LibroDTO();
        dto.setIsbn(ISBN);
        dto.setTitulo("Cien anos de soledad");
        dto.setIdioma("Espanol");
        dto.setAnioPublicacion(1967);
        dto.setEditorial("Sudamericana");
        dto.setGeneroId(GENERO_ID);
        dto.setAutoresIds(List.of(AUTOR_1, AUTOR_2));
    }

    private LibroDTO libroConId(Long id) {
        LibroDTO l = new LibroDTO();
        l.setId(id);
        return l;
    }

    // ---------------------------------------------------------------- crearLibro

    @Test
    @DisplayName("crearLibro: con genero y autores existentes guarda el libro")
    void crearLibro_valido_guarda() {
        when(generoDAO.existsById(GENERO_ID)).thenReturn(true);
        when(autorDAO.existsById(AUTOR_1)).thenReturn(true);
        when(autorDAO.existsById(AUTOR_2)).thenReturn(true);
        when(libroDAO.save(dto)).thenAnswer(inv -> {
            LibroDTO l = inv.getArgument(0);
            l.setId(LIBRO_ID);
            return l;
        });

        LibroDTO resultado = service.crearLibro(dto);

        assertEquals(LIBRO_ID, resultado.getId());
        verify(libroDAO).save(dto);
    }

    @Test
    @DisplayName("crearLibro: sin autores guarda el libro")
    void crearLibro_sinAutores_guarda() {
        dto.setAutoresIds(null);
        when(generoDAO.existsById(GENERO_ID)).thenReturn(true);
        when(libroDAO.save(dto)).thenAnswer(inv -> inv.getArgument(0));

        assertSame(dto, service.crearLibro(dto));

        verify(autorDAO, never()).existsById(any());
    }

    @Test
    @DisplayName("crearLibro: ISBN duplicado lanza BusinessValidationException")
    void crearLibro_isbnDuplicado_lanzaBusinessValidation() {
        when(libroDAO.existsByIsbn(ISBN)).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearLibro(dto));

        verify(libroDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearLibro: anio de publicacion futuro lanza BusinessValidationException")
    void crearLibro_anioFuturo_lanzaBusinessValidation() {
        dto.setAnioPublicacion(Year.now().getValue() + 1);

        assertThrows(BusinessValidationException.class, () -> service.crearLibro(dto));

        verify(libroDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearLibro: genero inexistente lanza ResourceNotFoundException")
    void crearLibro_generoInexistente_lanzaNotFound() {
        when(generoDAO.existsById(GENERO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearLibro(dto));

        verify(libroDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearLibro: autor inexistente lanza ResourceNotFoundException")
    void crearLibro_autorInexistente_lanzaNotFound() {
        when(generoDAO.existsById(GENERO_ID)).thenReturn(true);
        when(autorDAO.existsById(AUTOR_1)).thenReturn(true);
        when(autorDAO.existsById(AUTOR_2)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearLibro(dto));

        verify(libroDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerLibroPorId: devuelve el libro cuando existe")
    void obtenerLibroPorId_existente_loDevuelve() {
        LibroDTO existente = libroConId(LIBRO_ID);
        when(libroDAO.findById(LIBRO_ID)).thenReturn(Optional.of(existente));

        assertSame(existente, service.obtenerLibroPorId(LIBRO_ID));
    }

    @Test
    @DisplayName("obtenerLibroPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerLibroPorId_inexistente_lanzaNotFound() {
        when(libroDAO.findById(LIBRO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerLibroPorId(LIBRO_ID));
    }

    @Test
    @DisplayName("obtenerTodosLosLibros: delega en el DAO")
    void obtenerTodosLosLibros_devuelveLista() {
        when(libroDAO.findAll()).thenReturn(List.of(libroConId(1L), libroConId(2L)));

        assertEquals(2, service.obtenerTodosLosLibros().size());
    }

    // ---------------------------------------------------------------- actualizarLibro

    @Test
    @DisplayName("actualizarLibro: libro inexistente lanza ResourceNotFoundException")
    void actualizarLibro_inexistente_lanzaNotFound() {
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarLibro(LIBRO_ID, dto));

        verify(libroDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarLibro: ISBN propio o libre actualiza sin conflicto")
    void actualizarLibro_valido_actualiza() {
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(true);
        // El mismo libro aparece como "dueño" del ISBN: no es conflicto.
        when(libroDAO.findByIsbn(ISBN)).thenReturn(Optional.of(libroConId(LIBRO_ID)));
        when(generoDAO.existsById(GENERO_ID)).thenReturn(true);
        when(autorDAO.existsById(AUTOR_1)).thenReturn(true);
        when(autorDAO.existsById(AUTOR_2)).thenReturn(true);
        when(libroDAO.update(LIBRO_ID, dto)).thenReturn(Optional.of(dto));

        assertSame(dto, service.actualizarLibro(LIBRO_ID, dto));
    }

    @Test
    @DisplayName("actualizarLibro: ISBN de otro libro lanza BusinessValidationException")
    void actualizarLibro_isbnDeOtro_lanzaBusinessValidation() {
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(true);
        when(libroDAO.findByIsbn(ISBN)).thenReturn(Optional.of(libroConId(OTRO_ID)));

        assertThrows(BusinessValidationException.class, () -> service.actualizarLibro(LIBRO_ID, dto));

        verify(libroDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarLibro: anio de publicacion futuro lanza BusinessValidationException")
    void actualizarLibro_anioFuturo_lanzaBusinessValidation() {
        dto.setAnioPublicacion(Year.now().getValue() + 1);
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(true);
        when(libroDAO.findByIsbn(ISBN)).thenReturn(Optional.empty());

        assertThrows(BusinessValidationException.class, () -> service.actualizarLibro(LIBRO_ID, dto));

        verify(libroDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarLibro: genero inexistente lanza ResourceNotFoundException")
    void actualizarLibro_generoInexistente_lanzaNotFound() {
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(true);
        when(libroDAO.findByIsbn(ISBN)).thenReturn(Optional.empty());
        when(generoDAO.existsById(GENERO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarLibro(LIBRO_ID, dto));

        verify(libroDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarLibro: autor inexistente lanza ResourceNotFoundException")
    void actualizarLibro_autorInexistente_lanzaNotFound() {
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(true);
        when(libroDAO.findByIsbn(ISBN)).thenReturn(Optional.empty());
        when(generoDAO.existsById(GENERO_ID)).thenReturn(true);
        when(autorDAO.existsById(AUTOR_1)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarLibro(LIBRO_ID, dto));

        verify(libroDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarLibro: si el DAO no devuelve el libro actualizado lanza ResourceNotFoundException")
    void actualizarLibro_daoNoActualiza_lanzaNotFound() {
        dto.setAutoresIds(null);
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(true);
        when(libroDAO.findByIsbn(ISBN)).thenReturn(Optional.empty());
        when(generoDAO.existsById(GENERO_ID)).thenReturn(true);
        when(libroDAO.update(LIBRO_ID, dto)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarLibro(LIBRO_ID, dto));
    }

    // ---------------------------------------------------------------- eliminarLibro

    @Test
    @DisplayName("eliminarLibro: sin prestamos, reservas ni ejemplares elimina el libro")
    void eliminarLibro_sinDependencias_elimina() {
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(true);
        when(ejemplarDAO.findByLibroId(LIBRO_ID)).thenReturn(List.of());
        when(libroDAO.deleteById(LIBRO_ID)).thenReturn(true);

        service.eliminarLibro(LIBRO_ID);

        verify(libroDAO).deleteById(LIBRO_ID);
    }

    @Test
    @DisplayName("eliminarLibro: libro inexistente lanza ResourceNotFoundException")
    void eliminarLibro_inexistente_lanzaNotFound() {
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarLibro(LIBRO_ID));

        verify(libroDAO, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminarLibro: con prestamos activos lanza BusinessValidationException")
    void eliminarLibro_conPrestamosActivos_lanzaBusinessValidation() {
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(true);
        when(prestamoDAO.existsByEjemplarLibroIdAndEstadoIn(eq(LIBRO_ID), anyCollection())).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.eliminarLibro(LIBRO_ID));

        verify(libroDAO, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminarLibro: con reservas pendientes lanza BusinessValidationException")
    void eliminarLibro_conReservasPendientes_lanzaBusinessValidation() {
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(true);
        when(reservaLibroDAO.existsByLibroIdAndEstadoIn(eq(LIBRO_ID), anyCollection())).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.eliminarLibro(LIBRO_ID));

        verify(libroDAO, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminarLibro: con ejemplares registrados lanza BusinessValidationException")
    void eliminarLibro_conEjemplares_lanzaBusinessValidation() {
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(true);
        when(ejemplarDAO.findByLibroId(LIBRO_ID)).thenReturn(List.of(new EjemplarDTO()));

        assertThrows(BusinessValidationException.class, () -> service.eliminarLibro(LIBRO_ID));

        verify(libroDAO, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminarLibro: si el DAO no logra eliminarlo lanza ResourceNotFoundException")
    void eliminarLibro_daoNoElimina_lanzaNotFound() {
        when(libroDAO.existsById(LIBRO_ID)).thenReturn(true);
        when(libroDAO.deleteById(LIBRO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarLibro(LIBRO_ID));
    }

    // ---------------------------------------------------------------- buscarLibros

    private EjemplarDTO ejemplarEnSede(Long libroId, Long sedeId) {
        EjemplarDTO e = new EjemplarDTO();
        e.setLibroId(libroId);
        e.setSedeId(sedeId);
        e.setEstado("DISPONIBLE");
        return e;
    }

    @Test
    @DisplayName("buscarLibros: sin filtro de disponibilidad devuelve lo encontrado y normaliza textos vacios a nulo")
    void buscarLibros_sinDisponible_devuelveEncontrados() {
        when(libroDAO.buscar("dune", null, null, 1967, GENERO_ID))
                .thenReturn(List.of(libroConId(LIBRO_ID), libroConId(OTRO_ID)));

        List<LibroDTO> resultado = service.buscarLibros(" dune ", "  ", "", 1967, GENERO_ID, null, null);

        assertEquals(2, resultado.size());
        verify(ejemplarDAO, never()).findByLibroIdAndEstadoIgnoreCase(any(), any());
    }

    @Test
    @DisplayName("buscarLibros: disponible=true deja solo los libros con un ejemplar DISPONIBLE")
    void buscarLibros_disponibleTrue_filtraLosDisponibles() {
        when(libroDAO.buscar(null, null, null, null, null))
                .thenReturn(List.of(libroConId(LIBRO_ID), libroConId(OTRO_ID)));
        when(ejemplarDAO.findByLibroIdAndEstadoIgnoreCase(LIBRO_ID, "DISPONIBLE"))
                .thenReturn(List.of(ejemplarEnSede(LIBRO_ID, 5L)));
        when(ejemplarDAO.findByLibroIdAndEstadoIgnoreCase(OTRO_ID, "DISPONIBLE")).thenReturn(List.of());

        List<LibroDTO> resultado = service.buscarLibros(null, null, null, null, null, true, null);

        assertEquals(1, resultado.size());
        assertEquals(LIBRO_ID, resultado.get(0).getId());
    }

    @Test
    @DisplayName("buscarLibros: disponible=false deja solo los libros sin ejemplares DISPONIBLES")
    void buscarLibros_disponibleFalse_filtraLosNoDisponibles() {
        when(libroDAO.buscar(null, null, null, null, null))
                .thenReturn(List.of(libroConId(LIBRO_ID), libroConId(OTRO_ID)));
        when(ejemplarDAO.findByLibroIdAndEstadoIgnoreCase(LIBRO_ID, "DISPONIBLE"))
                .thenReturn(List.of(ejemplarEnSede(LIBRO_ID, 5L)));
        when(ejemplarDAO.findByLibroIdAndEstadoIgnoreCase(OTRO_ID, "DISPONIBLE")).thenReturn(List.of());

        List<LibroDTO> resultado = service.buscarLibros(null, null, null, null, null, false, null);

        assertEquals(1, resultado.size());
        assertEquals(OTRO_ID, resultado.get(0).getId());
    }

    @Test
    @DisplayName("buscarLibros: con sede solo cuenta los ejemplares disponibles en esa sede")
    void buscarLibros_conSede_soloCuentaEjemplaresDeEsaSede() {
        when(libroDAO.buscar(null, null, null, null, null)).thenReturn(List.of(libroConId(LIBRO_ID)));
        when(ejemplarDAO.findByLibroIdAndEstadoIgnoreCase(LIBRO_ID, "DISPONIBLE"))
                .thenReturn(List.of(ejemplarEnSede(LIBRO_ID, 5L)));

        assertEquals(1, service.buscarLibros(null, null, null, null, null, true, 5L).size());
        assertEquals(0, service.buscarLibros(null, null, null, null, null, true, 9L).size());
    }
}