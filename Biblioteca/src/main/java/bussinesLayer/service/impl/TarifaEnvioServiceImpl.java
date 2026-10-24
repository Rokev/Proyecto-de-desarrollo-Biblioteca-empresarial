package bussinesLayer.service.impl;

import bussinesLayer.dto.CotizacionEnvioDTO;
import bussinesLayer.dto.TarifaEnvioDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.TarifaEnvioService;
import bussinesLayer.util.CiudadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.TarifaEnvioDAO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TarifaEnvioServiceImpl implements TarifaEnvioService {

    private final TarifaEnvioDAO tarifaEnvioDAO;

    @Override
    public TarifaEnvioDTO crearTarifa(TarifaEnvioDTO dto) {
        prepararPar(dto);
        if (tarifaEnvioDAO.findByPar(dto.getCiudadA(), dto.getCiudadB()).isPresent()) {
            throw new BusinessValidationException(
                    "Ya existe una tarifa para " + dto.getCiudadA() + " - " + dto.getCiudadB());
        }
        TarifaEnvioDTO guardada = tarifaEnvioDAO.save(dto);
        log.info("AUDITORIA tarifa creada: {} - {} = {}", guardada.getCiudadA(), guardada.getCiudadB(), guardada.getTarifa());
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public TarifaEnvioDTO obtenerTarifaPorId(Long id) {
        return tarifaEnvioDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarifa de envío no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TarifaEnvioDTO> obtenerTodasLasTarifas() {
        return tarifaEnvioDAO.findAll();
    }

    @Override
    public TarifaEnvioDTO actualizarTarifa(Long id, TarifaEnvioDTO dto) {
        TarifaEnvioDTO anterior = obtenerTarifaPorId(id);
        prepararPar(dto);
        tarifaEnvioDAO.findByPar(dto.getCiudadA(), dto.getCiudadB())
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new BusinessValidationException(
                            "Ya existe otra tarifa para " + dto.getCiudadA() + " - " + dto.getCiudadB());
                });
        TarifaEnvioDTO actualizada = tarifaEnvioDAO.update(id, dto)
                .orElseThrow(() -> new ResourceNotFoundException("Tarifa de envío no encontrada con ID: " + id));
        log.info("AUDITORIA tarifa {} modificada: {} - {} de {} a {}", id,
                actualizada.getCiudadA(), actualizada.getCiudadB(), anterior.getTarifa(), actualizada.getTarifa());
        return actualizada;
    }

    @Override
    public void eliminarTarifa(Long id) {
        TarifaEnvioDTO anterior = obtenerTarifaPorId(id);
        tarifaEnvioDAO.deleteById(id);
        log.info("AUDITORIA tarifa {} eliminada: {} - {} (valía {})", id,
                anterior.getCiudadA(), anterior.getCiudadB(), anterior.getTarifa());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> buscarCosto(String ciudadOrigen, String ciudadDestino) {
        String origen = CiudadUtil.normalizar(ciudadOrigen);
        String destino = CiudadUtil.normalizar(ciudadDestino);
        if (origen.equals(destino)) {
            return Optional.empty();
        }
        String a = origen.compareTo(destino) < 0 ? origen : destino;
        String b = origen.compareTo(destino) < 0 ? destino : origen;
        return tarifaEnvioDAO.findByPar(a, b).map(TarifaEnvioDTO::getTarifa);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calcularCosto(String ciudadOrigen, String ciudadDestino) {
        return buscarCosto(ciudadOrigen, ciudadDestino).orElseThrow(() -> new BusinessValidationException(
                "No hay tarifa de envío cargada entre " + CiudadUtil.normalizar(ciudadOrigen)
                        + " y " + CiudadUtil.normalizar(ciudadDestino)));
    }

    @Override
    @Transactional(readOnly = true)
    public CotizacionEnvioDTO cotizar(String ciudadOrigen, String ciudadDestino) {
        return new CotizacionEnvioDTO(CiudadUtil.normalizar(ciudadOrigen), CiudadUtil.normalizar(ciudadDestino),
                calcularCosto(ciudadOrigen, ciudadDestino));
    }

    /** Normaliza las dos ciudades, exige que sean distintas y las deja en orden alfabético (ciudadA < ciudadB). */
    private void prepararPar(TarifaEnvioDTO dto) {
        String a = CiudadUtil.normalizar(dto.getCiudadA());
        String b = CiudadUtil.normalizar(dto.getCiudadB());
        if (a.equals(b)) {
            throw new BusinessValidationException("Las dos ciudades de la tarifa deben ser distintas");
        }
        if (a.compareTo(b) > 0) {
            String tmp = a;
            a = b;
            b = tmp;
        }
        dto.setCiudadA(a);
        dto.setCiudadB(b);
    }
}