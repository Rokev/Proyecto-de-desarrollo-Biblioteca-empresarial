package bussinesLayer.security;

import bussinesLayer.dto.UsuarioDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Usa la criptografia real (HS256): se firma con JwtService y se verifica con un decodificador independiente. */
@DisplayName("JwtService")
class JwtServiceTest {

    private static final String SECRETO = "clave-de-pruebas-con-mas-de-32-caracteres-123";

    private JwtService service;
    private JwtDecoder decoder;
    private UsuarioDTO usuario;

    @BeforeEach
    void setUp() {
        service = new JwtService(new JwtConfig().jwtEncoder(SECRETO));
        ReflectionTestUtils.setField(service, "expiracionMinutos", 120L);
        decoder = decodificadorCon(SECRETO);

        usuario = new UsuarioDTO();
        usuario.setId(42L);
        usuario.setCorreo("maria@correo.com");
    }

    private JwtDecoder decodificadorCon(String secreto) {
        return NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Test
    @DisplayName("generar: el token lleva correo, id de usuario, rol y jti")
    void generar_incluyeLosClaimsEsperados() {
        JwtService.TokenEmitido emitido = service.generar(usuario, "CLIENTE");

        Jwt jwt = decoder.decode(emitido.token());

        assertEquals("maria@correo.com", jwt.getSubject());
        assertEquals(42L, ((Number) jwt.getClaim("uid")).longValue());
        assertEquals("CLIENTE", jwt.getClaimAsString("rol"));
        assertEquals("libroya", jwt.getClaimAsString("iss"));
        assertEquals(emitido.jti(), jwt.getId());
    }

    @Test
    @DisplayName("generar: el vencimiento del token y de la sesion respetan los minutos configurados")
    void generar_respetaLaExpiracionConfigurada() {
        JwtService.TokenEmitido emitido = service.generar(usuario, "ADMIN");

        Jwt jwt = decoder.decode(emitido.token());
        Duration vigenciaToken = Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt());
        Duration vigenciaSesion = Duration.between(emitido.inicio(), emitido.expiracion());

        assertEquals(120, vigenciaToken.toMinutes());
        assertEquals(120, vigenciaSesion.toMinutes());
    }

    @Test
    @DisplayName("generar: cambiar la expiracion configurada cambia la vigencia")
    void generar_conOtraExpiracion() {
        ReflectionTestUtils.setField(service, "expiracionMinutos", 15L);

        JwtService.TokenEmitido emitido = service.generar(usuario, "CLIENTE");

        assertEquals(15, Duration.between(emitido.inicio(), emitido.expiracion()).toMinutes());
    }

    @Test
    @DisplayName("generar: cada token tiene un jti distinto (cada inicio de sesion es revocable por separado)")
    void generar_jtiUnicoPorToken() {
        JwtService.TokenEmitido primero = service.generar(usuario, "CLIENTE");
        JwtService.TokenEmitido segundo = service.generar(usuario, "CLIENTE");

        assertNotNull(primero.jti());
        assertNotEquals(primero.jti(), segundo.jti());
        assertNotEquals(primero.token(), segundo.token());
    }

    @Test
    @DisplayName("generar: el inicio de sesion es ahora y la expiracion es posterior")
    void generar_inicioAhora() {
        LocalDateTime antes = LocalDateTime.now().minusSeconds(2);

        JwtService.TokenEmitido emitido = service.generar(usuario, "CLIENTE");

        assertTrue(emitido.inicio().isAfter(antes));
        assertTrue(emitido.expiracion().isAfter(emitido.inicio()));
    }

    @Test
    @DisplayName("un token firmado con otra clave no es aceptado")
    void token_conOtraClave_esRechazado() {
        JwtService.TokenEmitido emitido = service.generar(usuario, "ADMIN");

        JwtDecoder ajeno = decodificadorCon("otra-clave-completamente-distinta-de-32-o-mas-chars");

        assertThrows(JwtException.class, () -> ajeno.decode(emitido.token()));
    }

    @Test
    @DisplayName("un token alterado (por ejemplo cambiando el rol) no es aceptado")
    void token_alterado_esRechazado() {
        String token = service.generar(usuario, "CLIENTE").token();
        String[] partes = token.split("\\.");
        // Se reemplaza el contenido por otro y se conserva la firma original
        String contenidoFalso = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"maria@correo.com\",\"uid\":42,\"rol\":\"ADMIN\"}".getBytes(StandardCharsets.UTF_8));
        String alterado = partes[0] + "." + contenidoFalso + "." + partes[2];

        assertThrows(JwtException.class, () -> decoder.decode(alterado));
    }
}
