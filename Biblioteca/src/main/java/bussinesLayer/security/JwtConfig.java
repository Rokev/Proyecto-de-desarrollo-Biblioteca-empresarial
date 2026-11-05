package bussinesLayer.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import persistanceLayer.dao.SesionDAO;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Configura la firma y validacion de los JWT con una clave secreta (HS256).
 */
@Configuration
public class JwtConfig {

    private SecretKey clave(String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("jwt.secret debe tener minimo 32 caracteres");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(@Value("${jwt.secret}") String secreto) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave(secreto)));
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${jwt.secret}") String secreto, SesionDAO sesionDAO) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave(secreto))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        // Ademas de firma y expiracion, el token solo vale si su sesion sigue activa (logout / suspension)
        OAuth2TokenValidator<Jwt> sesionActiva = jwt -> {
            String jti = jwt.getId();
            if (jti != null && sesionDAO.existsActivaByToken(jti)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "La sesion no esta activa", null));
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), sesionActiva));
        return decoder;
    }
}