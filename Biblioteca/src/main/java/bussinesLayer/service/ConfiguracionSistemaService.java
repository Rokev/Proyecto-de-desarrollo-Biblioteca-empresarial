package bussinesLayer.service;

import bussinesLayer.dto.PlazoSistemaDTO;
import bussinesLayer.dto.PoliticaMultaDTO;
import bussinesLayer.dto.PoliticaSancionDTO;
import bussinesLayer.dto.SancionDTO;
import bussinesLayer.util.ClavePlazo;
import bussinesLayer.util.ConceptoMulta;
import bussinesLayer.util.InfraccionSancion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Configuración de políticas de sanción (RF-74), multas (RF-75) y plazos (RF-76).
 * El resto de servicios consultan aquí los valores en lugar de tenerlos escritos en el código.
 */
public interface ConfiguracionSistemaService {

    // ---------------- RF-74: sanciones (administración)
    List<PoliticaSancionDTO> listarPoliticasSancion();

    PoliticaSancionDTO obtenerPoliticaSancion(String infraccion);

    PoliticaSancionDTO actualizarPoliticaSancion(String infraccion, PoliticaSancionDTO dto);

    // ---------------- RF-75: multas (administración)
    List<PoliticaMultaDTO> listarPoliticasMulta();

    PoliticaMultaDTO obtenerPoliticaMulta(String concepto);

    PoliticaMultaDTO actualizarPoliticaMulta(String concepto, PoliticaMultaDTO dto);

    // ---------------- RF-76: plazos (administración)
    List<PlazoSistemaDTO> listarPlazos();

    PlazoSistemaDTO obtenerPlazo(String clave);

    PlazoSistemaDTO actualizarPlazo(String clave, PlazoSistemaDTO dto);

    // ---------------- Consulta para el resto del sistema (usa el valor por defecto si no hay fila)

    /** Monto vigente de un concepto de multa. */
    BigDecimal montoMulta(ConceptoMulta concepto);

    /** Valor vigente de un plazo, en su unidad (días u horas). */
    int plazo(ClavePlazo clave);

    /**
     * Arma (sin guardar) la sanción que corresponde a la infracción según su política.
     * Vacío si la política está desactivada. El llamador la guarda con SancionDAO.
     */
    Optional<SancionDTO> construirSancion(InfraccionSancion infraccion, Long usuarioId, Long prestamoId, String motivo);

    /** Crea las políticas, montos y plazos por defecto que falten (se llama al arrancar). */
    void cargarValoresPorDefecto();
}
