package BussisnesCatLayer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "metodo de pago guardado por el usuario")
public class MetodoPagoDTO {
    @Schema(description = "id unico de metodo de pago", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "El campo tipo es obligatorio")
    @Size(max = 30, message = "El campo tipo no puede superar los 30 caracteres")
    @Schema(description = "TARJETA_CREDITO, TARJETA_DEBITO, PSE, etc.", example = "TARJETA_CREDITO", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 30)
    private String tipo;

    @Size(max = 30, message = "El campo marca no puede superar los 30 caracteres")
    @Schema(description = "Marca", example = "VISA", maxLength = 30)
    private String marca;

    @Size(max = 4, message = "El campo ultimosCuatroDigitos no puede superar los 4 caracteres")
    @Schema(description = "Ultimos 4 digitos", example = "4242", maxLength = 4)
    private String ultimosCuatroDigitos;

    @Size(max = 120, message = "El campo titular no puede superar los 120 caracteres")
    @Schema(description = "Titular", example = "Maria Perez", maxLength = 120)
    private String titular;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(max = 255, message = "El campo tokenPasarela no puede superar los 255 caracteres")
    @Schema(description = "Token entregado por la pasarela de pagos", example = "tok_abc123", accessMode = Schema.AccessMode.WRITE_ONLY, maxLength = 255)
    private String tokenPasarela;

    @Schema(description = "Indica si es el metodo predeterminado", example = "false")
    private Boolean predeterminado;

    @NotNull(message = "El campo usuarioId es obligatorio")
    @Schema(description = "id del usuario", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long usuarioId;

}
