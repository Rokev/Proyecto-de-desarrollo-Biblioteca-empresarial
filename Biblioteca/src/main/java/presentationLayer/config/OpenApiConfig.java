package presentationLayer.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion general de la documentacion OpenAPI/Swagger.
 * Swagger UI: http://localhost:8080/swagger-ui
 * JSON:       http://localhost:8080/api-docs
 * YAML:       http://localhost:8080/api-docs.yaml
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API de Biblioteca",
                version = "1.0.0",
                description = "API REST para la gestion de una red de bibliotecas: catalogo, prestamos, "
                        + "reservas, salas de estudio, eventos, suscripciones y pagos. Los errores se devuelven con el formato ApiError: "
                        + "400 datos invalidos o regla de negocio, 404 recurso inexistente, "
                        + "409 registro relacionado con otros datos y 500 error interno.",
                contact = @Contact(name = "Equipo de Desarrollo"),
                license = @License(name = "Uso academico")
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Servidor de Desarrollo")
        }
)
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        // Preparado para autenticacion JWT si se implementa mas adelante
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Ingresa tu token JWT")));
    }
}
