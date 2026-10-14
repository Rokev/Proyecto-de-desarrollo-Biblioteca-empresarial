package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Builder;

/**
 * Pregunta frecuente (FAQ) que usa el chatbot de servicio al cliente.
 */
@Entity
@Table(name = "preguntas_frecuentes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreguntaFrecuente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 250)
    private String pregunta;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String respuesta;

    @Column(length = 60)
    private String categoria;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;
}
