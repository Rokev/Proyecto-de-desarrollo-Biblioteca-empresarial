package bussinesLayer.service.impl;

import bussinesLayer.dto.AuthResponseDTO;
import bussinesLayer.dto.CambiarContrasenaDTO;
import bussinesLayer.dto.LoginRequestDTO;
import bussinesLayer.dto.RegistroDTO;
import bussinesLayer.dto.RestablecerContrasenaDTO;
import bussinesLayer.dto.RolDTO;
import bussinesLayer.dto.SesionDTO;
import bussinesLayer.dto.TokenRecuperacionDTO;
import bussinesLayer.dto.UsuarioDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.security.JwtService;
import bussinesLayer.service.CorreoService;
import bussinesLayer.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import persistanceLayer.dao.RolDAO;
import persistanceLayer.dao.SesionDAO;
import persistanceLayer.dao.TokenRecuperacionDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl")
class AuthServiceImplTest {

    private static final Long USUARIO_ID = 7L;
    private static final Long ROL_ID = 3L;
    private static final String CORREO = "ana@correo.com";
    private static final String HASH = "$2a$10$hashDeEjemplo";

    @Mock private UsuarioDAO usuarioDAO;
    @Mock private RolDAO rolDAO;
    @Mock private SesionDAO sesionDAO;
    @Mock private TokenRecuperacionDAO tokenRecuperacionDAO;
    @Mock private UsuarioService usuarioService;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CorreoService correoService;

    @InjectMocks
    private AuthServiceImpl service;

    private UsuarioDTO usuario;

    @BeforeEach
    void setUp() {
        usuario = new UsuarioDTO();
        usuario.setId(USUARIO_ID);
        usuario.setNombre("Ana");
        usuario.setCorreo(CORREO);
        usuario.setActivo(true);
        usuario.setRolId(ROL_ID);
    }

    private LoginRequestDTO credenciales(String correo, String contrasena) {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setCorreo(correo);
        dto.setContrasena(contrasena);
        return dto;
    }

    // ---------------------------------------------------------------- login

    @Test
    @DisplayName("login: credenciales correctas devuelven el token y guardan la sesion activa")
    void login_correcto_creaSesionYDevuelveToken() {
        LocalDateTime inicio = LocalDateTime.now();
        LocalDateTime expira = inicio.plusMinutes(120);
        when(usuarioDAO.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));
        when(usuarioDAO.findContrasenaHashById(USUARIO_ID)).thenReturn(Optional.of(HASH));
        when(passwordEncoder.matches("Clave1234", HASH)).thenReturn(true);
        when(rolDAO.findById(ROL_ID)).thenReturn(Optional.of(new RolDTO(ROL_ID, " cliente ", "Cliente")));
        when(jwtService.generar(usuario, "CLIENTE"))
                .thenReturn(new JwtService.TokenEmitido("token-firmado", "jti-1", inicio, expira));

        AuthResponseDTO respuesta = service.login(credenciales(CORREO, "Clave1234"));

        assertEquals("token-firmado", respuesta.getToken());
        assertEquals("Bearer", respuesta.getTipo());
        assertEquals(USUARIO_ID, respuesta.getUsuarioId());
        assertEquals("Ana", respuesta.getNombre());
        assertEquals("CLIENTE", respuesta.getRol());
        assertEquals(expira, respuesta.getExpiracion());

        ArgumentCaptor<SesionDTO> sesion = ArgumentCaptor.forClass(SesionDTO.class);
        verify(sesionDAO).save(sesion.capture());
        assertEquals("jti-1", sesion.getValue().getToken());
        assertTrue(sesion.getValue().getActiva());
        assertEquals(USUARIO_ID, sesion.getValue().getUsuarioId());
        assertEquals(expira, sesion.getValue().getFechaExpiracion());
    }

    @Test
    @DisplayName("login: el correo se recorta antes de buscarlo")
    void login_recortaEspaciosDelCorreo() {
        when(usuarioDAO.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> service.login(credenciales("  " + CORREO + " ", "x")));

        verify(usuarioDAO).findByCorreoIgnoreCase(CORREO);
    }

    @Test
    @DisplayName("login: correo inexistente lanza BadCredentialsException sin comprobar contrasenas")
    void login_correoInexistente_lanzaBadCredentials() {
        when(usuarioDAO.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> service.login(credenciales(CORREO, "Clave1234")));

        verifyNoInteractions(passwordEncoder, jwtService, sesionDAO);
    }

    @Test
    @DisplayName("login: contrasena incorrecta lanza BadCredentialsException y no emite token")
    void login_contrasenaIncorrecta_lanzaBadCredentials() {
        when(usuarioDAO.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));
        when(usuarioDAO.findContrasenaHashById(USUARIO_ID)).thenReturn(Optional.of(HASH));
        when(passwordEncoder.matches("mala", HASH)).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> service.login(credenciales(CORREO, "mala")));

        verifyNoInteractions(jwtService, sesionDAO);
    }

    @Test
    @DisplayName("login: usuario sin hash de contrasena lanza BadCredentialsException")
    void login_sinHash_lanzaBadCredentials() {
        when(usuarioDAO.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));
        when(usuarioDAO.findContrasenaHashById(USUARIO_ID)).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> service.login(credenciales(CORREO, "Clave1234")));

        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("login: cuenta suspendida o desactivada lanza DisabledException aunque la clave sea correcta")
    void login_cuentaInactiva_lanzaDisabled() {
        usuario.setActivo(false);
        when(usuarioDAO.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));
        when(usuarioDAO.findContrasenaHashById(USUARIO_ID)).thenReturn(Optional.of(HASH));
        when(passwordEncoder.matches("Clave1234", HASH)).thenReturn(true);

        assertThrows(DisabledException.class, () -> service.login(credenciales(CORREO, "Clave1234")));

        verifyNoInteractions(jwtService, sesionDAO);
    }

    @Test
    @DisplayName("login: si el rol del usuario ya no existe lanza ResourceNotFoundException")
    void login_rolInexistente_lanzaNotFound() {
        when(usuarioDAO.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));
        when(usuarioDAO.findContrasenaHashById(USUARIO_ID)).thenReturn(Optional.of(HASH));
        when(passwordEncoder.matches("Clave1234", HASH)).thenReturn(true);
        when(rolDAO.findById(ROL_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.login(credenciales(CORREO, "Clave1234")));

        verifyNoInteractions(jwtService);
    }

    // ---------------------------------------------------------------- registrar 

    @Test
    @DisplayName("registrar: crea siempre un usuario con rol CLIENTE y cuenta activa")
    void registrar_asignaRolClienteYActivo() {
        when(rolDAO.findByNombreIgnoreCase("CLIENTE")).thenReturn(Optional.of(new RolDTO(9L, "CLIENTE", "Cliente")));
        UsuarioDTO creado = new UsuarioDTO();
        creado.setId(55L);
        when(usuarioService.crearUsuario(any(UsuarioDTO.class))).thenReturn(creado);

        RegistroDTO registro = new RegistroDTO();
        registro.setNombre("Luis");
        registro.setApellido("Gomez");
        registro.setDocumento("123456");
        registro.setNombreUsuario("luis99");
        registro.setCorreo("luis@correo.com");
        registro.setContrasena("Clave1234");

        UsuarioDTO resultado = service.registrar(registro);

        assertSame(creado, resultado);
        ArgumentCaptor<UsuarioDTO> enviado = ArgumentCaptor.forClass(UsuarioDTO.class);
        verify(usuarioService).crearUsuario(enviado.capture());
        assertEquals(9L, enviado.getValue().getRolId());
        assertTrue(enviado.getValue().getActivo());
        assertEquals("luis99", enviado.getValue().getNombreUsuario());
        assertEquals("luis@correo.com", enviado.getValue().getCorreo());
        assertEquals("Clave1234", enviado.getValue().getContrasena()); // el cifrado ocurre en UsuarioService
    }

    @Test
    @DisplayName("registrar: si el rol CLIENTE no existe lanza ResourceNotFoundException")
    void registrar_sinRolCliente_lanzaNotFound() {
        when(rolDAO.findByNombreIgnoreCase("CLIENTE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.registrar(new RegistroDTO()));

        verifyNoInteractions(usuarioService);
    }

    // ---------------------------------------------------------------- logout

    @Test
    @DisplayName("logout: cierra la sesion identificada por el jti")
    void logout_cierraLaSesion() {
        service.logout("jti-1");

        verify(sesionDAO).cerrarSesion(eq("jti-1"), any(LocalDateTime.class));
    }

    // ---------------------------------------------------------------- recuperar

    @Test
    @DisplayName("solicitarRecuperacion: crea un token que vence en 24 horas y lo envia por correo")
    void solicitarRecuperacion_usuarioExiste_guardaTokenYEnviaCorreo() {
        when(usuarioDAO.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));

        service.solicitarRecuperacion(" " + CORREO + " ");

        ArgumentCaptor<TokenRecuperacionDTO> guardado = ArgumentCaptor.forClass(TokenRecuperacionDTO.class);
        verify(tokenRecuperacionDAO).invalidarTokensDeUsuario(USUARIO_ID);
        verify(tokenRecuperacionDAO).save(guardado.capture());
        TokenRecuperacionDTO token = guardado.getValue();
        assertNotNull(token.getToken());
        assertFalse(token.getUsado());
        assertEquals(USUARIO_ID, token.getUsuarioId());
        long horas = Duration.between(token.getFechaCreacion(), token.getFechaExpiracion()).toHours();
        assertEquals(24, horas);
        verify(correoService).enviarRecuperacionContrasena(CORREO, token.getToken());
    }

    @Test
    @DisplayName("solicitarRecuperacion: un correo no registrado no genera token, ni correo, ni error")
    void solicitarRecuperacion_correoDesconocido_noHaceNada() {
        when(usuarioDAO.findByCorreoIgnoreCase("nadie@correo.com")).thenReturn(Optional.empty());

        service.solicitarRecuperacion("nadie@correo.com");

        verifyNoInteractions(tokenRecuperacionDAO, correoService);
    }

    @Test
    @DisplayName("restablecerContrasena: cambia la clave, consume el token y cierra las sesiones")
    void restablecerContrasena_valido() {
        TokenRecuperacionDTO token = new TokenRecuperacionDTO();
        token.setToken("tok-1");
        token.setUsuarioId(USUARIO_ID);
        when(tokenRecuperacionDAO.findByTokenAndUsadoFalse("tok-1")).thenReturn(Optional.of(token));
        when(tokenRecuperacionDAO.consumirToken(eq("tok-1"), any(LocalDateTime.class))).thenReturn(1);
        when(passwordEncoder.encode("Nueva1234")).thenReturn("HASH-NUEVO");

        service.restablecerContrasena(new RestablecerContrasenaDTO("tok-1", "Nueva1234"));

        verify(usuarioDAO).updateContrasena(USUARIO_ID, "HASH-NUEVO");
        verify(sesionDAO).cerrarSesionesDeUsuario(eq(USUARIO_ID), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("restablecerContrasena: token inexistente o ya usado lanza BusinessValidationException")
    void restablecerContrasena_tokenInvalido() {
        when(tokenRecuperacionDAO.findByTokenAndUsadoFalse("malo")).thenReturn(Optional.empty());

        assertThrows(BusinessValidationException.class,
                () -> service.restablecerContrasena(new RestablecerContrasenaDTO("malo", "Nueva1234")));

        verifyNoInteractions(usuarioDAO, sesionDAO);
    }

    @Test
    @DisplayName("restablecerContrasena: token vencido (no se puede consumir) no cambia la clave")
    void restablecerContrasena_tokenVencido() {
        TokenRecuperacionDTO token = new TokenRecuperacionDTO();
        token.setUsuarioId(USUARIO_ID);
        when(tokenRecuperacionDAO.findByTokenAndUsadoFalse("viejo")).thenReturn(Optional.of(token));
        when(tokenRecuperacionDAO.consumirToken(eq("viejo"), any(LocalDateTime.class))).thenReturn(0);

        assertThrows(BusinessValidationException.class,
                () -> service.restablecerContrasena(new RestablecerContrasenaDTO("viejo", "Nueva1234")));

        verify(usuarioDAO, never()).updateContrasena(any(), anyString());
        verifyNoInteractions(sesionDAO);
    }

    // ---------------------------------------------------------------- cambiarContrasena

    @Test
    @DisplayName("cambiarContrasena: con la clave actual correcta guarda el hash nuevo y cierra todas las sesiones")
    void cambiarContrasena_valido() {
        when(usuarioDAO.findContrasenaHashById(USUARIO_ID)).thenReturn(Optional.of(HASH));
        when(passwordEncoder.matches("Vieja1234", HASH)).thenReturn(true);
        when(passwordEncoder.encode("Nueva1234")).thenReturn("HASH-NUEVO");

        service.cambiarContrasena(USUARIO_ID, new CambiarContrasenaDTO("Vieja1234", "Nueva1234"));

        verify(usuarioDAO).updateContrasena(USUARIO_ID, "HASH-NUEVO");
        verify(sesionDAO).cerrarSesionesDeUsuario(eq(USUARIO_ID), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("cambiarContrasena: clave actual incorrecta lanza BusinessValidationException")
    void cambiarContrasena_actualIncorrecta() {
        when(usuarioDAO.findContrasenaHashById(USUARIO_ID)).thenReturn(Optional.of(HASH));
        when(passwordEncoder.matches("mala", HASH)).thenReturn(false);

        assertThrows(BusinessValidationException.class,
                () -> service.cambiarContrasena(USUARIO_ID, new CambiarContrasenaDTO("mala", "Nueva1234")));

        verify(usuarioDAO, never()).updateContrasena(any(), anyString());
        verifyNoInteractions(sesionDAO);
    }

    @Test
    @DisplayName("cambiarContrasena: la nueva clave igual a la actual lanza BusinessValidationException")
    void cambiarContrasena_igualALaActual() {
        when(usuarioDAO.findContrasenaHashById(USUARIO_ID)).thenReturn(Optional.of(HASH));
        when(passwordEncoder.matches("Clave1234", HASH)).thenReturn(true);

        assertThrows(BusinessValidationException.class,
                () -> service.cambiarContrasena(USUARIO_ID, new CambiarContrasenaDTO("Clave1234", "Clave1234")));

        verify(usuarioDAO, never()).updateContrasena(any(), anyString());
    }

    @Test
    @DisplayName("cambiarContrasena: usuario inexistente lanza ResourceNotFoundException")
    void cambiarContrasena_usuarioInexistente() {
        when(usuarioDAO.findContrasenaHashById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.cambiarContrasena(99L, new CambiarContrasenaDTO("a", "Nueva1234")));

        verifyNoInteractions(passwordEncoder);
    }
}
