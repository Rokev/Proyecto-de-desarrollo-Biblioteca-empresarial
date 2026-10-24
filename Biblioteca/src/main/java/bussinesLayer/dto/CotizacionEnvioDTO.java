package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Costo de envío calculado entre dos ciudades (se muestra antes de confirmar el pedido, RF-28)")
public class CotizacionEnvioDTO {
    private String ciudadOrigen;
    private String ciudadDestino;
    private BigDecimal costoEnvio;
}