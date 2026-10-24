package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "transaccion de pago (multa, suscripcion o reserva de sala)")
public class PagoDTO {
    @Schema(description = "id unico de pago", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull(message = "El campo monto es obligatorio")
    @DecimalMin(value = "0.00", message = "El campo monto no puede ser negativo")
    @Schema(description = "Monto pagado", example = "50000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal monto;

    @NotNull(message = "El campo fechaPago es obligatorio")
    @Schema(description = "Fecha de pago", example = "2026-10-17T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime fechaPago;

    @NotBlank(message = "El campo concepto es obligatorio")
    @Size(max = 20, message = "El campo concepto no puede superar los 20 caracteres")
    @Schema(description = "MULTA, SUSCRIPCION, RESERVA_SALA", example = "MULTA", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 20)
    private String concepto;

    @NotBlank(message = "El campo estado es obligatorio")
    @Size(max = 20, message = "El campo estado no puede superar los 20 caracteres")
    @Schema(description = "EXITOSO, FALLIDO, PENDIENTE", example = "EXITOSO", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 20)
    private String estado;

    @NotBlank(message = "El campo referencia es obligatorio")
    @Size(max = 60, message = "El campo referencia no puede superar los 60 caracteres")
    @Schema(description = "Numero de comprobante / referencia", example = "REF-20261017-0001", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 60)
    private String referencia;

    @NotNull(message = "El campo usuarioId es obligatorio")
    @Schema(description = "id del usuario", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long usuarioId;

    @NotNull(message = "El campo metodoPagoId es obligatorio")
    @Schema(description = "id del metodo de pago", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long metodoPagoId;

    @Schema(description = "id de la multa (solo si el concepto es MULTA)", example = "1")
    private Long multaId;

    @Schema(description = "id de la suscripcion (solo si el concepto es SUSCRIPCION)", example = "1")
    private Long suscripcionId;

    @Schema(description = "id de la reserva de sala (solo si el concepto es RESERVA_SALA)", example = "1")
    private Long reservaSalaId;

    @Schema(description = "id del pedido interciudad (solo si el concepto es ENVIO_INTERCIUDAD)", example = "1")
    private Long pedidoInterciudadId;
}
