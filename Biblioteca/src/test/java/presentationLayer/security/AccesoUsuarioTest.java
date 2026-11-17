package presentationLayer.security;

import bussinesLayer.dto.UsuarioDTO;
import bussinesLayer.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AccesoUsuario")
class AccesoUsuarioTest {

    private static final Long YO = 5L;
    private static final Long OTRO = 6L;
    private static final Long MI_SEDE = 1L;
    private static final Long OTRA_SEDE = 2L;

    private final UsuarioService usuarioService = Mockito.mock(UsuarioService.class);
    private final AccesoUsuario acceso = new AccesoUsuario(usuarioService);

    /** Autentica como un usuario con un JWT real (uid y rol) en el contexto de seguridad. */
    private void autenticarComo(String rol, Long uid) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .claim("uid", uid)
                .claim("rol", rol)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }

    private void usuarioConSede(Long sedeId) {
        UsuarioDTO u = new UsuarioDTO();
        u.setId(YO);
        u.setSedeId(sedeId);
        Mockito.when(usuarioService.obtenerUsuarioPorId(YO)).thenReturn(u);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private record Item(Long dueno, Long sede) {
    }

    // ---------------------------------------------------------------- identidad y roles

    @Test
    @DisplayName("idActual: devuelve el uid del token")
    void idActual_devuelveElUid() {
        autenticarComo("CLIENTE", YO);

        assertEquals(YO, acceso.idActual());
    }

    @Test
    @DisplayName("idActual: sin autenticacion lanza AccessDeniedException")
    void idActual_sinSesion_lanzaAccessDenied() {
        assertThrows(AccessDeniedException.class, acceso::idActual);
    }

    @Test
    @DisplayName("roles: tieneRol, esAdmin, esBibliotecario y esPersonal")
    void roles() {
        autenticarComo("BIBLIOTECARIO", YO);
        assertTrue(acceso.tieneRol("BIBLIOTECARIO"));
        assertTrue(acceso.tieneRol("ADMIN", "BIBLIOTECARIO"));
        assertFalse(acceso.tieneRol("ADMIN"));
        assertFalse(acceso.esAdmin());
        assertTrue(acceso.esBibliotecario());
        assertTrue(acceso.esPersonal());

        autenticarComo("CLIENTE", YO);
        assertFalse(acceso.esPersonal());

        autenticarComo("ADMIN", YO);
        assertTrue(acceso.esAdmin());
        assertTrue(acceso.esPersonal());
    }

    // ---------------------------------------------------------------- propietario

    @Test
    @DisplayName("validarPropietario: el cliente accede a lo suyo")
    void validarPropietario_clienteLoPropio() {
        autenticarComo("CLIENTE", YO);

        assertDoesNotThrow(() -> acceso.validarPropietario(YO));
    }

    @Test
    @DisplayName("validarPropietario: el cliente no accede a datos de otro usuario ni con id nulo")
    void validarPropietario_clienteAjeno() {
        autenticarComo("CLIENTE", YO);

        assertThrows(AccessDeniedException.class, () -> acceso.validarPropietario(OTRO));
        assertThrows(AccessDeniedException.class, () -> acceso.validarPropietario(null));
    }

    @Test
    @DisplayName("validarPropietario: el personal accede a datos de cualquiera")
    void validarPropietario_personal() {
        autenticarComo("BIBLIOTECARIO", YO);
        assertDoesNotThrow(() -> acceso.validarPropietario(OTRO));

        autenticarComo("ADMIN", YO);
        assertDoesNotThrow(() -> acceso.validarPropietario(OTRO));
    }

    // ---------------------------------------------------------------- actor

    @Test
    @DisplayName("validarActor: un bibliotecario o repartidor solo actua como si mismo")
    void validarActor_soloComoSiMismo() {
        autenticarComo("BIBLIOTECARIO", YO);
        assertDoesNotThrow(() -> acceso.validarActor(YO));
        assertThrows(AccessDeniedException.class, () -> acceso.validarActor(OTRO));
        assertThrows(AccessDeniedException.class, () -> acceso.validarActor(null));

        autenticarComo("REPARTIDOR", YO);
        assertThrows(AccessDeniedException.class, () -> acceso.validarActor(OTRO));
    }

    @Test
    @DisplayName("validarActor: el admin puede actuar en nombre de otro")
    void validarActor_admin() {
        autenticarComo("ADMIN", YO);

        assertDoesNotThrow(() -> acceso.validarActor(OTRO));
    }

    // ---------------------------------------------------------------- sede

    @Test
    @DisplayName("validarSede: el admin pasa en cualquier sede sin consultar su cuenta")
    void validarSede_admin() {
        autenticarComo("ADMIN", YO);

        assertDoesNotThrow(() -> acceso.validarSede(OTRA_SEDE));
        Mockito.verifyNoInteractions(usuarioService);
    }

    @Test
    @DisplayName("validarSede: el bibliotecario pasa solo en su sede")
    void validarSede_bibliotecarioSoloSuSede() {
        autenticarComo("BIBLIOTECARIO", YO);
        usuarioConSede(MI_SEDE);

        assertDoesNotThrow(() -> acceso.validarSede(MI_SEDE));
        assertThrows(AccessDeniedException.class, () -> acceso.validarSede(OTRA_SEDE));
    }

    @Test
    @DisplayName("validarSede: con varias sedes basta que una sea la suya (pedido interciudad)")
    void validarSede_variasSedes() {
        autenticarComo("BIBLIOTECARIO", YO);
        usuarioConSede(MI_SEDE);

        assertDoesNotThrow(() -> acceso.validarSede(OTRA_SEDE, MI_SEDE));
        assertThrows(AccessDeniedException.class, () -> acceso.validarSede(OTRA_SEDE, 3L));
    }

    @Test
    @DisplayName("validarSede: un bibliotecario sin sede asignada no accede a ninguna")
    void validarSede_bibliotecarioSinSede() {
        autenticarComo("BIBLIOTECARIO", YO);
        usuarioConSede(null);

        assertThrows(AccessDeniedException.class, () -> acceso.validarSede(MI_SEDE));
    }

    @Test
    @DisplayName("validarSede: un cliente nunca pasa")
    void validarSede_cliente() {
        autenticarComo("CLIENTE", YO);

        assertThrows(AccessDeniedException.class, () -> acceso.validarSede(MI_SEDE));
    }

    @Test
    @DisplayName("puedeVerSede: refleja las mismas reglas sin lanzar excepcion")
    void puedeVerSede() {
        autenticarComo("BIBLIOTECARIO", YO);
        usuarioConSede(MI_SEDE);

        assertTrue(acceso.puedeVerSede(MI_SEDE));
        assertFalse(acceso.puedeVerSede(OTRA_SEDE));
    }

    // ---------------------------------------------------------------- propietario o sede

    @Test
    @DisplayName("validarPropietarioOSede: el dueño pasa aunque el recurso sea de otra sede")
    void validarPropietarioOSede_dueno() {
        autenticarComo("CLIENTE", YO);

        assertDoesNotThrow(() -> acceso.validarPropietarioOSede(YO, OTRA_SEDE));
    }

    @Test
    @DisplayName("validarPropietarioOSede: un cliente ajeno no pasa")
    void validarPropietarioOSede_clienteAjeno() {
        autenticarComo("CLIENTE", YO);

        assertThrows(AccessDeniedException.class, () -> acceso.validarPropietarioOSede(OTRO, MI_SEDE));
    }

    @Test
    @DisplayName("validarPropietarioOSede: el bibliotecario pasa solo si el recurso es de su sede")
    void validarPropietarioOSede_bibliotecario() {
        autenticarComo("BIBLIOTECARIO", YO);
        usuarioConSede(MI_SEDE);

        assertDoesNotThrow(() -> acceso.validarPropietarioOSede(OTRO, MI_SEDE));
        assertThrows(AccessDeniedException.class, () -> acceso.validarPropietarioOSede(OTRO, OTRA_SEDE));
    }

    @Test
    @DisplayName("validarPropietarioOSede: el admin siempre pasa")
    void validarPropietarioOSede_admin() {
        autenticarComo("ADMIN", YO);

        assertDoesNotThrow(() -> acceso.validarPropietarioOSede(OTRO, OTRA_SEDE));
    }

    // ---------------------------------------------------------------- listados

    private final List<Item> items = List.of(
            new Item(YO, MI_SEDE),
            new Item(OTRO, MI_SEDE),
            new Item(OTRO, OTRA_SEDE));

    @Test
    @DisplayName("visibles: el admin ve todo")
    void visibles_admin() {
        autenticarComo("ADMIN", YO);

        assertEquals(3, acceso.visibles(items, Item::dueno, i -> acceso.sedes(i.sede())).size());
    }

    @Test
    @DisplayName("visibles: el bibliotecario ve solo lo de su sede")
    void visibles_bibliotecario() {
        autenticarComo("BIBLIOTECARIO", YO);
        usuarioConSede(MI_SEDE);

        List<Item> resultado = acceso.visibles(items, Item::dueno, i -> acceso.sedes(i.sede()));

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().allMatch(i -> MI_SEDE.equals(i.sede())));
    }

    @Test
    @DisplayName("visibles: el cliente ve solo lo suyo")
    void visibles_cliente() {
        autenticarComo("CLIENTE", YO);

        List<Item> resultado = acceso.visibles(items, Item::dueno, i -> acceso.sedes(i.sede()));

        assertEquals(1, resultado.size());
        assertEquals(YO, resultado.get(0).dueno());
    }

    @Test
    @DisplayName("soloPropios: el personal ve todo y el cliente solo lo suyo")
    void soloPropios() {
        autenticarComo("BIBLIOTECARIO", YO);
        assertEquals(3, acceso.soloPropios(items, Item::dueno).size());

        autenticarComo("CLIENTE", YO);
        assertEquals(1, acceso.soloPropios(items, Item::dueno).size());
    }
}
