package persistanceLayer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos_interciudad")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoInterciudadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    @Builder.Default
    private String estado = "SOLICITADO";

    @Column(nullable = false, length = 80)
    private String ciudadEntrega;

    @Column(nullable = false, length = 200)
    private String direccionEntrega;

    @Column(nullable = false)
    private LocalDate fechaRecepcion;

    @Column(nullable = false, length = 50)
    private String horaRecepcion;

    @Column(nullable = false)
    private Double costoEnvio;

    @Column(nullable = false)
    private LocalDateTime fechaSolicitud;

    private LocalDateTime fechaTransito;
    private LocalDateTime fechaEntrega;
    private LocalDateTime fechaCancelacion;
    private LocalDateTime fechaLimiteRecogida;
    private LocalDateTime fechaLimiteDevolucion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejemplar_id", nullable = false)
    private EjemplarEntity ejemplar;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_origen_id", nullable = false)
    private SedeEntity sedeOrigen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_devolucion_id", nullable = false)
    private SedeEntity sedeDevolucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repartidor_id")
    private UsuarioEntity repartidor;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prestamo_id", nullable = false, unique = true)
    private PrestamoEntity prestamo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bibliotecario_id")
    private UsuarioEntity bibliotecario;
}