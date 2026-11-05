package bussinesLayer.security;

import bussinesLayer.dto.UsuarioDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    /** Resultado de emitir un token: el texto firmado y los datos para guardar la sesion. */
    public record TokenEmitido(String token, String jti, LocalDateTime inicio, LocalDateTime expiracion) {
    }

    private final JwtEncoder jwtEncoder;

    @Value("${jwt.expiracion-minutos:120}")
    private long expiracionMinutos;

    public TokenEmitido generar(UsuarioDTO usuario, String rol) {
        LocalDateTime inicio = LocalDateTime.now();
        LocalDateTime expiracion = inicio.plusMinutes(expiracionMinutos);

        Instant emitidoEn = Instant.now();
        Instant venceEn = emitidoEn.plus(expiracionMinutos, ChronoUnit.MINUTES);
        String jti = UUID.randomUUID().toString();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("libroya")
                .issuedAt(emitidoEn)
                .expiresAt(venceEn)
                .subject(usuario.getCorreo())
                .id(jti)
                .claim("uid", usuario.getId())
                .claim("rol", rol)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenEmitido(token, jti, inicio, expiracion);
    }
}