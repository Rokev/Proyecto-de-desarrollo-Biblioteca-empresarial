package presentationLayer.controller;
import org.springframework.web.bind.annotation.RequestParam;

import bussinesLayer.dto.PlanSuscripcionDTO;
import bussinesLayer.service.PlanSuscripcionService;
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
@RequestMapping("/api/v1/planes-suscripcion")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Planes de suscripcion", description = "Operaciones CRUD para la gestion de planes de suscripcion")
public class PlanSuscripcionController {

    private final PlanSuscripcionService planSuscripcionService;

    @PostMapping
    @Operation(summary = "Crear un plan de suscripcion", description = "Registra un nuevo plan de suscripcion")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Plan de suscripcion creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlanSuscripcionDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos, faltantes o nombre de plan ya registrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PlanSuscripcionDTO> crearPlanSuscripcion(
            @Parameter(description = "Datos del plan de suscripcion a crear", required = true)
            @Valid @RequestBody PlanSuscripcionDTO planSuscripcionDTO) {
        log.info("POST /api/v1/planes-suscripcion - nombre: {}", planSuscripcionDTO.getNombre());
        PlanSuscripcionDTO creado = planSuscripcionService.crearPlanSuscripcion(planSuscripcionDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar plan de suscripcion por ID", description = "Obtiene la informacion de un plan de suscripcion especifico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan de suscripcion encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlanSuscripcionDTO.class))),
            @ApiResponse(responseCode = "404", description = "Plan de suscripcion no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PlanSuscripcionDTO> obtenerPorId(
            @Parameter(description = "ID del plan de suscripcion", required = true, example = "1")
            @PathVariable Long id) {
        log.debug("GET /api/v1/planes-suscripcion/{}", id);
        return ResponseEntity.ok(planSuscripcionService.obtenerPlanSuscripcionPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todos los planes de suscripcion", description = "Obtiene la lista completa de planes de suscripcion registrados")
    @ApiResponse(responseCode = "200", description = "Lista de planes de suscripcion obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PlanSuscripcionDTO.class))))
    public ResponseEntity<List<PlanSuscripcionDTO>> obtenerTodos() {
        log.debug("GET /api/v1/planes-suscripcion");
        return ResponseEntity.ok(planSuscripcionService.obtenerTodosLosPlanesSuscripcion());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar plan de suscripcion", description = "Actualiza los datos de un plan de suscripcion existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan de suscripcion actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlanSuscripcionDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o nombre de plan ya registrado por otro plan",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Plan de suscripcion no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PlanSuscripcionDTO> actualizar(
            @Parameter(description = "ID del plan de suscripcion a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Nuevos datos del plan de suscripcion", required = true)
            @Valid @RequestBody PlanSuscripcionDTO planSuscripcionDTO) {
        log.info("PUT /api/v1/planes-suscripcion/{}", id);
        return ResponseEntity.ok(planSuscripcionService.actualizarPlanSuscripcion(id, planSuscripcionDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar plan de suscripcion", description = "Elimina un plan de suscripcion del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Plan de suscripcion eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Plan de suscripcion no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionado con otros datos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del plan de suscripcion a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        log.info("DELETE /api/v1/planes-suscripcion/{}", id);
        planSuscripcionService.eliminarPlanSuscripcion(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/desactivar")
    @Operation(summary = "Desactivar plan de suscripcion", description = "Impide nuevas compras del plan; las suscripciones vigentes no se afectan (RF-72)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operacion realizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlanSuscripcionDTO.class))),
            @ApiResponse(responseCode = "400", description = "La operacion no es valida en el estado actual",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PlanSuscripcionDTO> desactivar(
            @Parameter(description = "ID del plan", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(planSuscripcionService.desactivarPlan(id));
    }

    @PostMapping("/{id}/reactivar")
    @Operation(summary = "Reactivar plan de suscripcion", description = "Vuelve a permitir nuevas compras del plan (RF-73)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operacion realizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlanSuscripcionDTO.class))),
            @ApiResponse(responseCode = "400", description = "La operacion no es valida en el estado actual",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PlanSuscripcionDTO> reactivar(
            @Parameter(description = "ID del plan", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(planSuscripcionService.reactivarPlan(id));
    }
}