package bussinesLayer.service.impl;

import bussinesLayer.dto.AutorDTO;
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
@DisplayName("AutorServiceImpl")
class AutorServiceImplTest {

    private static final Long AUTOR_ID = 1L;

    @Mock private AutorDAO autorDAO;

    @InjectMocks
    private AutorServiceImpl service;

    private AutorDTO dto;

    @BeforeEach
    void setUp() {
        dto = new AutorDTO();
        dto.setNombre("Gabriel Garcia Marquez");
        dto.setNacionalidad("Colombiano");
        dto.setBiografia("Premio Nobel de Literatura");
    }

    private AutorDTO autorConIdYNombre(Long id, String nombre) {
        AutorDTO a = new AutorDTO();
        a.setId(id);
        a.setNombre(nombre);
        return a;
    }

    // ---------------------------------------------------------------- crearAutor

    @Test
    @DisplayName("crearAutor: nombre libre guarda y devuelve el autor con ID")
    void crearAutor_valido_guarda() {
        when(autorDAO.existsByNombreIgnoreCase("Gabriel Garcia Marquez")).thenReturn(false);
        when(autorDAO.save(dto)).thenAnswer(inv -> {
            AutorDTO a = inv.getArgument(0);
            a.setId(AUTOR_ID);
            return a;
        });

        AutorDTO resultado = service.crearAutor(dto);

        assertEquals(AUTOR_ID, resultado.getId());
        verify(autorDAO).save(dto);
    }

    @Test
    @DisplayName("crearAutor: nombre duplicado lanza BusinessValidationException")
    void crearAutor_nombreDuplicado_lanzaBusinessValidation() {
        when(autorDAO.existsByNombreIgnoreCase("Gabriel Garcia Marquez")).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearAutor(dto));

        verify(autorDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerAutorPorId: devuelve el autor cuando existe")
    void obtenerAutorPorId_existente_loDevuelve() {
        AutorDTO existente = autorConIdYNombre(AUTOR_ID, "Julio Cortazar");
        when(autorDAO.findById(AUTOR_ID)).thenReturn(Optional.of(existente));

        assertSame(existente, service.obtenerAutorPorId(AUTOR_ID));
    }

    @Test
    @DisplayName("obtenerAutorPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerAutorPorId_inexistente_lanzaNotFound() {
        when(autorDAO.findById(AUTOR_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerAutorPorId(AUTOR_ID));
    }

    @Test
    @DisplayName("obtenerTodosLosAutores: delega en el DAO")
    void obtenerTodosLosAutores_devuelveLista() {
        when(autorDAO.findAll()).thenReturn(List.of(
                autorConIdYNombre(1L, "Autor A"), autorConIdYNombre(2L, "Autor B")));

        assertEquals(2, service.obtenerTodosLosAutores().size());
    }

    // ---------------------------------------------------------------- actualizarAutor

    @Test
    @DisplayName("actualizarAutor: autor inexistente lanza ResourceNotFoundException")
    void actualizarAutor_inexistente_lanzaNotFound() {
        when(autorDAO.findById(AUTOR_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarAutor(AUTOR_ID, dto));

        verify(autorDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarAutor: conservando el mismo nombre (sin importar mayusculas) no valida duplicado")
    void actualizarAutor_mismoNombre_actualizaSinValidarDuplicado() {
        AutorDTO actual = autorConIdYNombre(AUTOR_ID, "GABRIEL GARCIA MARQUEZ");
        when(autorDAO.findById(AUTOR_ID)).thenReturn(Optional.of(actual));
        when(autorDAO.update(AUTOR_ID, dto)).thenReturn(Optional.of(dto));

        AutorDTO resultado = service.actualizarAutor(AUTOR_ID, dto);

        assertSame(dto, resultado);
        verify(autorDAO, never()).existsByNombreIgnoreCase(any());
    }

    @Test
    @DisplayName("actualizarAutor: nombre nuevo y libre actualiza")
    void actualizarAutor_nombreNuevoLibre_actualiza() {
        AutorDTO actual = autorConIdYNombre(AUTOR_ID, "Otro Nombre");
        when(autorDAO.findById(AUTOR_ID)).thenReturn(Optional.of(actual));
        when(autorDAO.existsByNombreIgnoreCase("Gabriel Garcia Marquez")).thenReturn(false);
        when(autorDAO.update(AUTOR_ID, dto)).thenReturn(Optional.of(dto));

        assertSame(dto, service.actualizarAutor(AUTOR_ID, dto));
    }

    @Test
    @DisplayName("actualizarAutor: nombre nuevo que ya usa otro autor lanza BusinessValidationException")
    void actualizarAutor_nombreDeOtro_lanzaBusinessValidation() {
        AutorDTO actual = autorConIdYNombre(AUTOR_ID, "Otro Nombre");
        when(autorDAO.findById(AUTOR_ID)).thenReturn(Optional.of(actual));
        when(autorDAO.existsByNombreIgnoreCase("Gabriel Garcia Marquez")).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.actualizarAutor(AUTOR_ID, dto));

        verify(autorDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarAutor: si el DAO no devuelve el autor actualizado lanza ResourceNotFoundException")
    void actualizarAutor_daoNoActualiza_lanzaNotFound() {
        AutorDTO actual = autorConIdYNombre(AUTOR_ID, "Gabriel Garcia Marquez");
        when(autorDAO.findById(AUTOR_ID)).thenReturn(Optional.of(actual));
        when(autorDAO.update(AUTOR_ID, dto)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarAutor(AUTOR_ID, dto));
    }

    // ---------------------------------------------------------------- eliminarAutor

    @Test
    @DisplayName("eliminarAutor: elimina cuando el autor existe")
    void eliminarAutor_existente_elimina() {
        when(autorDAO.deleteById(AUTOR_ID)).thenReturn(true);

        service.eliminarAutor(AUTOR_ID);

        verify(autorDAO).deleteById(AUTOR_ID);
    }

    @Test
    @DisplayName("eliminarAutor: autor inexistente lanza ResourceNotFoundException")
    void eliminarAutor_inexistente_lanzaNotFound() {
        when(autorDAO.deleteById(AUTOR_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarAutor(AUTOR_ID));
    }
}
