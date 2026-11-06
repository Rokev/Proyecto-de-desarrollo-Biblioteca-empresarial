package bussinesLayer.config;

import bussinesLayer.dto.RolDTO;
import bussinesLayer.dto.UsuarioDTO;
import bussinesLayer.service.RolService;
import bussinesLayer.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import persistanceLayer.dao.UsuarioDAO;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeguridadInicializador implements ApplicationRunner {

    private record RolInicial(String nombre, String descripcion) {
    }

    private static final List<RolInicial> ROLES = List.of(
            new RolInicial("CLIENTE", "Usuario cliente de la biblioteca"),
            new RolInicial("BIBLIOTECARIO", "Personal de la biblioteca"),
            new RolInicial("REPARTIDOR", "Repartidor de pedidos a domicilio"),
            new RolInicial("ADMIN", "Administrador del sistema"));

    private final RolService rolService;
    private final UsuarioService usuarioService;
    private final UsuarioDAO usuarioDAO;

    @Value("${admin.inicial.correo}")
    private String adminCorreo;

    @Value("${admin.inicial.contrasena}")
    private String adminContrasena;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Set<String> existentes = rolService.obtenerTodosLosRoles().stream()
                    .map(r -> r.getNombre().trim().toUpperCase())
                    .collect(Collectors.toSet());
            for (RolInicial rol : ROLES) {
                if (!existentes.contains(rol.nombre())) {
                    rolService.crearRol(new RolDTO(null, rol.nombre(), rol.descripcion()));
                    log.info("Rol creado: {}", rol.nombre());
                }
            }

            if (usuarioDAO.existsByCorreoIgnoreCase(adminCorreo)) {
                return;
            }
            Long rolAdminId = rolService.obtenerTodosLosRoles().stream()
                    .filter(r -> "ADMIN".equalsIgnoreCase(r.getNombre().trim()))
                    .map(RolDTO::getId)
                    .findFirst()
                    .orElseThrow();

            UsuarioDTO admin = new UsuarioDTO();
            admin.setNombre("Administrador");
            admin.setApellido("Sistema");
            admin.setDocumento("0000000000");
            admin.setNombreUsuario("admin");
            admin.setCorreo(adminCorreo);
            admin.setContrasena(adminContrasena);
            admin.setRolId(rolAdminId);
            admin.setActivo(true);
            usuarioService.crearUsuario(admin);
            log.info("Administrador inicial creado: {}", adminCorreo);
        } catch (Exception e) {
            log.warn("No se pudo inicializar la seguridad (roles/administrador): {}", e.getMessage());
        }
    }
}