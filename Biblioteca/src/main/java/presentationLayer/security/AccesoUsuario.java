package presentationLayer.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/** Identifica al usuario del token y comprueba que solo toque sus propios datos. */
@Component
public class AccesoUsuario {

    /** id del usuario autenticado (claim "uid" del JWT). */
    public Long idActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            Object uid = jwt.getClaim("uid");
            if (uid instanceof Number n) {
                return n.longValue();
            }
        }
        throw new AccessDeniedException("Sesion invalida");
    }

    public boolean tieneRol(String... roles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (String rol : roles) {
            boolean tiene = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_" + rol));
            if (tiene) {
                return true;
            }
        }
        return false;
    }

    public boolean esAdmin() {
        return tieneRol("ADMIN");
    }

    public boolean esPersonal() {
        return tieneRol("ADMIN", "BIBLIOTECARIO");
    }

    /** El dato debe ser del usuario autenticado; el personal (bibliotecario/admin) puede ver los de cualquiera. */
    public void validarPropietario(Long usuarioId) {
        if (esPersonal()) {
            return;
        }
        if (usuarioId == null || !usuarioId.equals(idActual())) {
            throw new AccessDeniedException("No puedes acceder a datos de otro usuario");
        }
    }

    /** bibliotecarioId / repartidorId recibidos por parametro: deben ser el del token (el admin puede actuar por otro). */
    public void validarActor(Long actorId) {
        if (esAdmin()) {
            return;
        }
        if (actorId == null || !actorId.equals(idActual())) {
            throw new AccessDeniedException("No puedes actuar en nombre de otro usuario");
        }
    }

    /** Para listados: el personal ve todo; el cliente solo los registros cuyo propietario es el. */
    public <T> List<T> soloPropios(List<T> lista, Function<T, Long> propietario) {
        if (esPersonal()) {
            return lista;
        }
        Long yo = idActual();
        return lista.stream().filter(x -> yo.equals(propietario.apply(x))).toList();
    }
}