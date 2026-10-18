package presentationLayer.controller;

import bussinesLayer.dto.RolDTO;
import bussinesLayer.service.RolService;
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
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Roles", description = "Operaciones CRUD para la gestion de roles de usuario")
public class RolController {

    private final RolService rolService;

    @PostMapping
    @Operation(summary = "Crear un rol", description = "Registra un nuevo rol de usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rol creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RolDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, faltantes o nombre de rol ya registrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<RolDTO> crearRol(
            @Parameter(description = "Datos del rol a crear", required = true)
            @Valid @RequestBody RolDTO rolDTO) {
        log.info("POST /api/v1/roles - nombre: {}", rolDTO.getNombre());
        RolDTO creado = rolService.crearRol(rolDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar rol por ID", description = "Obtiene la informacion de un rol especifico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rol encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RolDTO.class))),
            @ApiResponse(responseCode = "404", description = "Rol no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<RolDTO> obtenerPorId(
            @Parameter(description = "ID del rol", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/roles/{}", id);
        return ResponseEntity.ok(rolService.obtenerRolPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todos los roles", description = "Obtiene la lista completa de roles registrados")
    @ApiResponse(responseCode = "200", description = "Lista de roles obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = RolDTO.class))))
    public ResponseEntity<List<RolDTO>> obtenerTodos() {
        log.debug("GET /api/v1/roles");
        return ResponseEntity.ok(rolService.obtenerTodosLosRoles());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar rol", description = "Actualiza los datos de un rol existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rol actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RolDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o nombre de rol ya registrado por otro rol",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Rol no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<RolDTO> actualizar(
            @Parameter(description = "ID del rol a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos del rol", required = true)
            @Valid @RequestBody RolDTO rolDTO) {
        log.info("PUT /api/v1/roles/{}", id);
        return ResponseEntity.ok(rolService.actualizarRol(id, rolDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar rol", description = "Elimina un rol del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Rol eliminado exitosamente"),
            @ApiResponse(responseCode = "400", description = "No se puede eliminar el rol porque tiene usuarios asociados",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Rol no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionado con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del rol a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/roles/{}", id);
        rolService.eliminarRol(id);
        return ResponseEntity.noContent().build();
    }
}