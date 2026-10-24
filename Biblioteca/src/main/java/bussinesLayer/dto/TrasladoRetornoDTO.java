package bussinesLayer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "traslado de un ejemplar de regreso a su sede de origen tras una devolucion en otra sede")
public class TrasladoRetornoDTO {
    @Schema(description = "id unico del traslado", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "EN_TRANSITO, RECIBIDO", example = "EN_TRANSITO", accessMode = Schema.AccessMode.READ_ONLY)
    private String estado;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaRecepcion;

    @Schema(description = "id del ejemplar", example = "1")
    private Long ejemplarId;
    @Schema(description = "id del libro", example = "1")
    private Long libroId;
    @Schema(description = "Titulo del libro", example = "Cien anos de soledad")
    private String libroTitulo;

    @Schema(description = "id de la sede donde se devolvio el libro", example = "2")
    private Long sedeActualId;
    private String sedeActualNombre;

    @Schema(description = "id de la sede de origen a la que vuelve", example = "1")
    private Long sedeDestinoId;
    private String sedeDestinoNombre;

    @Schema(description = "id de la devolucion que origino el traslado", example = "1")
    private Long devolucionId;

    @Schema(description = "id del bibliotecario que recibio el ejemplar en la sede de origen", example = "2")
    private Long bibliotecarioId;
}