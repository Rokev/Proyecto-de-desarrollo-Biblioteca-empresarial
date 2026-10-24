package bussinesLayer.config;

import bussinesLayer.dto.TarifaEnvioDTO;
import bussinesLayer.service.TarifaEnvioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TarifaEnvioInicializador implements ApplicationRunner {

    private record Par(String a, String b, String tarifa) {
    }

    private static final List<Par> TARIFAS_INICIALES = List.of(
            new Par("Armenia", "Pereira", "12000"),
            new Par("Pereira", "Manizales", "12000"),
            new Par("Armenia", "Manizales", "18000"),
            new Par("Armenia", "Cali", "18000"),
            new Par("Pereira", "Cali", "20000"),
            new Par("Manizales", "Cali", "25000"),
            new Par("Pereira", "Medellin", "20000"),
            new Par("Manizales", "Medellin", "20000"),
            new Par("Armenia", "Medellin", "25000"),
            new Par("Medellin", "Cali", "30000"),
            new Par("Armenia", "Bogota", "28000"),
            new Par("Pereira", "Bogota", "30000"),
            new Par("Manizales", "Bogota", "30000"),
            new Par("Medellin", "Bogota", "32000"),
            new Par("Cali", "Bogota", "35000"));

    private final TarifaEnvioService tarifaEnvioService;

    @Override
    public void run(ApplicationArguments args) {
        if (!tarifaEnvioService.obtenerTodasLasTarifas().isEmpty()) {
            return;
        }
        for (Par p : TARIFAS_INICIALES) {
            TarifaEnvioDTO dto = new TarifaEnvioDTO();
            dto.setCiudadA(p.a());
            dto.setCiudadB(p.b());
            dto.setTarifa(new BigDecimal(p.tarifa()));
            tarifaEnvioService.crearTarifa(dto);
        }
        log.info("Tarifas de envío iniciales cargadas: {}", TARIFAS_INICIALES.size());
    }
}