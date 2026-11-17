package bussinesLayer.security;

import bussinesLayer.dto.UsuarioDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.util.ReflectionTestUtils;
import persistanceLayer.dao.SesionDAO;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Prueba el decodificador real: firma, vencimiento y que la sesion siga activa (RF-03). */
@DisplayName("JwtConfig")
class JwtConfigTest {

    private static final String SECRETO = "clave-de-pruebas-con-mas-de-32-caracteres-123";

    private final JwtConfig config = new JwtConfig();
    private SesionDAO sesionDAO;
    private JwtEncoder encoder;
    private JwtService jwtService;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() {
        sesionDAO = Mockito.mock(SesionDAO.class);
        encoder = config.jwtEncoder(SECRETO);
        jwtService = new JwtService(encoder);
        ReflectionTestUtils.setField(jwtService, "expiracionMinutos", 120L);
        decoder = config.jwtDecoder(SECRETO, sesionDAO);
    }

    private JwtService.TokenEmitido emitirToken() {
        UsuarioDTO usuario = new UsuarioDTO();
        usuario.setId(7L);
        usuario.setCorreo("ana@correo.com");
        return jwtService.generar(usuario, "CLIENTE");
    }

    @Test
    @DisplayName("la clave de menos de 32 caracteres se rechaza al arrancar")
    void claveCorta_lanzaIllegalState() {
        assertThrows(IllegalStateException.class, () -> config.jwtEncoder("corta"));
        assertThrows(IllegalStateException.class, () -> config.jwtDecoder("corta", sesionDAO));
    }

    @Test
    @DisplayName("token valido con sesion activa: se acepta")
    void tokenValidoConSesionActiva_seAcepta() {
        JwtService.TokenEmitido emitido = emitirToken();
        Mockito.when(sesionDAO.existsActivaByToken(emitido.jti())).thenReturn(true);

        Jwt jwt = decoder.decode(emitido.token());

        assertEquals("ana@correo.com", jwt.getSubject());
    }

    @Test
    @DisplayName("token valido pero sesion cerrada (logout, suspension o cambio de clave): se rechaza")
    void tokenConSesionCerrada_seRechaza() {
        JwtService.TokenEmitido emitido = emitirToken();
        Mockito.when(sesionDAO.existsActivaByToken(emitido.jti())).thenReturn(false);

        JwtException error = assertThrows(JwtException.class, () -> decoder.decode(emitido.token()));

        assertTrue(error.getMessage().contains("sesion"));
    }

    @Test
    @DisplayName("token vencido: se rechaza aunque la sesion siga marcada como activa")
    void tokenVencido_seRechaza() {
        String jti = UUID.randomUUID().toString();
        Mockito.when(sesionDAO.existsActivaByToken(jti)).thenReturn(true);
        Instant haceTresHoras = Instant.now().minus(3, ChronoUnit.HOURS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("libroya")
                .issuedAt(haceTresHoras)
                .expiresAt(haceTresHoras.plus(2, ChronoUnit.HOURS))
                .subject("ana@correo.com")
                .id(jti)
                .claim("uid", 7L)
                .claim("rol", "CLIENTE")
                .build();
        String vencido = encoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        assertThrows(JwtException.class, () -> decoder.decode(vencido));
    }

    @Test
    @DisplayName("token firmado con otra clave: se rechaza")
    void tokenConOtraClave_seRechaza() {
        JwtEncoder ajeno = config.jwtEncoder("otra-clave-completamente-distinta-de-32-o-mas-chars");
        JwtService servicioAjeno = new JwtService(ajeno);
        ReflectionTestUtils.setField(servicioAjeno, "expiracionMinutos", 120L);
        UsuarioDTO usuario = new UsuarioDTO();
        usuario.setId(1L);
        usuario.setCorreo("falso@correo.com");

        String falso = servicioAjeno.generar(usuario, "ADMIN").token();

        assertThrows(JwtException.class, () -> decoder.decode(falso));
    }

    @Test
    @DisplayName("token sin jti: no puede validarse contra una sesion y se rechaza")
    void tokenSinJti_seRechaza() {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("libroya")
                .issuedAt(ahora)
                .expiresAt(ahora.plus(1, ChronoUnit.HOURS))
                .subject("ana@correo.com")
                .claim("uid", 7L)
                .claim("rol", "ADMIN")
                .build();
        String sinJti = encoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        assertThrows(JwtException.class, () -> decoder.decode(sinJti));
    }
}
