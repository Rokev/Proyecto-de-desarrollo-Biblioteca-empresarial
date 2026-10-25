package presentationLayer.controller;

import bussinesLayer.dto.SuscripcionDTO;
import bussinesLayer.service.SuscripcionService;
import bussinesLayer.dto.CompraSuscripcionDTO;
import org.springframework.web.bind.annotation.RequestParam;
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
@RequestMapping("/api/v1/suscripciones")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Suscripciones", description = "Compra, cancelacion y consulta de vigencia de suscripciones, mas operaciones CRUD administrativas")
public class SuscripcionController {

        private final SuscripcionService suscripcionService;

        @PostMapping
        @Operation(summary = "Crear una suscripcion (administrativo)", description = "Alta administrativa sin pago. Para que un usuario compre una suscripcion use POST /api/v1/suscripciones/comprar")
        @ApiResponses({
                @ApiResponse(responseCode = "201", description = "Compra iniciada, suscripcion pendiente por pagar",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = SuscripcionDTO.class))),
                @ApiResponse(responseCode = "400", description = "Datos invalidos, fechas inconsistentes, plan inactivo o el usuario ya tiene una suscripcion activa",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
                @ApiResponse(responseCode = "404", description = "Usuario o plan de suscripcion no encontrado",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
                @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
        })
        public ResponseEntity<SuscripcionDTO> crearSuscripcion(
                @Parameter(description = "Datos de la suscripcion a crear", required = true)
                @Valid @RequestBody SuscripcionDTO suscripcionDTO) {
        log.info("POST /api/v1/suscripciones - usuarioId: {}, planId: {}", suscripcionDTO.getUsuarioId(), suscripcionDTO.getPlanId());
        SuscripcionDTO creada = suscripcionService.crearSuscripcion(suscripcionDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
        }

        @PostMapping("/comprar")
        @Operation(summary = "Iniciar la compra de una suscripcion",
                description = "Deja la suscripcion en PENDIENTE_PAGO con fechas y valor calculados desde el plan. "
                        + "Los beneficios se activan cuando el pago se aprueba en POST /api/v1/pagos/procesar (concepto SUSCRIPCION).")
        @ApiResponses({
                @ApiResponse(responseCode = "201", description = "Suscripcion comprada y activada",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = SuscripcionDTO.class))),
                @ApiResponse(responseCode = "400", description = "Datos invalidos, plan inactivo, metodo de pago ajeno al usuario, compra pendiente o suscripcion vigente existente",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
                @ApiResponse(responseCode = "404", description = "Usuario o plan de suscripcion no encontrado",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
                @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
        })
        public ResponseEntity<SuscripcionDTO> comprarSuscripcion(
                @Parameter(description = "Datos de la compra", required = true)
                @Valid @RequestBody CompraSuscripcionDTO compra) {
        log.info("POST /api/v1/suscripciones/comprar - usuarioId: {}, planId: {}", compra.getUsuarioId(), compra.getPlanId());
        SuscripcionDTO comprada = suscripcionService.comprarSuscripcion(compra);
        return ResponseEntity.status(HttpStatus.CREATED).body(comprada);
        }       
        @PostMapping("/{id}/cancelar")
        @Operation(summary = "Cancelar una suscripcion",
                description = "Cancela la suscripcion y desactiva la renovacion automatica. "
                        + "Los beneficios se mantienen hasta la fechaFin del periodo vigente.")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Suscripcion cancelada; conserva beneficios hasta fechaFin",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = SuscripcionDTO.class))),
                @ApiResponse(responseCode = "400", description = "La suscripcion no pertenece al usuario, ya esta cancelada, no esta activa o ya finalizo",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
                @ApiResponse(responseCode = "404", description = "Suscripcion no encontrada",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
        })
        public ResponseEntity<SuscripcionDTO> cancelarSuscripcion(
                @Parameter(description = "ID de la suscripcion a cancelar", required = true, example = "1")
                @PathVariable Long id,
                @Parameter(description = "ID del usuario dueño de la suscripcion", required = true, example = "1")
                @RequestParam Long usuarioId) {
        log.info("POST /api/v1/suscripciones/{}/cancelar - usuarioId: {}", id, usuarioId);
        return ResponseEntity.ok(suscripcionService.cancelarSuscripcion(id, usuarioId));
        }       
        @GetMapping("/usuario/{usuarioId}/vigente")
        @Operation(summary = "Consultar la suscripcion vigente de un usuario",
                description = "Devuelve la suscripcion ACTIVA, o CANCELADA con fechaFin aun por venir. Responde 404 si el usuario no tiene suscripcion vigente.")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Suscripcion vigente encontrada",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = SuscripcionDTO.class))),
                @ApiResponse(responseCode = "404", description = "Usuario inexistente o sin suscripcion vigente",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
        })
        public ResponseEntity<SuscripcionDTO> obtenerVigente(
                @Parameter(description = "ID del usuario", required = true, example = "1")
                @PathVariable Long usuarioId) {
        log.debug("GET /api/v1/suscripciones/usuario/{}/vigente", usuarioId);
        return ResponseEntity.ok(suscripcionService.obtenerSuscripcionVigente(usuarioId));
        }


        @GetMapping("/{id}")
        @Operation(summary = "Buscar suscripcion por ID", description = "Obtiene la informacion de una suscripcion especifica")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Suscripcion encontrada",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = SuscripcionDTO.class))),
                @ApiResponse(responseCode = "404", description = "Suscripcion no encontrada",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
        })
        public ResponseEntity<SuscripcionDTO> obtenerPorId(
                @Parameter(description = "ID de la suscripcion", required = true, example = "1")
                @PathVariable Long id) {
        log.debug("GET /api/v1/suscripciones/{}", id);
        return ResponseEntity.ok(suscripcionService.obtenerSuscripcionPorId(id));
        }

        @GetMapping
        @Operation(summary = "Listar todas las suscripciones", description = "Obtiene la lista completa de suscripciones registradas")
        @ApiResponse(responseCode = "200", description = "Lista de suscripciones obtenida exitosamente",
                content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SuscripcionDTO.class))))
        public ResponseEntity<List<SuscripcionDTO>> obtenerTodos() {
        log.debug("GET /api/v1/suscripciones");
        return ResponseEntity.ok(suscripcionService.obtenerTodasLasSuscripciones());
        }

        @PutMapping("/{id}")
        @Operation(summary = "Actualizar suscripcion", description = "Actualiza los datos de una suscripcion existente")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Suscripcion actualizada exitosamente",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = SuscripcionDTO.class))),
                @ApiResponse(responseCode = "400", description = "Datos invalidos, fechas inconsistentes, plan inactivo o intento de cambiar el usuario",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
                @ApiResponse(responseCode = "404", description = "Suscripcion, usuario o plan de suscripcion no encontrado",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
        })
        public ResponseEntity<SuscripcionDTO> actualizar(
                @Parameter(description = "ID de la suscripcion a actualizar", required = true, example = "1")
                @PathVariable Long id,
                @Parameter(description = "Nuevos datos de la suscripcion", required = true)
                @Valid @RequestBody SuscripcionDTO suscripcionDTO) {
        log.info("PUT /api/v1/suscripciones/{}", id);
        return ResponseEntity.ok(suscripcionService.actualizarSuscripcion(id, suscripcionDTO));
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Eliminar suscripcion", description = "Elimina una suscripcion del sistema")
        @ApiResponses({
                @ApiResponse(responseCode = "204", description = "Suscripcion eliminada exitosamente"),
                @ApiResponse(responseCode = "404", description = "Suscripcion no encontrada",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
                @ApiResponse(responseCode = "409", description = "No se puede eliminar porque esta relacionada con otros datos",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
        })
        public ResponseEntity<Void> eliminar(
                @Parameter(description = "ID de la suscripcion a eliminar", required = true, example = "1")
                @PathVariable Long id) {
        log.info("DELETE /api/v1/suscripciones/{}", id);
        suscripcionService.eliminarSuscripcion(id);
        return ResponseEntity.noContent().build();
        }
}