package persistanceLayer.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** La configuracion de JPA vive aqui (y no en la clase principal) para que las pruebas @WebMvcTest no la carguen. */
@Configuration
@EnableJpaRepositories(basePackages = "persistanceLayer.repository")
@EntityScan(basePackages = "persistanceLayer.entity")
public class JpaConfig {
}
