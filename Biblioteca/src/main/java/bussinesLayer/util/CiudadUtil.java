package bussinesLayer.util;

import bussinesLayer.exception.BusinessValidationException;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/** Deja los nombres de ciudad en un formato único: sin tildes y en mayúscula inicial ("medellín" -> "Medellin"). */
public final class CiudadUtil {

    private CiudadUtil() {
    }

    public static String normalizar(String ciudad) {
        if (ciudad == null || ciudad.isBlank()) {
            throw new BusinessValidationException("La ciudad es obligatoria");
        }
        String sinTildes = Normalizer.normalize(ciudad.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return Arrays.stream(sinTildes.toLowerCase(Locale.ROOT).split("\\s+"))
                .map(p -> Character.toUpperCase(p.charAt(0)) + p.substring(1))
                .collect(Collectors.joining(" "));
    }
}