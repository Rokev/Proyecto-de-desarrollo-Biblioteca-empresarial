package presentationLayer.controller;

import bussinesLayer.dto.ActualizarPerfilDTO;
import bussinesLayer.dto.AuthResponseDTO;
import bussinesLayer.dto.LoginRequestDTO;
import bussinesLayer.dto.RecuperarContrasenaDTO;
import bussinesLayer.dto.RegistroDTO;
import bussinesLayer.dto.RestablecerContrasenaDTO;
import bussinesLayer.dto.UsuarioDTO;
import bussinesLayer.dto.CambiarContrasenaDTO;
import bussinesLayer.service.AuthService;
import bussinesLayer.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacion", description = "Inicio de sesion, registro, cierre de sesion y recuperacion de contrasena")
public class AuthController {

    @Value("${app.recuperacion.mostrar-token:false}")
    private boolean mostrarToken;
    private final AuthService authService;
    private final UsuarioService usuarioService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion", description = "Valida correo y contrasena y devuelve un token JWT")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/registro")
    @Operation(summary = "Registrar cliente", description = "Crea una cuenta con rol CLIENTE")
    public ResponseEntity<UsuarioDTO> registro(@Valid @RequestBody RegistroDTO registro) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(registro));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesion", description = "Invalida el token con el que se hace la peticion")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        authService.logout(jwt.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/recuperar")
    @Operation(summary = "Solicitar recuperacion de contrasena",
            description = "Genera un token de 24 horas para restablecer la contrasena. "
                    + "En modo simulacion (app.recuperacion.mostrar-token=true) el token viene en la respuesta.")
    public ResponseEntity<Map<String, String>> recuperar(@Valid @RequestBody RecuperarContrasenaDTO dto) {
        String token = authService.solicitarRecuperacion(dto.getCorreo());
        Map<String, String> respuesta = new java.util.LinkedHashMap<>();
        respuesta.put("mensaje", "Si el correo esta registrado, se genero un token de recuperacion");
        if (mostrarToken) {
            respuesta.put("token", token);
        }
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/restablecer")
    @Operation(summary = "Restablecer contrasena", description = "Cambia la contrasena usando el token de recuperacion")
    public ResponseEntity<Map<String, String>> restablecer(@Valid @RequestBody RestablecerContrasenaDTO dto) {
        authService.restablecerContrasena(dto);
        return ResponseEntity.ok(Map.of("mensaje", "Contrasena actualizada correctamente"));
    }

    @GetMapping("/me")
    @Operation(summary = "Mi perfil", description = "Devuelve los datos del usuario autenticado")
    public ResponseEntity<UsuarioDTO> me(@AuthenticationPrincipal Jwt jwt) {
        Number uid = jwt.getClaim("uid");
        return ResponseEntity.ok(usuarioService.obtenerUsuarioPorId(uid.longValue()));
    }

    @PutMapping("/me")
    @Operation(summary = "Actualizar mi perfil", description = "Actualiza nombre, apellido, telefono y direccion del usuario autenticado")
    public ResponseEntity<UsuarioDTO> actualizarMiPerfil(@AuthenticationPrincipal Jwt jwt,
                                                        @Valid @RequestBody ActualizarPerfilDTO perfil) {
        Number uid = jwt.getClaim("uid");
        return ResponseEntity.ok(usuarioService.actualizarPerfil(uid.longValue(), perfil));
    }

    @PutMapping("/me/contrasena")
    @Operation(summary = "Cambiar mi contrasena", description = "Requiere la contrasena actual. Cierra todas las sesiones del usuario")
    public ResponseEntity<Map<String, String>> cambiarContrasena(@AuthenticationPrincipal Jwt jwt,
                                                                @Valid @RequestBody CambiarContrasenaDTO dto) {
        Number uid = jwt.getClaim("uid");
        authService.cambiarContrasena(uid.longValue(), dto);
        return ResponseEntity.ok(Map.of("mensaje", "Contrasena actualizada. Inicia sesion de nuevo"));
    }
}