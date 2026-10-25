package bussinesLayer.aspect;

import bussinesLayer.service.AuditoriaService;
import bussinesLayer.util.AccionAuditoria;
import bussinesLayer.util.ModuloAuditoria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Registra en el log de auditoría (RF-79) las acciones administrativas del sistema sin tocar los servicios
 * existentes: RF-59, RF-63, RF-65, RF-77 y demás cambios de usuarios, sedes, planes, políticas, tarifas y chatbot.
 *
 * Se ejecuta DESPUÉS de que el método de negocio termina bien, así que una operación rechazada
 * (validación, sede con préstamos activos, etc.) no deja rastro.
 * Quién hizo la acción se toma del parámetro de la petición {@code administradorId} o, si no viene,
 * del encabezado {@code X-Usuario-Id}. Si no viene ninguno, el registro queda como SISTEMA.
 * Un fallo al auditar nunca rompe la operación principal.
 *
 * Para auditar otro método: agrega una línea en REGLAS ("ClaseImpl.metodo" -> módulo, acción, texto).
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditoriaAspect {

    private record Regla(String modulo, String accion, String texto) {
    }

    private static final Map<String, Regla> REGLAS = Map.ofEntries(
            // Usuarios, bibliotecarios y repartidores (RF-59, RF-60, RF-63, RF-64, RF-65)
            Map.entry("UsuarioServiceImpl.actualizarUsuario", new Regla(ModuloAuditoria.USUARIOS, AccionAuditoria.MODIFICAR, "Datos de cuenta modificados")),
            Map.entry("UsuarioServiceImpl.eliminarUsuario", new Regla(ModuloAuditoria.USUARIOS, AccionAuditoria.ELIMINAR, "Cuenta eliminada")),
            Map.entry("UsuarioServiceImpl.desactivarPersonal", new Regla(ModuloAuditoria.PERSONAL, AccionAuditoria.DESACTIVAR, "Cuenta de bibliotecario/repartidor desactivada")),
            Map.entry("UsuarioServiceImpl.suspenderUsuario", new Regla(ModuloAuditoria.USUARIOS, AccionAuditoria.SUSPENDER, "Cuenta de usuario suspendida")),
            Map.entry("UsuarioServiceImpl.reactivarCuenta", new Regla(ModuloAuditoria.USUARIOS, AccionAuditoria.REACTIVAR, "Cuenta reactivada")),
            // Sedes (RF-66 a RF-70)
            Map.entry("SedeServiceImpl.crearSede", new Regla(ModuloAuditoria.SEDES, AccionAuditoria.CREAR, "Sede creada")),
            Map.entry("SedeServiceImpl.actualizarSede", new Regla(ModuloAuditoria.SEDES, AccionAuditoria.MODIFICAR, "Sede actualizada")),
            Map.entry("SedeServiceImpl.eliminarSede", new Regla(ModuloAuditoria.SEDES, AccionAuditoria.ELIMINAR, "Sede eliminada")),
            Map.entry("SedeServiceImpl.desactivarSede", new Regla(ModuloAuditoria.SEDES, AccionAuditoria.DESACTIVAR, "Sede desactivada")),
            Map.entry("SedeServiceImpl.reactivarSede", new Regla(ModuloAuditoria.SEDES, AccionAuditoria.REACTIVAR, "Sede reactivada")),
            // Planes de suscripción (RF-71 a RF-73)
            Map.entry("PlanSuscripcionServiceImpl.crearPlanSuscripcion", new Regla(ModuloAuditoria.PLANES, AccionAuditoria.CREAR, "Plan de suscripción creado")),
            Map.entry("PlanSuscripcionServiceImpl.actualizarPlanSuscripcion", new Regla(ModuloAuditoria.PLANES, AccionAuditoria.MODIFICAR, "Plan de suscripción modificado")),
            Map.entry("PlanSuscripcionServiceImpl.eliminarPlanSuscripcion", new Regla(ModuloAuditoria.PLANES, AccionAuditoria.ELIMINAR, "Plan de suscripción eliminado")),
            Map.entry("PlanSuscripcionServiceImpl.desactivarPlan", new Regla(ModuloAuditoria.PLANES, AccionAuditoria.DESACTIVAR, "Plan de suscripción desactivado")),
            Map.entry("PlanSuscripcionServiceImpl.reactivarPlan", new Regla(ModuloAuditoria.PLANES, AccionAuditoria.REACTIVAR, "Plan de suscripción reactivado")),
            // Políticas de sanción, multa y plazos (RF-74 a RF-76)
            Map.entry("ConfiguracionSistemaServiceImpl.actualizarPoliticaSancion", new Regla(ModuloAuditoria.POLITICAS, AccionAuditoria.CONFIGURAR, "Política de sanción actualizada")),
            Map.entry("ConfiguracionSistemaServiceImpl.actualizarPoliticaMulta", new Regla(ModuloAuditoria.POLITICAS, AccionAuditoria.CONFIGURAR, "Política de multa actualizada")),
            Map.entry("ConfiguracionSistemaServiceImpl.actualizarPlazo", new Regla(ModuloAuditoria.POLITICAS, AccionAuditoria.CONFIGURAR, "Plazo del sistema actualizado")),
            // Tarifas de envío (RF-77)
            Map.entry("TarifaEnvioServiceImpl.crearTarifa", new Regla(ModuloAuditoria.TARIFAS_ENVIO, AccionAuditoria.CREAR, "Tarifa de envío creada")),
            Map.entry("TarifaEnvioServiceImpl.actualizarTarifa", new Regla(ModuloAuditoria.TARIFAS_ENVIO, AccionAuditoria.MODIFICAR, "Tarifa de envío modificada")),
            Map.entry("TarifaEnvioServiceImpl.eliminarTarifa", new Regla(ModuloAuditoria.TARIFAS_ENVIO, AccionAuditoria.ELIMINAR, "Tarifa de envío eliminada")),
            // Base de conocimiento del chatbot (RF-81)
            Map.entry("PreguntaFrecuenteServiceImpl.crearPreguntaFrecuente", new Regla(ModuloAuditoria.CHATBOT, AccionAuditoria.CREAR, "Respuesta del chatbot creada")),
            Map.entry("PreguntaFrecuenteServiceImpl.actualizarPreguntaFrecuente", new Regla(ModuloAuditoria.CHATBOT, AccionAuditoria.MODIFICAR, "Respuesta del chatbot modificada")),
            Map.entry("PreguntaFrecuenteServiceImpl.eliminarPreguntaFrecuente", new Regla(ModuloAuditoria.CHATBOT, AccionAuditoria.ELIMINAR, "Respuesta del chatbot eliminada"))
    );

    private static final String[] GETTERS_ETIQUETA = {"getNombreUsuario", "getNombre", "getPregunta", "getInfraccion",
            "getConcepto", "getClave", "getCiudadOrigen"};

    private final AuditoriaService auditoriaService;

    @AfterReturning(pointcut = "execution(* bussinesLayer.service.impl.*ServiceImpl.*(..))", returning = "resultado")
    public void auditar(JoinPoint joinPoint, Object resultado) {
        String clave = joinPoint.getTarget().getClass().getSimpleName() + "." + joinPoint.getSignature().getName();
        Regla regla = REGLAS.get(clave);
        if (regla == null) {
            return;
        }
        try {
            Long entidadId = extraerId(resultado);
            Object[] args = joinPoint.getArgs();
            if (entidadId == null && args.length > 0 && args[0] instanceof Long id) {
                entidadId = id; // métodos void (eliminar...): el id viene como primer argumento
            }
            String etiqueta = extraerEtiqueta(resultado);
            if (etiqueta == null && args.length > 0 && args[0] instanceof String clavePolitica) {
                etiqueta = clavePolitica; // políticas y plazos se identifican por su clave
            }
            StringBuilder descripcion = new StringBuilder(regla.texto());
            if (etiqueta != null) {
                descripcion.append(": ").append(etiqueta);
            }
            if (entidadId != null) {
                descripcion.append(" (id ").append(entidadId).append(")");
            }
            auditoriaService.registrar(usuarioDeLaPeticion(), regla.accion(), regla.modulo(), entidadId, descripcion.toString());
        } catch (Exception e) {
            log.warn("No se pudo registrar la auditoría de {}: {}", clave, e.getMessage());
        }
    }

    private Long usuarioDeLaPeticion() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null; // fuera de una petición HTTP (tarea programada, pruebas...)
        }
        String valor = attrs.getRequest().getParameter("administradorId");
        if (valor == null || valor.isBlank()) {
            valor = attrs.getRequest().getHeader("X-Usuario-Id");
        }
        try {
            return valor == null || valor.isBlank() ? null : Long.valueOf(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long extraerId(Object objeto) {
        Object id = invocar(objeto, "getId");
        return id instanceof Long l ? l : null;
    }

    private String extraerEtiqueta(Object objeto) {
        for (String getter : GETTERS_ETIQUETA) {
            Object valor = invocar(objeto, getter);
            if (valor instanceof String s && !s.isBlank()) {
                return s.length() > 120 ? s.substring(0, 117) + "..." : s;
            }
        }
        return null;
    }

    private Object invocar(Object objeto, String getter) {
        if (objeto == null) {
            return null;
        }
        try {
            Method m = objeto.getClass().getMethod(getter);
            return m.invoke(objeto);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
