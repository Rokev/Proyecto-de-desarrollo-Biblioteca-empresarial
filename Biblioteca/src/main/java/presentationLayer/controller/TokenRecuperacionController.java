package presentationLayer.controller;

import bussinesLayer.dto.TokenRecuperacionDTO;
import bussinesLayer.service.TokenRecuperacionService;
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
@RequestMapping("/api/v1/tokens-recuperacion")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tokens de recuperacion", description = "Operaciones CRUD para la gestion de tokens de recuperacion de contrasena")
public class TokenRecuperacionController {

    private final TokenRecuperacionService tokenRecuperacionService;

    @PostMapping
    @Operation(summary = "Crear un token de recuperacion", description = "Registra un nuevo token de recuperacion de contrasena para un usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Token de recuperacion creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenRecuperacionDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, token ya existente o fecha de expiracion anterior a la creacion",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<TokenRecuperacionDTO> crearTokenRecuperacion(
            @Parameter(description = "Datos del token de recuperacion a crear", required = true)
            @Valid @RequestBody TokenRecuperacionDTO tokenRecuperacionDTO) {
        log.info("POST /api/v1/tokens-recuperacion - usuarioId: {}", tokenRecuperacionDTO.getUsuarioId());
        TokenRecuperacionDTO creado = tokenRecuperacionService.crearTokenRecuperacion(tokenRecuperacionDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar token de recuperacion por ID", description = "Obtiene la informacion de un token de recuperacion especifico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token de recuperacion encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenRecuperacionDTO.class))),
            @ApiResponse(responseCode = "404", description = "Token de recuperacion no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<TokenRecuperacionDTO> obtenerPorId(
            @Parameter(description = "ID del token de recuperacion", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/tokens-recuperacion/{}", id);
        return ResponseEntity.ok(tokenRecuperacionService.obtenerTokenRecuperacionPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todos los tokens de recuperacion", description = "Obtiene la lista completa de tokens de recuperacion registrados")
    @ApiResponse(responseCode = "200", description = "Lista de tokens de recuperacion obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TokenRecuperacionDTO.class))))
    public ResponseEntity<List<TokenRecuperacionDTO>> obtenerTodos() {
        log.debug("GET /api/v1/tokens-recuperacion");
        return ResponseEntity.ok(tokenRecuperacionService.obtenerTodosLosTokensRecuperacion());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar token de recuperacion", description = "Actualiza los datos de un token de recuperacion existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token de recuperacion actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenRecuperacionDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, token ya existente o fecha de expiracion anterior a la creacion",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Token de recuperacion o usuario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<TokenRecuperacionDTO> actualizar(
            @Parameter(description = "ID del token de recuperacion a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos del token de recuperacion", required = true)
            @Valid @RequestBody TokenRecuperacionDTO tokenRecuperacionDTO) {
        log.info("PUT /api/v1/tokens-recuperacion/{}", id);
        return ResponseEntity.ok(tokenRecuperacionService.actualizarTokenRecuperacion(id, tokenRecuperacionDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar token de recuperacion", description = "Elimina un token de recuperacion del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Token de recuperacion eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Token de recuperacion no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionado con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del token de recuperacion a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/tokens-recuperacion/{}", id);
        tokenRecuperacionService.eliminarTokenRecuperacion(id);
        return ResponseEntity.noContent().build();
    }
}