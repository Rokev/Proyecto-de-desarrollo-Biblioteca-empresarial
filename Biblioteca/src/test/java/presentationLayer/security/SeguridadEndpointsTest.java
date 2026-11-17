package presentationLayer.security;

import com.app.Biblioteca.BibliotecaApplication;
import org.springframework.test.context.ContextConfiguration;
import bussinesLayer.dto.AuthResponseDTO;
import bussinesLayer.dto.PrestamoDTO;
import bussinesLayer.dto.ReservaLibroDTO;
import bussinesLayer.dto.SedeDTO;
import bussinesLayer.dto.UsuarioDTO;
import bussinesLayer.service.AuthService;
import bussinesLayer.service.PrestamoService;
import bussinesLayer.service.ReservaLibroService;
import bussinesLayer.service.SancionService;
import bussinesLayer.service.SedeService;
import bussinesLayer.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import presentationLayer.config.SecurityConfig;
import presentationLayer.controller.AuthController;
import presentationLayer.controller.PrestamoController;
import presentationLayer.controller.ReservaLibroController;
import presentationLayer.controller.SancionController;
import presentationLayer.controller.SedeController;
import presentationLayer.controller.UsuarioController;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba la seguridad real (SecurityConfig + AccesoUsuario + manejo de errores) sobre los controllers,
 * con los servicios simulados: sin token = 401, rol o dueño incorrecto = 403, caso permitido = 200.
 */
@WebMvcTest(controllers = {
        AuthController.class, PrestamoController.class, ReservaLibroController.class,
        SancionController.class, SedeController.class, UsuarioController.class})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, AccesoUsuario.class})
@TestPropertySource(properties = "cors.origenes=http://localhost:3000")
@DisplayName("Seguridad de los endpoints")
@ContextConfiguration(classes = BibliotecaApplication.class)
class SeguridadEndpointsTest {

    private static final long CLIENTE_ID = 5L;
    private static final long OTRO_ID = 6L;
    private static final long MI_SEDE = 1L;
    private static final long OTRA_SEDE = 2L;

    @Autowired private MockMvc mvc;

    @MockitoBean private JwtDecoder jwtDecoder; // la configuracion real lo necesita; los tests usan jwt() o lo simulan
    @MockitoBean private AuthService authService;
    @MockitoBean private PrestamoService prestamoService;
    @MockitoBean private ReservaLibroService reservaLibroService;
    @MockitoBean private SancionService sancionService;
    @MockitoBean private SedeService sedeService;
    @MockitoBean private UsuarioService usuarioService;

    /** Simula una peticion con un JWT valido de ese rol (uid y rol como en los tokens reales). */
    private RequestPostProcessor como(String rol, long uid) {
        return jwt()
                .jwt(j -> j.claim("uid", uid).claim("rol", rol).claim("jti", "jti-" + uid))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @BeforeEach
    void bibliotecarioConSede() {
        // AccesoUsuario consulta la sede de la cuenta; el usuario 5 se usa como bibliotecario de la sede 1
        UsuarioDTO u = new UsuarioDTO();
        u.setId(CLIENTE_ID);
        u.setSedeId(MI_SEDE);
        when(usuarioService.obtenerUsuarioPorId(CLIENTE_ID)).thenReturn(u);
    }

    private PrestamoDTO prestamo(long usuarioId, long sedeId) {
        PrestamoDTO p = new PrestamoDTO();
        p.setId(1L);
        p.setUsuarioId(usuarioId);
        p.setSedeRetiroId(sedeId);
        return p;
    }

    // ---------------------------------------------------------------- publico y sin sesion

    @Test
    @DisplayName("GET /sedes/activas es publico (RF-07)")
    void sedesActivas_sinToken_200() throws Exception {
        when(sedeService.obtenerSedesActivas()).thenReturn(List.of());

        mvc.perform(get("/api/v1/sedes/activas")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /sedes (todas, incluidas las inactivas) exige sesion")
    void sedesTodas_sinToken_401() throws Exception {
        mvc.perform(get("/api/v1/sedes")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("un endpoint protegido sin token responde 401")
    void prestamos_sinToken_401() throws Exception {
        mvc.perform(get("/api/v1/prestamos/usuario/5")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/prestamos").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("un token invalido o con sesion cerrada responde 401")
    void tokenInvalido_401() throws Exception {
        when(jwtDecoder.decode("token-malo")).thenThrow(new BadJwtException("La sesion no esta activa"));

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer token-malo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login es publico")
    void login_sinToken_200() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponseDTO());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@correo.com\",\"contrasena\":\"Clave1234\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /auth/login con credenciales incorrectas responde 401")
    void login_credencialesIncorrectas_401() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Credenciales invalidas"));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@correo.com\",\"contrasena\":\"mala\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /auth/me exige sesion y con sesion devuelve el perfil")
    void me() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").with(como("CLIENTE", CLIENTE_ID))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /auth/logout cierra la sesion identificada por el jti del token")
    void logout_cierraLaSesionDelToken() throws Exception {
        mvc.perform(post("/api/v1/auth/logout").with(como("CLIENTE", CLIENTE_ID)))
                .andExpect(status().isNoContent());

        verify(authService).logout("jti-5");
    }

    // ---------------------------------------------------------------- rol

    @Test
    @DisplayName("GET /usuarios es solo del administrador")
    void usuarios_soloAdmin() throws Exception {
        when(usuarioService.obtenerTodosLosUsuarios()).thenReturn(List.of());

        mvc.perform(get("/api/v1/usuarios").with(como("CLIENTE", CLIENTE_ID))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/usuarios").with(como("BIBLIOTECARIO", CLIENTE_ID))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/usuarios").with(como("ADMIN", 1L))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("un cliente no puede crear sedes (solo el administrador)")
    void crearSede_clienteProhibido() throws Exception {
        mvc.perform(post("/api/v1/sedes").with(como("CLIENTE", CLIENTE_ID))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un usuario con sesion puede consultar el detalle de una sede")
    void sedePorId_conSesion_200() throws Exception {
        when(sedeService.obtenerSedePorId(1L)).thenReturn(new SedeDTO());

        mvc.perform(get("/api/v1/sedes/1").with(como("CLIENTE", CLIENTE_ID))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("un cliente no puede eliminar prestamos y un repartidor no accede a prestamos")
    void prestamos_rolesNoPermitidos() throws Exception {
        mvc.perform(delete("/api/v1/prestamos/1").with(como("CLIENTE", CLIENTE_ID)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/prestamos/usuario/5").with(como("REPARTIDOR", CLIENTE_ID)))
                .andExpect(status().isForbidden());

        verify(prestamoService, never()).eliminarPrestamo(any());
    }

    // ---------------------------------------------------------------- propietario

    @Test
    @DisplayName("un cliente ve sus prestamos pero no los de otro usuario")
    void prestamosPorUsuario_soloLosPropios() throws Exception {
        mvc.perform(get("/api/v1/prestamos/usuario/5").with(como("CLIENTE", CLIENTE_ID))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/prestamos/usuario/6").with(como("CLIENTE", CLIENTE_ID))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un cliente consulta un prestamo propio pero no el de otro")
    void prestamoPorId_soloElPropio() throws Exception {
        when(prestamoService.obtenerPrestamoPorId(1L)).thenReturn(prestamo(CLIENTE_ID, MI_SEDE));
        when(prestamoService.obtenerPrestamoPorId(2L)).thenReturn(prestamo(OTRO_ID, MI_SEDE));

        mvc.perform(get("/api/v1/prestamos/1").with(como("CLIENTE", CLIENTE_ID))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/prestamos/2").with(como("CLIENTE", CLIENTE_ID))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un cliente ve sus sanciones (RF-13) pero no las de otro usuario")
    void sanciones_soloLasPropias() throws Exception {
        mvc.perform(get("/api/v1/sanciones/usuario/5").with(como("CLIENTE", CLIENTE_ID))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/sanciones/usuario/6").with(como("CLIENTE", CLIENTE_ID))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un cliente no puede cancelar la reserva de otro usuario, pero si la suya")
    void cancelarReservaLibro_soloLaPropia() throws Exception {
        ReservaLibroDTO reserva = new ReservaLibroDTO();
        reserva.setSedeId(MI_SEDE);
        when(reservaLibroService.obtenerReservaLibroPorId(1L)).thenReturn(reserva);
        when(reservaLibroService.cancelarReservaLibro(1L, CLIENTE_ID)).thenReturn(reserva);

        mvc.perform(post("/api/v1/reservas-libro/1/cancelar").param("usuarioId", "6")
                        .with(como("CLIENTE", CLIENTE_ID)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/reservas-libro/1/cancelar").param("usuarioId", "5")
                        .with(como("CLIENTE", CLIENTE_ID)))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- sede del bibliotecario (RF-38, RF-48 a RF-57)

    @Test
    @DisplayName("el bibliotecario consulta los prestamos de su sede pero no los de otra")
    void prestamosPorSede_bibliotecarioSoloSuSede() throws Exception {
        mvc.perform(get("/api/v1/prestamos/sede/1").with(como("BIBLIOTECARIO", CLIENTE_ID))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/prestamos/sede/2").with(como("BIBLIOTECARIO", CLIENTE_ID))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("el administrador consulta los prestamos de cualquier sede")
    void prestamosPorSede_adminTodas() throws Exception {
        mvc.perform(get("/api/v1/prestamos/sede/2").with(como("ADMIN", 1L))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("un bibliotecario sin sede asignada no accede a ninguna sede")
    void prestamosPorSede_bibliotecarioSinSede() throws Exception {
        UsuarioDTO sinSede = new UsuarioDTO();
        sinSede.setId(7L);
        when(usuarioService.obtenerUsuarioPorId(7L)).thenReturn(sinSede);

        mvc.perform(get("/api/v1/prestamos/sede/1").with(como("BIBLIOTECARIO", 7L))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un bibliotecario no puede eliminar un prestamo de otra sede y NO se llega a borrar")
    void eliminarPrestamo_deOtraSede_noSeBorra() throws Exception {
        when(prestamoService.obtenerPrestamoPorId(1L)).thenReturn(prestamo(OTRO_ID, OTRA_SEDE));

        mvc.perform(delete("/api/v1/prestamos/1").with(como("BIBLIOTECARIO", CLIENTE_ID)))
                .andExpect(status().isForbidden());

        verify(prestamoService, never()).eliminarPrestamo(any());
    }

    @Test
    @DisplayName("un bibliotecario no puede eliminar una reserva de libro de otra sede y NO se llega a borrar")
    void eliminarReservaLibro_deOtraSede_noSeBorra() throws Exception {
        ReservaLibroDTO reserva = new ReservaLibroDTO();
        reserva.setSedeId(OTRA_SEDE);
        when(reservaLibroService.obtenerReservaLibroPorId(1L)).thenReturn(reserva);

        mvc.perform(delete("/api/v1/reservas-libro/1").with(como("BIBLIOTECARIO", CLIENTE_ID)))
                .andExpect(status().isForbidden());

        verify(reservaLibroService, never()).eliminarReservaLibro(any());
    }

    @Test
    @DisplayName("un bibliotecario si puede eliminar un prestamo de su sede")
    void eliminarPrestamo_deSuSede_loBorra() throws Exception {
        when(prestamoService.obtenerPrestamoPorId(1L)).thenReturn(prestamo(OTRO_ID, MI_SEDE));

        mvc.perform(delete("/api/v1/prestamos/1").with(como("BIBLIOTECARIO", CLIENTE_ID)))
                .andExpect(status().isNoContent());

        verify(prestamoService).eliminarPrestamo(1L);
    }
}
