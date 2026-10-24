package bussinesLayer.service;

import bussinesLayer.dto.CotizacionEnvioDTO;
import bussinesLayer.dto.TarifaEnvioDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Gestión de las tarifas de envío por pareja de ciudades */
public interface TarifaEnvioService {

    TarifaEnvioDTO crearTarifa(TarifaEnvioDTO dto);

    TarifaEnvioDTO obtenerTarifaPorId(Long id);

    List<TarifaEnvioDTO> obtenerTodasLasTarifas();

    TarifaEnvioDTO actualizarTarifa(Long id, TarifaEnvioDTO dto);

    void eliminarTarifa(Long id);

    /** Vacío si no hay tarifa cargada para la pareja (o si las dos ciudades son la misma). */
    Optional<BigDecimal> buscarCosto(String ciudadOrigen, String ciudadDestino);

    /** Lanza BusinessValidationException si la pareja no tiene tarifa. */
    BigDecimal calcularCosto(String ciudadOrigen, String ciudadDestino);

    CotizacionEnvioDTO cotizar(String ciudadOrigen, String ciudadDestino);
}