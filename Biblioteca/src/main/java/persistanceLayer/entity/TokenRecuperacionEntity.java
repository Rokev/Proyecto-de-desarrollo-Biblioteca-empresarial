package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Builder;

import java.time.LocalDateTime;
/**
 * Token de restablecimiento de contraseña enviado por correo
 * Expira a las 24 horas de su creación y solo puede usarse una vez.
 */
@Entity
@Table(name = "tokens_recuperacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRecuperacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    /** fechaCreacion + 24 horas. */
    @Column(nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean usado = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;
}
