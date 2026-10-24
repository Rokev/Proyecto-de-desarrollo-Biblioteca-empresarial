package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "pedido interciudad de un libro con entrega a domicilio (usuarios con suscripcion)")
public class PedidoInterciudadDTO {
    @Schema(description = "id unico del pedido", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "SOLICITADO, EN_TRANSITO, DISPONIBLE_PARA_RETIRO, RETIRADO, CANCELADO", example = "SOLICITADO", accessMode = Schema.AccessMode.READ_ONLY)
    private String estado;

    @Schema(description = "Ciudad de entrega", example = "Medellin")
    private String ciudadEntrega;

    @Schema(description = "Direccion de entrega a domicilio", example = "Calle 10 # 20-30")
    private String direccionEntrega;

    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaTransito;
    private LocalDateTime fechaDisponible;
    private LocalDateTime fechaRetiro;
    private LocalDateTime fechaCancelacion;

    @Schema(description = "id del usuario que pidio el libro", example = "1")
    private Long usuarioId;

    @Schema(description = "id del ejemplar asignado", example = "1")
    private Long ejemplarId;

    @Schema(description = "id del libro", example = "1")
    private Long libroId;

    @Schema(description = "Titulo del libro", example = "Cien anos de soledad")
    private String libroTitulo;

    @Schema(description = "id de la sede de origen del ejemplar", example = "1")
    private Long sedeOrigenId;

    @Schema(description = "Nombre de la sede de origen", example = "Biblioteca Central Bogota")
    private String sedeOrigenNombre;

    @Schema(description = "id del prestamo vinculado", example = "1")
    private Long prestamoId;

    @Schema(description = "id del ultimo bibliotecario que cambio el estado", example = "2")
    private Long bibliotecarioId;
}