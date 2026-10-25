package presentationLayer.controller;
import org.springframework.web.bind.annotation.RequestParam;

import bussinesLayer.dto.UsuarioDTO;
import bussinesLayer.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import presentationLayer.advice.ApiError;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Usuarios", description = "Operaciones CRUD para la gestion de usuarios de la biblioteca")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    @Operation(summary = "Crear un usuario", description = "Registra un nuevo usuario con su rol y sede")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, faltantes o nombre de usuario, correo o documento ya registrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Rol o sede no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<UsuarioDTO> crearUsuario(
            @Parameter(description = "Datos del usuario a crear", required = true)
            @Valid @RequestBody UsuarioDTO usuarioDTO) {
        log.info("POST /api/v1/usuarios - nombreUsuario: {}", usuarioDTO.getNombreUsuario());
        UsuarioDTO creado = usuarioService.crearUsuario(usuarioDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuario por ID", description = "Obtiene la informacion de un usuario especifico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<UsuarioDTO> obtenerPorId(
            @Parameter(description = "ID del usuario", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/usuarios/{}", id);
        return ResponseEntity.ok(usuarioService.obtenerUsuarioPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todos los usuarios", description = "Obtiene la lista completa de usuarios registrados")
    @ApiResponse(responseCode = "200", description = "Lista de usuarios obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UsuarioDTO.class))))
    public ResponseEntity<List<UsuarioDTO>> obtenerTodos() {
        log.debug("GET /api/v1/usuarios");
        return ResponseEntity.ok(usuarioService.obtenerTodosLosUsuarios());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar usuario", description = "Actualiza los datos de un usuario existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o nombre de usuario, correo o documento ya registrado por otro usuario",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario, rol o sede no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<UsuarioDTO> actualizar(
            @Parameter(description = "ID del usuario a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos del usuario", required = true)
            @Valid @RequestBody UsuarioDTO usuarioDTO) {
        log.info("PUT /api/v1/usuarios/{}", id);
        return ResponseEntity.ok(usuarioService.actualizarUsuario(id, usuarioDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar usuario", description = "Elimina un usuario del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionado con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del usuario a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/usuarios/{}", id);
        usuarioService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/filtrar")
    @Operation(summary = "Filtrar usuarios", description = "Lista usuarios, bibliotecarios y repartidores filtrando por rol, sede, estado (activo) y suscripcion vigente")
    @ApiResponse(responseCode = "200", description = "Consulta realizada exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UsuarioDTO.class))))
    public ResponseEntity<List<UsuarioDTO>> filtrar(
            @Parameter(description = "Nombre del rol (por ejemplo BIBLIOTECARIO o REPARTIDOR)") @RequestParam(required = false) String rol,
            @Parameter(description = "ID de la sede") @RequestParam(required = false) Long sedeId,
            @Parameter(description = "true: cuentas activas; false: inactivas o suspendidas") @RequestParam(required = false) Boolean activo,
            @Parameter(description = "true: con suscripcion vigente; false: sin ella") @RequestParam(required = false) Boolean conSuscripcion) {
        return ResponseEntity.ok(usuarioService.filtrarUsuarios(rol, sedeId, activo, conSuscripcion));
    }

    @PostMapping("/{id}/desactivar")
    @Operation(summary = "Desactivar bibliotecario o repartidor", description = "Desactiva la cuenta sin borrar su historial ( 60)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operacion realizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioDTO.class))),
            @ApiResponse(responseCode = "400", description = "La operacion no es valida en el estado actual",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<UsuarioDTO> desactivarPersonal(
            @Parameter(description = "ID de la cuenta", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.desactivarPersonal(id));
    }

    @PostMapping("/{id}/suspender")
    @Operation(summary = "Suspender usuario", description = "Suspende la cuenta de un usuario y le notifica ( 64)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operacion realizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioDTO.class))),
            @ApiResponse(responseCode = "400", description = "La operacion no es valida en el estado actual",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<UsuarioDTO> suspender(
            @Parameter(description = "ID del usuario", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.suspenderUsuario(id));
    }

    @PostMapping("/{id}/reactivar")
    @Operation(summary = "Reactivar cuenta", description = "Reactiva un usuario suspendido o un bibliotecario/repartidor desactivado, conserva su historial y le notifica ( 65)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operacion realizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioDTO.class))),
            @ApiResponse(responseCode = "400", description = "La operacion no es valida en el estado actual",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<UsuarioDTO> reactivar(
            @Parameter(description = "ID de la cuenta", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.reactivarCuenta(id));
    }
}