package bussinesLayer.service.impl;

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
import persistanceLayer.dao.SedeDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioServiceImpl")
class UsuarioServiceImplTest {

    private static final Long USUARIO_ID = 1L;
    private static final Long OTRO_ID = 2L;
    private static final Long ROL_ID = 5L;
    private static final Long SEDE_ID = 7L;

    @Mock private UsuarioDAO usuarioDAO;
    @Mock private RolDAO rolDAO;
    @Mock private SedeDAO sedeDAO;

    @InjectMocks
    private UsuarioServiceImpl service;

    private UsuarioDTO dto;

    @BeforeEach
    void setUp() {
        dto = new UsuarioDTO();
        dto.setNombre("Maria");
        dto.setApellido("Perez");
        dto.setDocumento("1020304050");
        dto.setNombreUsuario("maria12");
        dto.setCorreo("maria12@gmail.com");
        dto.setContrasena("Clave1234");
        dto.setRolId(ROL_ID);
    }

    private UsuarioDTO usuarioConId(Long id) {
        UsuarioDTO u = new UsuarioDTO();
        u.setId(id);
        return u;
    }

    // ---------------------------------------------------------------- crearUsuario

    @Test
    @DisplayName("crearUsuario: asigna activo=true y fechaRegistro cuando vienen vacios")
    void crearUsuario_valido_aplicaDefaults() {
        when(rolDAO.existsById(ROL_ID)).thenReturn(true);
        when(usuarioDAO.save(any(UsuarioDTO.class))).thenAnswer(inv -> {
            UsuarioDTO u = inv.getArgument(0);
            u.setId(USUARIO_ID);
            return u;
        });

        UsuarioDTO resultado = service.crearUsuario(dto);

        assertEquals(USUARIO_ID, resultado.getId());
        assertTrue(resultado.getActivo());
        assertNotNull(resultado.getFechaRegistro());
    }

    @Test
    @DisplayName("crearUsuario: conserva activo y fechaRegistro si ya vienen informados")
    void crearUsuario_conActivoYFecha_losConserva() {
        LocalDateTime fecha = LocalDateTime.of(2026, 1, 15, 10, 0);
        dto.setActivo(false);
        dto.setFechaRegistro(fecha);
        when(rolDAO.existsById(ROL_ID)).thenReturn(true);
        when(usuarioDAO.save(any(UsuarioDTO.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDTO resultado = service.crearUsuario(dto);

        assertFalse(resultado.getActivo());
        assertEquals(fecha, resultado.getFechaRegistro());
    }

    @Test
    @DisplayName("crearUsuario: con sede existente la valida y crea el usuario")
    void crearUsuario_conSedeExistente_crea() {
        dto.setSedeId(SEDE_ID);
        when(rolDAO.existsById(ROL_ID)).thenReturn(true);
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(true);
        when(usuarioDAO.save(any(UsuarioDTO.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDTO resultado = service.crearUsuario(dto);

        assertEquals(SEDE_ID, resultado.getSedeId());
    }

    @Test
    @DisplayName("crearUsuario: nombre de usuario duplicado lanza BusinessValidationException")
    void crearUsuario_nombreUsuarioDuplicado_lanzaBusinessValidation() {
        when(usuarioDAO.existsByNombreUsuarioIgnoreCase("maria12")).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearUsuario(dto));

        verify(usuarioDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearUsuario: correo duplicado lanza BusinessValidationException")
    void crearUsuario_correoDuplicado_lanzaBusinessValidation() {
        when(usuarioDAO.existsByCorreoIgnoreCase("maria12@gmail.com")).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearUsuario(dto));

        verify(usuarioDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearUsuario: documento duplicado lanza BusinessValidationException")
    void crearUsuario_documentoDuplicado_lanzaBusinessValidation() {
        when(usuarioDAO.existsByDocumento("1020304050")).thenReturn(true);

        assertThrows(BusinessValidationException.class, () -> service.crearUsuario(dto));

        verify(usuarioDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearUsuario: rol inexistente lanza ResourceNotFoundException")
    void crearUsuario_rolInexistente_lanzaNotFound() {
        when(rolDAO.existsById(ROL_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearUsuario(dto));

        verify(usuarioDAO, never()).save(any());
    }

    @Test
    @DisplayName("crearUsuario: sede inexistente lanza ResourceNotFoundException")
    void crearUsuario_sedeInexistente_lanzaNotFound() {
        dto.setSedeId(SEDE_ID);
        when(rolDAO.existsById(ROL_ID)).thenReturn(true);
        when(sedeDAO.existsById(SEDE_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.crearUsuario(dto));

        verify(usuarioDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerUsuarioPorId: devuelve el usuario cuando existe")
    void obtenerUsuarioPorId_existente_loDevuelve() {
        UsuarioDTO existente = usuarioConId(USUARIO_ID);
        when(usuarioDAO.findById(USUARIO_ID)).thenReturn(Optional.of(existente));

        assertSame(existente, service.obtenerUsuarioPorId(USUARIO_ID));
    }

    @Test
    @DisplayName("obtenerUsuarioPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerUsuarioPorId_inexistente_lanzaNotFound() {
        when(usuarioDAO.findById(USUARIO_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerUsuarioPorId(USUARIO_ID));
    }

    @Test
    @DisplayName("obtenerTodosLosUsuarios: delega en el DAO")
    void obtenerTodosLosUsuarios_devuelveLista() {
        when(usuarioDAO.findAll()).thenReturn(List.of(usuarioConId(1L), usuarioConId(2L)));

        assertEquals(2, service.obtenerTodosLosUsuarios().size());
    }

    // ---------------------------------------------------------------- actualizarUsuario

    @Test
    @DisplayName("actualizarUsuario: usuario inexistente lanza ResourceNotFoundException")
    void actualizarUsuario_inexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarUsuario(USUARIO_ID, dto));

        verify(usuarioDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarUsuario: datos propios o libres actualizan sin conflicto")
    void actualizarUsuario_valido_actualiza() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        // El mismo usuario aparece como "dueño" del nombre de usuario: no es conflicto.
        when(usuarioDAO.findByNombreUsuarioIgnoreCase("maria12")).thenReturn(Optional.of(usuarioConId(USUARIO_ID)));
        when(rolDAO.existsById(ROL_ID)).thenReturn(true);
        when(usuarioDAO.update(USUARIO_ID, dto)).thenReturn(Optional.of(dto));

        UsuarioDTO resultado = service.actualizarUsuario(USUARIO_ID, dto);

        assertSame(dto, resultado);
    }

    @Test
    @DisplayName("actualizarUsuario: nombre de usuario de otra persona lanza BusinessValidationException")
    void actualizarUsuario_nombreUsuarioDeOtro_lanzaBusinessValidation() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(usuarioDAO.findByNombreUsuarioIgnoreCase("maria12")).thenReturn(Optional.of(usuarioConId(OTRO_ID)));

        assertThrows(BusinessValidationException.class, () -> service.actualizarUsuario(USUARIO_ID, dto));

        verify(usuarioDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarUsuario: correo de otra persona lanza BusinessValidationException")
    void actualizarUsuario_correoDeOtro_lanzaBusinessValidation() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(usuarioDAO.findByCorreoIgnoreCase("maria12@gmail.com")).thenReturn(Optional.of(usuarioConId(OTRO_ID)));

        assertThrows(BusinessValidationException.class, () -> service.actualizarUsuario(USUARIO_ID, dto));

        verify(usuarioDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarUsuario: documento de otra persona lanza BusinessValidationException")
    void actualizarUsuario_documentoDeOtro_lanzaBusinessValidation() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(usuarioDAO.findByDocumento("1020304050")).thenReturn(Optional.of(usuarioConId(OTRO_ID)));

        assertThrows(BusinessValidationException.class, () -> service.actualizarUsuario(USUARIO_ID, dto));

        verify(usuarioDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarUsuario: rol inexistente lanza ResourceNotFoundException")
    void actualizarUsuario_rolInexistente_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(rolDAO.existsById(ROL_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarUsuario(USUARIO_ID, dto));

        verify(usuarioDAO, never()).update(any(), any());
    }

    @Test
    @DisplayName("actualizarUsuario: si el DAO no devuelve el usuario actualizado lanza ResourceNotFoundException")
    void actualizarUsuario_daoNoActualiza_lanzaNotFound() {
        when(usuarioDAO.existsById(USUARIO_ID)).thenReturn(true);
        when(rolDAO.existsById(ROL_ID)).thenReturn(true);
        when(usuarioDAO.update(USUARIO_ID, dto)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarUsuario(USUARIO_ID, dto));
    }

    // ---------------------------------------------------------------- eliminarUsuario

    @Test
    @DisplayName("eliminarUsuario: elimina cuando el usuario existe")
    void eliminarUsuario_existente_elimina() {
        when(usuarioDAO.deleteById(USUARIO_ID)).thenReturn(true);

        service.eliminarUsuario(USUARIO_ID);

        verify(usuarioDAO).deleteById(USUARIO_ID);
    }

    @Test
    @DisplayName("eliminarUsuario: usuario inexistente lanza ResourceNotFoundException")
    void eliminarUsuario_inexistente_lanzaNotFound() {
        when(usuarioDAO.deleteById(USUARIO_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarUsuario(USUARIO_ID));
    }
}
