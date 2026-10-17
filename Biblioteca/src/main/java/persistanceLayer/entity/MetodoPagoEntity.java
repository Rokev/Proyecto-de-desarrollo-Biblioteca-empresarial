package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

/**
 * Método de pago guardado por el usuario.
 * Por seguridad NO se almacena el número completo de la tarjeta, solo un token de
 * la pasarela de pagos y los últimos 4 dígitos.
 */
@Entity
@Table(name = "metodos_pago")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetodoPagoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** TARJETA_CREDITO, TARJETA_DEBITO, PSE, etc. */
    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(length = 30)
    private String marca;

    @Column(length = 4)
    private String ultimosCuatroDigitos;

    @Column(length = 120)
    private String titular;

    /** Token entregado por la pasarela de pagos. */
    @Column(length = 255)
    private String tokenPasarela;

    @Column(nullable = false)
    @Builder.Default
    private Boolean predeterminado = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "metodoPago", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PagoEntity> pagos = new ArrayList<>();
}
