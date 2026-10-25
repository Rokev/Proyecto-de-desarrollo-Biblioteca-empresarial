package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Prestamo con el libro, la sede y las fechas relevantes (consultas de usuario y de bibliotecario)")
public class PrestamoDetalleDTO {
    @Schema(description = "ID del prestamo")
    private Long prestamoId;

    @Schema(description = "SOLICITADO, ACTIVO, VENCIDO, DEVUELTO o CANCELADO")
    private String estado;

    @Schema(description = "SEDE, DOMICILIO o INTERCIUDAD")
    private String modalidad;

    @Schema(description = "ID del usuario")
    private Long usuarioId;

    @Schema(description = "ID del libro")
    private Long libroId;

    @Schema(description = "Titulo del libro")
    private String libroTitulo;

    @Schema(description = "ID del ejemplar")
    private Long ejemplarId;

    @Schema(description = "ID de la sede de retiro")
    private Long sedeId;

    @Schema(description = "Nombre de la sede de retiro")
    private String sedeNombre;

    @Schema(description = "Fecha de solicitud")
    private LocalDateTime fechaSolicitud;

    @Schema(description = "Fecha de entrega al usuario")
    private LocalDateTime fechaEntrega;

    @Schema(description = "Fecha limite de devolucion")
    private LocalDate fechaLimiteDevolucion;

    @Schema(description = "Fecha en que se devolvio")
    private LocalDateTime fechaDevolucion;
}
