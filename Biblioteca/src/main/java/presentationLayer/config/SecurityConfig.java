package presentationLayer.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import presentationLayer.security.RestAccessDeniedHandler;
import presentationLayer.security.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String CLIENTE = "CLIENTE";
    private static final String BIBLIOTECARIO = "BIBLIOTECARIO";
    private static final String REPARTIDOR = "REPARTIDOR";
    private static final String ADMIN = "ADMIN";

    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    @Value("${cors.origenes}")
    private String[] origenesPermitidos;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Lee el claim "rol" del JWT y lo convierte en la autoridad ROLE_<ROL>. */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("rol");
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(autoridades);
        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .oauth2ResourceServer(oauth -> oauth
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                                                .authorizeHttpRequests(auth -> auth

                        // ---- Publico (sin login)
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/registro",
                                "/api/v1/auth/recuperar", "/api/v1/auth/restablecer").permitAll()
                        .requestMatchers("/api/v1/chatbot/**").permitAll()
                        // RF-07: publicamente solo las sedes activas
                        .requestMatchers(HttpMethod.GET, "/api/v1/libros/**", "/api/v1/autores/**",
                                "/api/v1/generos/**", "/api/v1/sedes/activas",
                                "/api/v1/preguntas-frecuentes/**").permitAll()

                        // ---- Lectura para usuarios con sesion
                        .requestMatchers(HttpMethod.GET, "/api/v1/sedes/*", "/api/v1/ejemplares/**",
                                "/api/v1/salas-estudio/**", "/api/v1/eventos/**", "/api/v1/planes-suscripcion/**",
                                "/api/v1/tarifas-envio/cotizar").hasAnyRole(CLIENTE, BIBLIOTECARIO, ADMIN)

                        // ---- Escritura de catalogo: bibliotecario y admin
                        .requestMatchers("/api/v1/libros/**", "/api/v1/autores/**", "/api/v1/generos/**",
                                "/api/v1/ejemplares/**", "/api/v1/salas-estudio/**", "/api/v1/eventos/**")
                        .hasAnyRole(BIBLIOTECARIO, ADMIN)

                        // ---- Escritura: solo admin (RF-66 a RF-73, RF-69 lista completa de sedes, RF-81)
                        .requestMatchers("/api/v1/sedes/**", "/api/v1/planes-suscripcion/**",
                                "/api/v1/preguntas-frecuentes/**").hasRole(ADMIN)

                        // ---- CLIENTE: solo estos endpoints (cada uno valida en el controller que el dato sea suyo)
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/prestamos", "/api/v1/reservas-libro", "/api/v1/reservas-sala",
                                "/api/v1/metodos-pago", "/api/v1/inscripciones-evento",
                                "/api/v1/pagos/procesar",
                                "/api/v1/suscripciones/comprar", "/api/v1/suscripciones/*/cancelar",
                                "/api/v1/pedidos-interciudad", "/api/v1/pedidos-interciudad/*/cancelar",
                                "/api/v1/devoluciones/solicitar",
                                "/api/v1/reservas-libro/*/cancelar", "/api/v1/reservas-sala/*/cancelar")
                        .hasAnyRole(CLIENTE, BIBLIOTECARIO, ADMIN)
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/prestamos/*", "/api/v1/prestamos/usuario/*",
                                "/api/v1/reservas-libro/*", "/api/v1/reservas-libro/usuario/*",
                                "/api/v1/reservas-sala", "/api/v1/reservas-sala/*",
                                "/api/v1/pagos/comprobante/*", "/api/v1/pagos/usuario/*",
                                "/api/v1/suscripciones/*", "/api/v1/suscripciones/usuario/*/vigente",
                                "/api/v1/multas/*", "/api/v1/multas/usuario/**",
                                "/api/v1/sanciones/usuario/*",
                                "/api/v1/notificaciones", "/api/v1/notificaciones/*",
                                "/api/v1/metodos-pago", "/api/v1/metodos-pago/*",
                                "/api/v1/inscripciones-evento", "/api/v1/inscripciones-evento/*",
                                "/api/v1/pedidos-interciudad/*", "/api/v1/pedidos-interciudad/usuario/*",
                                "/api/v1/devoluciones/sede-sugerida")
                        .hasAnyRole(CLIENTE, BIBLIOTECARIO, ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/notificaciones/*", "/api/v1/metodos-pago/*")
                        .hasAnyRole(CLIENTE, BIBLIOTECARIO, ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/metodos-pago/*", "/api/v1/inscripciones-evento/*")
                        .hasAnyRole(CLIENTE, BIBLIOTECARIO, ADMIN)

                        // ---- REPARTIDOR: entregas (tambien existen dentro de /pedidos-interciudad)
                        .requestMatchers(HttpMethod.GET, "/api/v1/pedidos-interciudad/repartidor/*")
                        .hasAnyRole(REPARTIDOR, ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/v1/pedidos-interciudad/*/confirmar-entrega",
                                "/api/v1/pedidos-interciudad/*/registrar-no-recibido",
                                "/api/v1/pedidos-interciudad/*/iniciar-traslado-retorno")
                        .hasAnyRole(REPARTIDOR, ADMIN)
                        .requestMatchers("/api/v1/repartidores/**").hasAnyRole(REPARTIDOR, ADMIN)

                        // ---- Lectura (solo listados) para el bibliotecario: nombres de usuarios y repartidores en su panel.
                        //      Debe ir ANTES de la regla de administracion; escritura y /usuarios/{id} siguen siendo solo ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/v1/usuarios", "/api/v1/roles")
                        .hasAnyRole(BIBLIOTECARIO, ADMIN)

                        // ---- Personal de biblioteca: lo demas de estos modulos
                        .requestMatchers("/api/v1/devoluciones/**", "/api/v1/traslados-retorno/**",
                                "/api/v1/sanciones/**",
                                "/api/v1/prestamos/**", "/api/v1/reservas-libro/**", "/api/v1/reservas-sala/**",
                                "/api/v1/suscripciones/**", "/api/v1/multas/**",
                                "/api/v1/notificaciones/**", "/api/v1/metodos-pago/**",
                                "/api/v1/pedidos-interciudad/**", "/api/v1/inscripciones-evento/**")
                        .hasAnyRole(BIBLIOTECARIO, ADMIN)

                        // ---- Administracion (RF-58 a RF-65, RF-77 a RF-80)
                        .requestMatchers("/api/v1/usuarios/**", "/api/v1/roles/**", "/api/v1/tarifas-envio/**",
                                "/api/v1/configuracion/**", "/api/v1/reportes/**", "/api/v1/auditoria/**",
                                "/api/v1/pagos/**",
                                "/api/v1/sesiones/**", "/api/v1/tokens-recuperacion/**").hasRole(ADMIN)

                        // ---- Resto de /auth (logout, me) y cualquier otra ruta: solo con sesion
                        .anyRequest().authenticated());

        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(origenesPermitidos));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Content-Disposition")); // para descargar reportes PDF/CSV
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", config);
        return fuente;
    }
}