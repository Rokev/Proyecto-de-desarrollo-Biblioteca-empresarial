package bussinesLayer.service.impl;

import bussinesLayer.dto.AuditoriaDTO;
import bussinesLayer.dto.UsuarioDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import persistanceLayer.dao.AuditoriaDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditoriaServiceImpl")
class AuditoriaServiceImplTest {

    private static final Long USUARIO_ID = 5L;
    private static final Long ENTIDAD_ID = 10L;
    private static final Long AUDITORIA_ID = 1L;

    @Mock private AuditoriaDAO auditoriaDAO;
    @Mock private UsuarioDAO usuarioDAO;

    @InjectMocks
    private AuditoriaServiceImpl service;

    /** El DAO real devuelve el DTO guardado; aquí se devuelve el mismo objeto recibido. */
    private void guardarDevuelveLoRecibido() {
        when(auditoriaDAO.save(any(AuditoriaDTO.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private UsuarioDTO usuarioConNombre(String nombre) {
        UsuarioDTO u = new UsuarioDTO();
        u.setNombreUsuario(nombre);
        return u;
    }

    // ---------------------------------------------------------------- registrar: validaciones

    @Test
    @DisplayName("registrar: acción nula lanza BusinessValidationException")
    void registrar_accionNula_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class,
                () -> service.registrar(USUARIO_ID, null, "SEDES", ENTIDAD_ID, "desc"));

        verify(auditoriaDAO, never()).save(any());
    }

    @Test
    @DisplayName("registrar: acción en blanco lanza BusinessValidationException")
    void registrar_accionEnBlanco_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class,
                () -> service.registrar(USUARIO_ID, "   ", "SEDES", ENTIDAD_ID, "desc"));

        verify(auditoriaDAO, never()).save(any());
    }

    @Test
    @DisplayName("registrar: módulo nulo lanza BusinessValidationException")
    void registrar_moduloNulo_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class,
                () -> service.registrar(USUARIO_ID, "CREAR", null, ENTIDAD_ID, "desc"));

        verify(auditoriaDAO, never()).save(any());
    }

    @Test
    @DisplayName("registrar: módulo en blanco lanza BusinessValidationException")
    void registrar_moduloEnBlanco_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class,
                () -> service.registrar(USUARIO_ID, "CREAR", "", ENTIDAD_ID, "desc"));

        verify(auditoriaDAO, never()).save(any());
    }

    // ---------------------------------------------------------------- registrar: flujo normal

    @Test
    @DisplayName("registrar: normaliza acción y módulo (trim + mayúsculas) y resuelve el nombre del usuario")
    void registrar_valido_normalizaYResuelveUsuario() {
        when(usuarioDAO.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioConNombre("admin")));
        guardarDevuelveLoRecibido();
        LocalDateTime antes = LocalDateTime.now();

        AuditoriaDTO resultado = service.registrar(USUARIO_ID, "  modificar ", " sedes  ", ENTIDAD_ID,
                "Sede 'Centro' desactivada");

        assertEquals(USUARIO_ID, resultado.getUsuarioId());
        assertEquals("admin", resultado.getUsuarioNombre());
        assertEquals("MODIFICAR", resultado.getAccion());
        assertEquals("SEDES", resultado.getModulo());
        assertEquals(ENTIDAD_ID, resultado.getEntidadId());
        assertEquals("Sede 'Centro' desactivada", resultado.getDescripcion());
        assertFalse(resultado.getFecha().isBefore(antes), "la fecha debe ser la del momento del registro");
    }

    @Test
    @DisplayName("registrar: persiste el DTO con los datos normalizados")
    void registrar_valido_persisteElDto() {
        when(usuarioDAO.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioConNombre("admin")));
        guardarDevuelveLoRecibido();

        service.registrar(USUARIO_ID, "crear", "libros", ENTIDAD_ID, "Libro creado");

        ArgumentCaptor<AuditoriaDTO> captor = ArgumentCaptor.forClass(AuditoriaDTO.class);
        verify(auditoriaDAO).save(captor.capture());
        assertEquals("CREAR", captor.getValue().getAccion());
        assertEquals("LIBROS", captor.getValue().getModulo());
    }

    @Test
    @DisplayName("registrar: sin usuario (acción del sistema) usa 'SISTEMA' y no consulta usuarios")
    void registrar_usuarioNulo_usaSistema() {
        guardarDevuelveLoRecibido();

        AuditoriaDTO resultado = service.registrar(null, "CREAR", "PRESTAMOS", ENTIDAD_ID, "Tarea programada");

        assertNull(resultado.getUsuarioId());
        assertEquals("SISTEMA", resultado.getUsuarioNombre());
        verifyNoInteractions(usuarioDAO);
    }

    @Test
    @DisplayName("registrar: usuario inexistente usa el nombre de respaldo 'usuario#ID'")
    void registrar_usuarioInexistente_usaNombreDeRespaldo() {
        when(usuarioDAO.findById(99L)).thenReturn(Optional.empty());
        guardarDevuelveLoRecibido();

        AuditoriaDTO resultado = service.registrar(99L, "ELIMINAR", "ROLES", ENTIDAD_ID, "Rol eliminado");

        assertEquals("usuario#99", resultado.getUsuarioNombre());
    }

    @Test
    @DisplayName("registrar: descripción nula usa '(sin descripción)'")
    void registrar_descripcionNula_usaTextoPorDefecto() {
        when(usuarioDAO.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioConNombre("admin")));
        guardarDevuelveLoRecibido();

        AuditoriaDTO resultado = service.registrar(USUARIO_ID, "CREAR", "SEDES", ENTIDAD_ID, null);

        assertEquals("(sin descripción)", resultado.getDescripcion());
    }

    @Test
    @DisplayName("registrar: descripción en blanco usa '(sin descripción)'")
    void registrar_descripcionEnBlanco_usaTextoPorDefecto() {
        when(usuarioDAO.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioConNombre("admin")));
        guardarDevuelveLoRecibido();

        AuditoriaDTO resultado = service.registrar(USUARIO_ID, "CREAR", "SEDES", ENTIDAD_ID, "   ");

        assertEquals("(sin descripción)", resultado.getDescripcion());
    }

    @Test
    @DisplayName("registrar: descripción de más de 500 caracteres se recorta a 500 terminando en '...'")
    void registrar_descripcionLarga_serecorta() {
        when(usuarioDAO.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioConNombre("admin")));
        guardarDevuelveLoRecibido();

        AuditoriaDTO resultado = service.registrar(USUARIO_ID, "CREAR", "SEDES", ENTIDAD_ID, "x".repeat(600));

        assertEquals(500, resultado.getDescripcion().length());
        assertTrue(resultado.getDescripcion().endsWith("..."));
    }

    @Test
    @DisplayName("registrar: descripción de exactamente 500 caracteres no se recorta")
    void registrar_descripcionEnElLimite_noSeRecorta() {
        when(usuarioDAO.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioConNombre("admin")));
        guardarDevuelveLoRecibido();
        String descripcion = "y".repeat(500);

        AuditoriaDTO resultado = service.registrar(USUARIO_ID, "CREAR", "SEDES", ENTIDAD_ID, descripcion);

        assertEquals(descripcion, resultado.getDescripcion());
    }

    // ---------------------------------------------------------------- obtenerPorId

    @Test
    @DisplayName("obtenerPorId: devuelve el registro cuando existe")
    void obtenerPorId_existente_loDevuelve() {
        AuditoriaDTO existente = new AuditoriaDTO();
        existente.setId(AUDITORIA_ID);
        when(auditoriaDAO.findById(AUDITORIA_ID)).thenReturn(Optional.of(existente));

        assertSame(existente, service.obtenerPorId(AUDITORIA_ID));
    }

    @Test
    @DisplayName("obtenerPorId: lanza ResourceNotFoundException cuando no existe")
    void obtenerPorId_inexistente_lanzaNotFound() {
        when(auditoriaDAO.findById(AUDITORIA_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerPorId(AUDITORIA_ID));
    }

    // ---------------------------------------------------------------- consultar: validaciones

    @Test
    @DisplayName("consultar: 'hasta' anterior a 'desde' lanza BusinessValidationException")
    void consultar_rangoInvertido_lanzaBusinessValidation() {
        LocalDate desde = LocalDate.of(2026, 10, 20);
        LocalDate hasta = LocalDate.of(2026, 10, 19);

        assertThrows(BusinessValidationException.class,
                () -> service.consultar(null, desde, hasta, null, null, 0, 10));

        verifyNoInteractions(auditoriaDAO);
    }

    @Test
    @DisplayName("consultar: página negativa lanza BusinessValidationException")
    void consultar_paginaNegativa_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class,
                () -> service.consultar(null, null, null, null, null, -1, 10));

        verifyNoInteractions(auditoriaDAO);
    }

    @Test
    @DisplayName("consultar: tamaño de página menor que 1 lanza BusinessValidationException")
    void consultar_tamanioCero_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class,
                () -> service.consultar(null, null, null, null, null, 0, 0));

        verifyNoInteractions(auditoriaDAO);
    }

    @Test
    @DisplayName("consultar: tamaño de página mayor que 200 lanza BusinessValidationException")
    void consultar_tamanioExcedeMaximo_lanzaBusinessValidation() {
        assertThrows(BusinessValidationException.class,
                () -> service.consultar(null, null, null, null, null, 0, 201));

        verifyNoInteractions(auditoriaDAO);
    }

    // ---------------------------------------------------------------- consultar: flujo normal

    @Test
    @DisplayName("consultar: tamaño de página 200 (límite) es válido")
    void consultar_tamanioEnElLimite_esValido() {
        AuditoriaDAO.Pagina pagina = new AuditoriaDAO.Pagina(List.of(), 0);
        when(auditoriaDAO.buscar(any(), any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(pagina);

        assertSame(pagina, service.consultar(null, null, null, null, null, 0, 200));
    }

    @Test
    @DisplayName("consultar: convierte 'desde' a inicio del día y 'hasta' a inicio del día siguiente")
    void consultar_convierteFechasARangoSemiAbierto() {
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 31);
        AuditoriaDAO.Pagina pagina = new AuditoriaDAO.Pagina(List.of(new AuditoriaDTO()), 1);
        when(auditoriaDAO.buscar(USUARIO_ID, LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 11, 1, 0, 0), "CREAR", "SEDES", 2, 25)).thenReturn(pagina);

        AuditoriaDAO.Pagina resultado = service.consultar(USUARIO_ID, desde, hasta, "CREAR", "SEDES", 2, 25);

        assertSame(pagina, resultado);
    }

    @Test
    @DisplayName("consultar: mismo día en 'desde' y 'hasta' es válido (incluye el día completo)")
    void consultar_mismoDia_esValido() {
        LocalDate dia = LocalDate.of(2026, 10, 25);
        AuditoriaDAO.Pagina pagina = new AuditoriaDAO.Pagina(List.of(), 0);
        when(auditoriaDAO.buscar(isNull(), any(LocalDateTime.class), any(LocalDateTime.class),
                isNull(), isNull(), anyInt(), anyInt())).thenReturn(pagina);

        assertSame(pagina, service.consultar(null, dia, dia, null, null, 0, 10));
        verify(auditoriaDAO).buscar(null, LocalDateTime.of(2026, 10, 25, 0, 0),
                LocalDateTime.of(2026, 10, 26, 0, 0), null, null, 0, 10);
    }

    @Test
    @DisplayName("consultar: sin fechas pasa nulos al DAO")
    void consultar_sinFechas_pasaNulos() {
        AuditoriaDAO.Pagina pagina = new AuditoriaDAO.Pagina(List.of(), 0);
        when(auditoriaDAO.buscar(isNull(), isNull(), isNull(), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(pagina);

        assertSame(pagina, service.consultar(null, null, null, null, null, 0, 10));
    }

    @Test
    @DisplayName("consultar: acción y módulo en blanco se convierten en nulo (sin filtro)")
    void consultar_filtrosEnBlanco_seConvierteEnNulo() {
        AuditoriaDAO.Pagina pagina = new AuditoriaDAO.Pagina(List.of(), 0);
        when(auditoriaDAO.buscar(anyLong(), isNull(), isNull(), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(pagina);

        assertSame(pagina, service.consultar(USUARIO_ID, null, null, "   ", "", 0, 10));
    }

    @Test
    @DisplayName("consultar: acción y módulo se recortan con trim antes de consultar")
    void consultar_filtrosConEspacios_seRecortan() {
        AuditoriaDAO.Pagina pagina = new AuditoriaDAO.Pagina(List.of(), 0);
        when(auditoriaDAO.buscar(isNull(), isNull(), isNull(), eq("crear"), eq("libros"), anyInt(), anyInt()))
                .thenReturn(pagina);

        assertSame(pagina, service.consultar(null, null, null, "  crear ", " libros  ", 0, 10));
    }
}