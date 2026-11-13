package presentationLayer.security;

import bussinesLayer.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/** Identifica al usuario del token y comprueba que solo toque sus datos (o los de su sede si es bibliotecario). */
@Component
@RequiredArgsConstructor
public class AccesoUsuario {

    private final UsuarioService usuarioService;

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

    public boolean esBibliotecario() {
        return tieneRol("BIBLIOTECARIO");
    }

    public boolean esPersonal() {
        return tieneRol("ADMIN", "BIBLIOTECARIO");
    }

    /** El dato debe ser del usuario autenticado; el personal puede ver los de cualquiera. */
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

    /** Para listados sin sede: el personal ve todo; el cliente solo lo suyo. */
    public <T> List<T> soloPropios(List<T> lista, Function<T, Long> propietario) {
        if (esPersonal()) {
            return lista;
        }
        Long yo = idActual();
        return lista.stream().filter(x -> yo.equals(propietario.apply(x))).toList();
    }

    // ------------------------------------------------------------------ control por sede

    /** Sede asignada en la cuenta del usuario autenticado (solo bibliotecarios). */
    public Long sedeActual() {
        return usuarioService.obtenerUsuarioPorId(idActual()).getSedeId();
    }

    /** Admin: siempre. Bibliotecario: si alguna de las sedes indicadas es la suya. Los demas: nunca. */
    public boolean puedeVerSede(Long... sedeIds) {
        if (esAdmin()) {
            return true;
        }
        if (!esBibliotecario()) {
            return false;
        }
        Long mia = sedeActual();
        return mia != null && Arrays.asList(sedeIds).contains(mia);
    }

    public void validarSede(Long... sedeIds) {
        if (!puedeVerSede(sedeIds)) {
            throw new AccessDeniedException("Este recurso pertenece a otra sede");
        }
    }

    /** El dueño del dato pasa siempre; el bibliotecario solo si es de su sede; el admin siempre. */
    public void validarPropietarioOSede(Long usuarioId, Long... sedeIds) {
        if (esAdmin() || (usuarioId != null && usuarioId.equals(idActual()))) {
            return;
        }
        validarSede(sedeIds);
    }

    /** Listados: admin todo; bibliotecario lo de su sede; cliente lo suyo. */
    public <T> List<T> visibles(List<T> lista, Function<T, Long> propietario, Function<T, List<Long>> sedes) {
        if (esAdmin()) {
            return lista;
        }
        if (esBibliotecario()) {
            Long mia = sedeActual();
            return lista.stream().filter(x -> mia != null && sedes.apply(x).contains(mia)).toList();
        }
        Long yo = idActual();
        return lista.stream().filter(x -> yo.equals(propietario.apply(x))).toList();
    }

    public List<Long> sedes(Long... ids) {
        return Arrays.asList(ids);
    }
}