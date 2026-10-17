package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "prestamo de un ejemplar a un usuario")
public class PrestamoDTO {
    @Schema(description = "id unico de prestamo", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull(message = "El campo fechaSolicitud es obligatorio")
    @Schema(description = "Fecha de solicitud", example = "2026-10-17T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime fechaSolicitud;

    @Schema(description = "Fecha de entrega del libro", example = "2026-10-17T12:00:00")
    private LocalDateTime fechaEntrega;

    @Schema(description = "Fecha limite de devolucion", example = "2026-10-31")
    private LocalDate fechaVencimiento;

    @Schema(description = "Fecha de devolucion", example = "2026-10-30T10:00:00")
    private LocalDateTime fechaDevolucion;

    @Size(max = 20, message = "El campo estado no puede superar los 20 caracteres")
    @Schema(description = "SOLICITADO, ACTIVO, DEVUELTO, VENCIDO, CANCELADO", example = "SOLICITADO", maxLength = 20)
    private String estado;

    @Size(max = 20, message = "El campo modalidad no puede superar los 20 caracteres")
    @Schema(description = "SEDE, DOMICILIO o INTERCIUDAD", example = "SEDE", maxLength = 20)
    private String modalidad;

    @Size(max = 200, message = "El campo direccionEntrega no puede superar los 200 caracteres")
    @Schema(description = "Direccion de entrega (solo si la modalidad es DOMICILIO)", example = "calle 35 casa n34-57", maxLength = 200)
    private String direccionEntrega;

    @Min(value = 0, message = "El campo vecesRenovado no puede ser negativo")
    @Schema(description = "Veces que se ha renovado", example = "0")
    private Integer vecesRenovado;

    @NotNull(message = "El campo usuarioId es obligatorio")
    @Schema(description = "id del usuario", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long usuarioId;

    @NotNull(message = "El campo ejemplarId es obligatorio")
    @Schema(description = "id del ejemplar", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long ejemplarId;

    @NotNull(message = "El campo sedeRetiroId es obligatorio")
    @Schema(description = "id de la sede de retiro", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long sedeRetiroId;

}
