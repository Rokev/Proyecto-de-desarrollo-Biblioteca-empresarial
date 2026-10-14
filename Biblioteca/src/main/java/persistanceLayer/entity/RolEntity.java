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
 * Rol de un usuario dentro del sistema (CLIENTE, BIBLIOTECARIA / GESTOR, etc.).
 */
@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String nombre;

    @Column(length = 200)
    private String descripcion;

    @OneToMany(mappedBy = "rol", fetch = FetchType.LAZY)
    @Builder.Default
    private List<UsuarioEntity> usuarios = new ArrayList<>();
}
