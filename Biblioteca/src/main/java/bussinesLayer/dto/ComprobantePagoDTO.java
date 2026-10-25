package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Comprobante de pago generado por la pasarela simulada")
public class ComprobantePagoDTO {

    @Schema(description = "ID único del registro de pago", example = "10")
    private Long id;

    @Schema(description = "Número de referencia generado por el sistema", example = "PAY-20261024-A8F92")
    private String numeroReferencia;

    @Schema(description = "Estado del pago: PENDIENTE, APROBADO, RECHAZADO, REEMBOLSADO", example = "APROBADO")
    private String estado;

    @Schema(description = "Concepto pagado", example = "MULTA")
    private String concepto;

    @Schema(description = "Monto procesado", example = "15000.00")
    private BigDecimal monto;

    @Schema(description = "Últimos 4 dígitos de la tarjeta utilizada", example = "1234")
    private String ultimosCuatroDigitos;

    @Schema(description = "Fecha y hora del procesamiento", example = "2026-10-24T15:30:00")
    private LocalDateTime fechaPago;

    @Schema(description = "Mensaje descriptivo sobre el resultado de la transacción", example = "Pago aprobado exitosamente")
    private String mensaje;

    @Schema(description = "ID del usuario", example = "1")
    private Long usuarioId;
}