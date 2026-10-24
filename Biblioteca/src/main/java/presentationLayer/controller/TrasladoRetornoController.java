package presentationLayer.controller;

import bussinesLayer.dto.TrasladoRetornoDTO;
import bussinesLayer.service.TrasladoRetornoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import presentationLayer.advice.ApiError;

import java.util.List;

@RestController
@RequestMapping("/api/v1/traslados-retorno")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Traslados de retorno", description = "Regreso de los libros devueltos en otra sede a su sede de origen")
public class TrasladoRetornoController {

    private final TrasladoRetornoService trasladoService;

    @GetMapping("/{id}")
    @Operation(summary = "Buscar traslado por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Traslado encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TrasladoRetornoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Traslado no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<TrasladoRetornoDTO> obtenerPorId(
            @Parameter(description = "ID del traslado", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(trasladoService.obtenerTrasladoPorId(id));
    }

    @GetMapping("/sede-destino/{sedeId}")
    @Operation(summary = "Traslados que vienen hacia una sede (sede de origen)",
            description = "Para que la sede de origen vea los libros que le estan regresando. Se puede filtrar por estado (EN_TRANSITO, RECIBIDO).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de traslados",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TrasladoRetornoDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Estado invalido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Sede no encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<TrasladoRetornoDTO>> obtenerHaciaSede(
            @Parameter(description = "ID de la sede de origen", required = true, example = "1") @PathVariable Long sedeId,
            @Parameter(description = "Filtrar por estado (opcional)", example = "EN_TRANSITO") @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(trasladoService.obtenerTrasladosHaciaSede(sedeId, estado));
    }

    @PostMapping("/{id}/recibir")
    @Operation(summary = "Recibir un ejemplar en la sede de origen (bibliotecario)",
            description = "El ejemplar queda disponible. Si hay un usuario con reserva en espera, se le aparta y se le avisa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Traslado recibido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TrasladoRetornoDTO.class))),
            @ApiResponse(responseCode = "400", description = "El traslado no esta EN_TRANSITO",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Traslado o bibliotecario no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<TrasladoRetornoDTO> recibir(
            @Parameter(description = "ID del traslado", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "ID del bibliotecario", required = true, example = "2") @RequestParam Long bibliotecarioId) {
        log.info("POST /api/v1/traslados-retorno/{}/recibir - bibliotecarioId: {}", id, bibliotecarioId);
        return ResponseEntity.ok(trasladoService.recibirTraslado(id, bibliotecarioId));
    }
}