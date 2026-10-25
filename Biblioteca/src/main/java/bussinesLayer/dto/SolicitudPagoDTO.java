package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Solicitud de pago con tarjeta simulada para suscripción, envío interciudad, multas o reservas de salas")
public class SolicitudPagoDTO {

    @NotNull(message = "El campo usuarioId es obligatorio")
    @Schema(description = "ID del usuario que realiza el pago", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long usuarioId;

    @NotBlank(message = "El concepto de pago es obligatorio")
    @Schema(description = "Concepto: SUSCRIPCION, ENVIO_INTERCIUDAD, MULTA, RECARGO, RESERVA_SALA", example = "MULTA", requiredMode = Schema.RequiredMode.REQUIRED)
    private String concepto;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto a pagar debe ser mayor a cero")
    @Schema(description = "Monto a procesar", example = "15000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal monto;

    @NotBlank(message = "El número de tarjeta es obligatorio")
    @Pattern(regexp = "^[0-9]{16}$", message = "El número de tarjeta simulada debe contener exactamente 16 dígitos")
    @Schema(description = "Número de tarjeta de prueba de 16 dígitos (Ej: 4532000000001234 para aprobado)", example = "4532000000001234", requiredMode = Schema.RequiredMode.REQUIRED)
    private String numeroTarjeta;

    @Schema(description = "ID de la multa a pagar (Obligatorio si el concepto es MULTA)", example = "1")
    private Long multaId;

    @Schema(description = "ID del plan de suscripción a comprar (Obligatorio si el concepto es SUSCRIPCION)", example = "1")
    private Long planSuscripcionId;

    @Schema(description = "ID de la reserva de sala a pagar (Obligatorio si el concepto es RESERVA_SALA)", example = "1")
    private Long reservaSalaId;

    @Schema(description = "ID del pedido interciudad a pagar (Obligatorio si el concepto es ENVIO_INTERCIUDAD)", example = "1")
    private Long pedidoInterciudadId;
}