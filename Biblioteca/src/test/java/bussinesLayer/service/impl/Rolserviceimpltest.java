package bussinesLayer.service.impl;

import bussinesLayer.dto.RolDTO;
import bussinesLayer.dto.UsuarioDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.RolDAO;
import persistanceLayer.dao.UsuarioDAO;

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
@DisplayName("RolServiceImpl")
class RolServiceImplTest {

    private static final Long ROL_ID = 1L;
    private static final Long OTRO_ID = 2L;

    @Mock private RolDAO rolDAO;
    @Mock private UsuarioDAO usuarioDAO;

    @InjectMocks
    private RolServiceImpl service;

    private RolDTO dto;

    @BeforeEach
    void setUp() {
        dto = new RolDTO();
        dto.setNombre("CLIENTE");
        dto.setDescripcion("Usuario cliente de la biblioteca");
    }

    private RolDTO rolConId(Long id) {
        RolDTO r = new RolDTO();
        r.setId(id);
        return r;
    }

    // ---------------------------------------------------------------- crearRol

    @Test
    @DisplayName("crearRol: nombre libre guarda y devuelve el rol con ID")
    void crearRol_valido_guarda() {
        when(rolDAO.existsByNombreIgnoreCase("CLIENTE")).thenReturn(false);
        when(rolDAO.save(dto)).thenAnswer(inv -> {
            RolDTO r = inv.getArgument(0);
            r.setId(ROL_ID);
            return r;
        });

        RolDTO resultado = service.crearRol(dto);

        assertEquals(ROL_ID, resultado.getId());
        verify(rolDAO).save(dto);
    }

    @Test
    @DisplayName("crearRol: nombre duplicado lanza BusinessValidationException")
    void crearRol_nombreDuplicado_lanzaBusinessValidation() {
        when(rolDAO.existsByNombreIgnoreCase("CLIENTE")).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearRol(dto));

        verify(rolDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerRolPorId: devuelve el rol cuando existe")
    void obtenerRolPorId_existente_loDevuelve() {
        RolDTO existente = rolConId(ROL_ID);
        when(rolDAO.findById(ROL_ID)).thenReturn(Optional.of(existente));

        assertSame(existente, service.obtenerRolPorId(ROL_ID));
    }

    @Test
    @DisplayName("obtenerRolPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerRolPorId_inexistente_lanzaNotFound() {
        when(rolDAO.findById(ROL_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerRolPorId(ROL_ID));
    }

    @Test
    @DisplayName("obtenerTodosLosRoles: delega en el DAO")
    void obtenerTodosLosRoles_devuelveLista() {
        when(rolDAO.findAll()).thenReturn(List.of(rolConId(1L), rolConId(2L)));

        assertEquals(2, service.obtenerTodosLosRoles().size());
    }

    // ---------------------------------------------------------------- actualizarRol

    @Test
    @DisplayName("actualizarRol: rol inexistente lanza ResourceNotFoundException")
    void actualizarRol_inexistente_lanzaNotFound() {
        when(rolDAO.existsById(ROL_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarRol(ROL_ID, dto));

        verify(rolDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarRol: nombre propio o libre actualiza sin conflicto")
    void actualizarRol_valido_actualiza() {
        when(rolDAO.existsById(ROL_ID)).thenReturn(true);
        // El mismo rol aparece como "dueño" del nombre: no es conflicto.
        when(rolDAO.findByNombreIgnoreCase("CLIENTE")).thenReturn(Optional.of(rolConId(ROL_ID)));
        when(rolDAO.update(ROL_ID, dto)).thenReturn(Optional.of(dto));

        assertSame(dto, service.actualizarRol(ROL_ID, dto));
    }

    @Test
    @DisplayName("actualizarRol: nombre de otro rol lanza BusinessValidationException")
    void actualizarRol_nombreDeOtro_lanzaBusinessValidation() {
        when(rolDAO.existsById(ROL_ID)).thenReturn(true);
        when(rolDAO.findByNombreIgnoreCase("CLIENTE")).thenReturn(Optional.of(rolConId(OTRO_ID)));

        assertThrows(BusinessValidationException.class, () -> service.actualizarRol(ROL_ID, dto));

        verify(rolDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarRol: si el DAO no devuelve el rol actualizado lanza ResourceNotFoundException")
    void actualizarRol_daoNoActualiza_lanzaNotFound() {
        when(rolDAO.existsById(ROL_ID)).thenReturn(true);
        when(rolDAO.findByNombreIgnoreCase("CLIENTE")).thenReturn(Optional.empty());
        when(rolDAO.update(ROL_ID, dto)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarRol(ROL_ID, dto));
    }

    // ---------------------------------------------------------------- eliminarRol

    @Test
    @DisplayName("eliminarRol: sin usuarios asociados elimina el rol")
    void eliminarRol_sinUsuarios_elimina() {
        when(rolDAO.existsById(ROL_ID)).thenReturn(true);
        when(usuarioDAO.findByRolId(ROL_ID)).thenReturn(List.of());

        service.eliminarRol(ROL_ID);

        verify(rolDAO).deleteById(ROL_ID);
    }

    @Test
    @DisplayName("eliminarRol: rol inexistente lanza ResourceNotFoundException")
    void eliminarRol_inexistente_lanzaNotFound() {
        when(rolDAO.existsById(ROL_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarRol(ROL_ID));

        verify(rolDAO, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminarRol: con usuarios asociados lanza BusinessValidationException")
    void eliminarRol_conUsuarios_lanzaBusinessValidation() {
        when(rolDAO.existsById(ROL_ID)).thenReturn(true);
        when(usuarioDAO.findByRolId(ROL_ID)).thenReturn(List.of(new UsuarioDTO()));

        assertThrows(BusinessValidationException.class, () -> service.eliminarRol(ROL_ID));

        verify(rolDAO, never()).deleteById(any());
    }
}
