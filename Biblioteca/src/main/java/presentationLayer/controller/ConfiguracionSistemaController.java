package presentationLayer.controller;

import bussinesLayer.dto.PlazoSistemaDTO;
import bussinesLayer.dto.PoliticaMultaDTO;
import bussinesLayer.dto.PoliticaSancionDTO;
import bussinesLayer.service.ConfiguracionSistemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**  74,  75 y  76: configuración de políticas de sanción, multas y plazos (rol administrador). */
@RestController
@RequestMapping("/api/v1/configuracion")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Configuración del sistema", description = "Políticas de sanción, multas y plazos ( 74,  75,  76)")
public class ConfiguracionSistemaController {

    private final ConfiguracionSistemaService configuracionService;

    // ----------------------------------------------------------------  74

    @GetMapping("/sanciones")
    @Operation(summary = "Listar políticas de sanción", description = "Una por infracción: MORA_DEVOLUCION, PEDIDO_NO_RECOGIDO, LIBRO_DANADO, LIBRO_PERDIDO")
    public ResponseEntity<List<PoliticaSancionDTO>> listarSanciones() {
        return ResponseEntity.ok(configuracionService.listarPoliticasSancion());
    }

    @GetMapping("/sanciones/{infraccion}")
    @Operation(summary = "Consultar la política de sanción de una infracción")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Política encontrada"),
            @ApiResponse(responseCode = "400", description = "Infracción desconocida")
    })
    public ResponseEntity<PoliticaSancionDTO> obtenerSancion(
            @Parameter(description = "Infracción", example = "MORA_DEVOLUCION") @PathVariable String infraccion) {
        return ResponseEntity.ok(configuracionService.obtenerPoliticaSancion(infraccion));
    }

    @PutMapping("/sanciones/{infraccion}")
    @Operation(summary = "Configurar la política de sanción de una infracción",
            description = "Define el tipo de restricción (bloqueo temporal, suspensión, advertencia), su duración en días y si está activa. Aplica a las sanciones que se generen desde ese momento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Política actualizada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<PoliticaSancionDTO> actualizarSancion(
            @Parameter(description = "Infracción", example = "PEDIDO_NO_RECOGIDO") @PathVariable String infraccion,
            @Valid @RequestBody PoliticaSancionDTO dto) {
        log.info("PUT /api/v1/configuracion/sanciones/{}", infraccion);
        return ResponseEntity.ok(configuracionService.actualizarPoliticaSancion(infraccion, dto));
    }

    // ----------------------------------------------------------------  75

    @GetMapping("/multas")
    @Operation(summary = "Listar políticas de multa", description = "Montos de mora por día, libro dañado, libro perdido, pedido no recogido y recargo por pedido no recibido")
    public ResponseEntity<List<PoliticaMultaDTO>> listarMultas() {
        return ResponseEntity.ok(configuracionService.listarPoliticasMulta());
    }

    @GetMapping("/multas/{concepto}")
    @Operation(summary = "Consultar el monto de un concepto de multa")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Política encontrada"),
            @ApiResponse(responseCode = "400", description = "Concepto desconocido")
    })
    public ResponseEntity<PoliticaMultaDTO> obtenerMulta(
            @Parameter(description = "Concepto", example = "MORA_POR_DIA") @PathVariable String concepto) {
        return ResponseEntity.ok(configuracionService.obtenerPoliticaMulta(concepto));
    }

    @PutMapping("/multas/{concepto}")
    @Operation(summary = "Configurar el monto de un concepto de multa",
            description = "Aplica a las multas que se generen desde ese momento. Las multas ya generadas no cambian, salvo la mora de préstamos vencidos, que se recalcula cada día con el valor vigente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Monto actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<PoliticaMultaDTO> actualizarMulta(
            @Parameter(description = "Concepto", example = "RECARGO_PEDIDO_NO_RECIBIDO") @PathVariable String concepto,
            @Valid @RequestBody PoliticaMultaDTO dto) {
        log.info("PUT /api/v1/configuracion/multas/{}", concepto);
        return ResponseEntity.ok(configuracionService.actualizarPoliticaMulta(concepto, dto));
    }

    // ----------------------------------------------------------------  76

    @GetMapping("/plazos")
    @Operation(summary = "Listar plazos del sistema", description = "Días de préstamo, días de préstamo interciudad, horas de retiro y días de recogida en sede de devolución")
    public ResponseEntity<List<PlazoSistemaDTO>> listarPlazos() {
        return ResponseEntity.ok(configuracionService.listarPlazos());
    }

    @GetMapping("/plazos/{clave}")
    @Operation(summary = "Consultar un plazo del sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plazo encontrado"),
            @ApiResponse(responseCode = "400", description = "Clave desconocida")
    })
    public ResponseEntity<PlazoSistemaDTO> obtenerPlazo(
            @Parameter(description = "Clave del plazo", example = "DIAS_PRESTAMO") @PathVariable String clave) {
        return ResponseEntity.ok(configuracionService.obtenerPlazo(clave));
    }

    @PutMapping("/plazos/{clave}")
    @Operation(summary = "Configurar un plazo del sistema",
            description = "Aplica a los préstamos, reservas y pedidos que se creen o avancen desde ese momento; los plazos ya calculados no cambian")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plazo actualizado"),
            @ApiResponse(responseCode = "400", description = "Valor fuera de rango")
    })
    public ResponseEntity<PlazoSistemaDTO> actualizarPlazo(
            @Parameter(description = "Clave del plazo", example = "HORAS_RETIRO_LIBRO") @PathVariable String clave,
            @Valid @RequestBody PlazoSistemaDTO dto) {
        log.info("PUT /api/v1/configuracion/plazos/{}", clave);
        return ResponseEntity.ok(configuracionService.actualizarPlazo(clave, dto));
    }
}


