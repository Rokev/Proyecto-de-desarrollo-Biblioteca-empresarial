package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Reserva de libro con el libro, la sede y la fecha limite de retiro")
public class ReservaLibroDetalleDTO {
    @Schema(description = "ID de la reserva")
    private Long reservaId;

    @Schema(description = "EN_ESPERA, NOTIFICADA, CANCELADA o EXPIRADA")
    private String estado;

    @Schema(description = "ID del usuario")
    private Long usuarioId;

    @Schema(description = "ID del libro")
    private Long libroId;

    @Schema(description = "Titulo del libro")
    private String libroTitulo;

    @Schema(description = "ID de la sede")
    private Long sedeId;

    @Schema(description = "Nombre de la sede")
    private String sedeNombre;

    @Schema(description = "Fecha de la reserva")
    private LocalDateTime fechaReserva;

    @Schema(description = "Fecha limite para retirar el libro (solo NOTIFICADA)")
    private LocalDateTime fechaLimiteRetiro;
}
