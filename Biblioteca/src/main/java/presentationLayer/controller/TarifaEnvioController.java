package presentationLayer.controller;

import bussinesLayer.dto.CotizacionEnvioDTO;
import bussinesLayer.dto.TarifaEnvioDTO;
import bussinesLayer.service.TarifaEnvioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tarifas-envio")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tarifas de envío", description = "Tarifas de envío interciudad por pareja de ciudades")
public class TarifaEnvioController {

    private final TarifaEnvioService tarifaEnvioService;

    @PostMapping
    @Operation(summary = "Crear una tarifa", description = "Carga la tarifa de una pareja de ciudades (vale en ambos sentidos)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tarifa creada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o pareja ya existente")
    })
    public ResponseEntity<TarifaEnvioDTO> crear(@Valid @RequestBody TarifaEnvioDTO dto) {
        log.info("POST /api/v1/tarifas-envio - {} / {}", dto.getCiudadA(), dto.getCiudadB());
        return ResponseEntity.status(HttpStatus.CREATED).body(tarifaEnvioService.crearTarifa(dto));
    }

    @GetMapping("/cotizar")
    @Operation(summary = "Cotizar un envío", description = "Calcula el costo del envío entre dos ciudades antes de confirmar el pedido")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cotización calculada"),
            @ApiResponse(responseCode = "400", description = "No hay tarifa para esa pareja de ciudades")
    })
    public ResponseEntity<CotizacionEnvioDTO> cotizar(
            @Parameter(description = "Ciudad de la sede de origen", example = "Armenia")
            @RequestParam("ciudadOrigen") String ciudadOrigen,
            @Parameter(description = "Ciudad de entrega", example = "Pereira")
            @RequestParam("ciudadDestino") String ciudadDestino) {
        return ResponseEntity.ok(tarifaEnvioService.cotizar(ciudadOrigen, ciudadDestino));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar tarifa por ID")
    public ResponseEntity<TarifaEnvioDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(tarifaEnvioService.obtenerTarifaPorId(id));
    }

    @GetMapping
    @Operation(summary = "Listar todas las tarifas")
    public ResponseEntity<List<TarifaEnvioDTO>> obtenerTodas() {
        return ResponseEntity.ok(tarifaEnvioService.obtenerTodasLasTarifas());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una tarifa")
    public ResponseEntity<TarifaEnvioDTO> actualizar(@PathVariable Long id, @Valid @RequestBody TarifaEnvioDTO dto) {
        log.info("PUT /api/v1/tarifas-envio/{}", id);
        return ResponseEntity.ok(tarifaEnvioService.actualizarTarifa(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una tarifa")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        log.info("DELETE /api/v1/tarifas-envio/{}", id);
        tarifaEnvioService.eliminarTarifa(id);
        return ResponseEntity.noContent().build();
    }
}