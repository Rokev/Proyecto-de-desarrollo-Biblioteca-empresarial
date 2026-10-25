package bussinesLayer.util;

import bussinesLayer.dto.PoliticaSancionDTO;
import bussinesLayer.service.ConfiguracionSistemaService;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reemplaza variables del tipo {DIAS_PRESTAMO} en un texto por los valores vigentes de la configuracion
 *  para que las respuestas del chatbot nunca queden desactualizadas.
 *
 * <ul>
 *   <li>{CLAVE_PLAZO}: {DIAS_PRESTAMO}, {DIAS_PRESTAMO_INTERCIUDAD}, {HORAS_RETIRO_LIBRO}, {DIAS_RECOGIDA_SEDE_DEVOLUCION}</li>
 *   <li>{MULTA_CONCEPTO}: {MULTA_MORA_POR_DIA}, {MULTA_LIBRO_DANADO}, {MULTA_LIBRO_PERDIDO}, {MULTA_PEDIDO_NO_RECOGIDO},
 *       {MULTA_RECARGO_PEDIDO_NO_RECIBIDO} (monto sin signo $)</li>
 *   <li>{SANCION_INFRACCION}: frase con la restriccion, p. ej. "un bloqueo de prestamos por 7 dias"</li>
 * </ul>
 * Una variable desconocida se deja tal cual.
 */
public final class VariablesConfiguracion {

    private static final Pattern VARIABLE = Pattern.compile("\\{([A-Z_]+)\\}");

    private VariablesConfiguracion() {
    }

    public static String resolver(String texto, ConfiguracionSistemaService config) {
        if (texto == null || texto.indexOf('{') < 0) {
            return texto;
        }
        Matcher m = VARIABLE.matcher(texto);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String valor = valorDe(m.group(1), config);
            m.appendReplacement(sb, Matcher.quoteReplacement(valor != null ? valor : m.group()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String valorDe(String nombre, ConfiguracionSistemaService config) {
        try {
            if (nombre.startsWith("MULTA_")) {
                ConceptoMulta c = ConceptoMulta.valueOf(nombre.substring("MULTA_".length()));
                NumberFormat formato = NumberFormat.getNumberInstance(new Locale("es", "CO"));
                formato.setMaximumFractionDigits(2);
                return formato.format(config.montoMulta(c));
            }
            if (nombre.startsWith("SANCION_")) {
                return fraseSancion(config.obtenerPoliticaSancion(nombre.substring("SANCION_".length())));
            }
            return String.valueOf(config.plazo(ClavePlazo.valueOf(nombre)));
        } catch (IllegalArgumentException e) {
            return null; // variable desconocida (o valor de configuracion invalido): se deja el texto original
        } catch (RuntimeException e) {
            return null; // p. ej. BusinessValidationException por un nombre de infraccion desconocido
        }
    }

    private static String fraseSancion(PoliticaSancionDTO p) {
        if (!Boolean.TRUE.equals(p.getActiva())) {
            return "ninguna restriccion adicional";
        }
        if (InfraccionSancion.TipoSancion.ADVERTENCIA.equals(p.getTipoSancion())) {
            return "una advertencia";
        }
        String base = InfraccionSancion.TipoSancion.SUSPENSION_CUENTA.equals(p.getTipoSancion())
                ? "la suspension de tu cuenta" : "un bloqueo de prestamos";
        if (p.getDuracionDias() == null) {
            return base + " hasta que devuelvas el libro";
        }
        return base + " por " + p.getDuracionDias() + (p.getDuracionDias() == 1 ? " dia" : " dias");
    }
}
