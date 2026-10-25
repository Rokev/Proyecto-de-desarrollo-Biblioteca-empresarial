package bussinesLayer.service.impl;

import bussinesLayer.dto.PlazoSistemaDTO;
import bussinesLayer.dto.PoliticaMultaDTO;
import bussinesLayer.dto.PoliticaSancionDTO;
import bussinesLayer.dto.SancionDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.service.ConfiguracionSistemaService;
import bussinesLayer.util.ClavePlazo;
import bussinesLayer.util.ConceptoMulta;
import bussinesLayer.util.InfraccionSancion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.PlazoSistemaDAO;
import persistanceLayer.dao.PoliticaMultaDAO;
import persistanceLayer.dao.PoliticaSancionDAO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ConfiguracionSistemaServiceImpl implements ConfiguracionSistemaService {

    private static final int MAX_DURACION_SANCION_DIAS = 365;

    private final PoliticaSancionDAO politicaSancionDAO;
    private final PoliticaMultaDAO politicaMultaDAO;
    private final PlazoSistemaDAO plazoSistemaDAO;

    // ------------------------------------------------------------------  74

    @Override
    @Transactional(readOnly = true)
    public List<PoliticaSancionDTO> listarPoliticasSancion() {
        return Arrays.stream(InfraccionSancion.values()).map(this::politicaSancionVigente).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PoliticaSancionDTO obtenerPoliticaSancion(String infraccion) {
        return politicaSancionVigente(parsear(InfraccionSancion.class, infraccion, "infracción"));
    }

    @Override
    public PoliticaSancionDTO actualizarPoliticaSancion(String infraccion, PoliticaSancionDTO dto) {
        InfraccionSancion inf = parsear(InfraccionSancion.class, infraccion, "infracción");
        String tipo = dto.getTipoSancion() == null ? "" : dto.getTipoSancion().trim().toUpperCase();
        if (!InfraccionSancion.TipoSancion.TODOS.contains(tipo)) {
            throw new BusinessValidationException("Tipo de sanción inválido. Valores permitidos: "
                    + InfraccionSancion.TipoSancion.TODOS);
        }
        Integer dias = dto.getDuracionDias();
        if (dias == null && !inf.permiteDuracionIndefinida()) {
            throw new BusinessValidationException("La infracción " + inf + " exige indicar duracionDias");
        }
        if (dias != null && (dias < 1 || dias > MAX_DURACION_SANCION_DIAS)) {
            throw new BusinessValidationException(
                    "duracionDias debe estar entre 1 y " + MAX_DURACION_SANCION_DIAS);
        }

        PoliticaSancionDTO anterior = politicaSancionVigente(inf);
        PoliticaSancionDTO nueva = new PoliticaSancionDTO();
        nueva.setInfraccion(inf.name());
        nueva.setTipoSancion(tipo);
        nueva.setDuracionDias(dias);
        nueva.setActiva(dto.getActiva());
        nueva.setDescripcion(inf.getDescripcion());
        PoliticaSancionDTO guardada = politicaSancionDAO.guardar(nueva);
        log.info("AUDITORIA politica de sancion {} modificada: tipo {} -> {}, duracionDias {} -> {}, activa {} -> {}",
                inf, anterior.getTipoSancion(), guardada.getTipoSancion(), anterior.getDuracionDias(),
                guardada.getDuracionDias(), anterior.getActiva(), guardada.getActiva());
        return guardada;
    }

    // ------------------------------------------------------------------  75

    @Override
    @Transactional(readOnly = true)
    public List<PoliticaMultaDTO> listarPoliticasMulta() {
        return Arrays.stream(ConceptoMulta.values()).map(this::politicaMultaVigente).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PoliticaMultaDTO obtenerPoliticaMulta(String concepto) {
        return politicaMultaVigente(parsear(ConceptoMulta.class, concepto, "concepto"));
    }

    @Override
    public PoliticaMultaDTO actualizarPoliticaMulta(String concepto, PoliticaMultaDTO dto) {
        ConceptoMulta c = parsear(ConceptoMulta.class, concepto, "concepto");
        if (dto.getMonto() == null || dto.getMonto().signum() <= 0) {
            throw new BusinessValidationException("El monto debe ser mayor que cero");
        }
        PoliticaMultaDTO anterior = politicaMultaVigente(c);
        PoliticaMultaDTO nueva = new PoliticaMultaDTO();
        nueva.setConcepto(c.name());
        nueva.setMonto(dto.getMonto());
        nueva.setDescripcion(c.getDescripcion());
        PoliticaMultaDTO guardada = politicaMultaDAO.guardar(nueva);
        log.info("AUDITORIA politica de multa {} modificada: monto {} -> {}", c, anterior.getMonto(), guardada.getMonto());
        return guardada;
    }

    // ------------------------------------------------------------------  76

    @Override
    @Transactional(readOnly = true)
    public List<PlazoSistemaDTO> listarPlazos() {
        return Arrays.stream(ClavePlazo.values()).map(this::plazoVigente).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PlazoSistemaDTO obtenerPlazo(String clave) {
        return plazoVigente(parsear(ClavePlazo.class, clave, "plazo"));
    }

    @Override
    public PlazoSistemaDTO actualizarPlazo(String clave, PlazoSistemaDTO dto) {
        ClavePlazo c = parsear(ClavePlazo.class, clave, "plazo");
        if (dto.getValor() == null || dto.getValor() < c.getMinimo() || dto.getValor() > c.getMaximo()) {
            throw new BusinessValidationException("El plazo " + c + " debe estar entre " + c.getMinimo()
                    + " y " + c.getMaximo() + " " + c.getUnidad().toLowerCase());
        }
        PlazoSistemaDTO anterior = plazoVigente(c);
        PlazoSistemaDTO nuevo = new PlazoSistemaDTO();
        nuevo.setClave(c.name());
        nuevo.setValor(dto.getValor());
        nuevo.setUnidad(c.getUnidad());
        nuevo.setDescripcion(c.getDescripcion());
        PlazoSistemaDTO guardado = plazoSistemaDAO.guardar(nuevo);
        log.info("AUDITORIA plazo {} modificado: {} -> {} {}", c, anterior.getValor(), guardado.getValor(), c.getUnidad());
        return guardado;
    }

    // ------------------------------------------------- Consulta para otros servicios

    @Override
    @Transactional(readOnly = true)
    public BigDecimal montoMulta(ConceptoMulta concepto) {
        return politicaMultaVigente(concepto).getMonto();
    }

    @Override
    @Transactional(readOnly = true)
    public int plazo(ClavePlazo clave) {
        return plazoVigente(clave).getValor();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SancionDTO> construirSancion(InfraccionSancion infraccion, Long usuarioId, Long prestamoId, String motivo) {
        PoliticaSancionDTO politica = politicaSancionVigente(infraccion);
        if (!Boolean.TRUE.equals(politica.getActiva())) {
            return Optional.empty();
        }
        LocalDateTime ahora = LocalDateTime.now();
        SancionDTO sancion = new SancionDTO();
        sancion.setTipo(politica.getTipoSancion());
        sancion.setMotivo(motivo);
        sancion.setFechaAplicacion(ahora);
        sancion.setFechaFin(politica.getDuracionDias() == null ? null : ahora.plusDays(politica.getDuracionDias()));
        sancion.setEstado("ACTIVA");
        sancion.setUsuarioId(usuarioId);
        sancion.setPrestamoId(prestamoId);
        return Optional.of(sancion);
    }

    @Override
    public void cargarValoresPorDefecto() {
        for (InfraccionSancion i : InfraccionSancion.values()) {
            if (politicaSancionDAO.findByInfraccion(i.name()).isEmpty()) {
                politicaSancionDAO.guardar(sancionPorDefecto(i));
            }
        }
        for (ConceptoMulta c : ConceptoMulta.values()) {
            if (politicaMultaDAO.findByConcepto(c.name()).isEmpty()) {
                politicaMultaDAO.guardar(multaPorDefecto(c));
            }
        }
        for (ClavePlazo p : ClavePlazo.values()) {
            if (plazoSistemaDAO.findByClave(p.name()).isEmpty()) {
                plazoSistemaDAO.guardar(plazoPorDefecto(p));
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private PoliticaSancionDTO politicaSancionVigente(InfraccionSancion i) {
        return politicaSancionDAO.findByInfraccion(i.name()).orElseGet(() -> sancionPorDefecto(i));
    }

    private PoliticaMultaDTO politicaMultaVigente(ConceptoMulta c) {
        return politicaMultaDAO.findByConcepto(c.name()).orElseGet(() -> multaPorDefecto(c));
    }

    private PlazoSistemaDTO plazoVigente(ClavePlazo p) {
        return plazoSistemaDAO.findByClave(p.name()).orElseGet(() -> plazoPorDefecto(p));
    }

    private PoliticaSancionDTO sancionPorDefecto(InfraccionSancion i) {
        PoliticaSancionDTO dto = new PoliticaSancionDTO();
        dto.setInfraccion(i.name());
        dto.setTipoSancion(i.getTipoPorDefecto());
        dto.setDuracionDias(i.getDuracionDiasPorDefecto());
        dto.setActiva(i.isActivaPorDefecto());
        dto.setDescripcion(i.getDescripcion());
        return dto;
    }

    private PoliticaMultaDTO multaPorDefecto(ConceptoMulta c) {
        PoliticaMultaDTO dto = new PoliticaMultaDTO();
        dto.setConcepto(c.name());
        dto.setMonto(c.getMontoPorDefecto());
        dto.setDescripcion(c.getDescripcion());
        return dto;
    }

    private PlazoSistemaDTO plazoPorDefecto(ClavePlazo p) {
        PlazoSistemaDTO dto = new PlazoSistemaDTO();
        dto.setClave(p.name());
        dto.setValor(p.getValorPorDefecto());
        dto.setUnidad(p.getUnidad());
        dto.setDescripcion(p.getDescripcion());
        return dto;
    }

    private <E extends Enum<E>> E parsear(Class<E> tipo, String valor, String nombre) {
        try {
            return Enum.valueOf(tipo, valor == null ? "" : valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessValidationException("Valor de " + nombre + " desconocido: " + valor
                    + ". Valores permitidos: " + Arrays.toString(tipo.getEnumConstants()));
        }
    }
}
