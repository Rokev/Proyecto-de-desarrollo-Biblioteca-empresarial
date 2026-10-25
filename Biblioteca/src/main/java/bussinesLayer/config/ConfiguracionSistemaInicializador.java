package bussinesLayer.config;

import bussinesLayer.service.ConfiguracionSistemaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Al arrancar, carga las políticas y plazos por defecto que falten (no pisa lo ya configurado). */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConfiguracionSistemaInicializador implements ApplicationRunner {

    private final ConfiguracionSistemaService configuracionSistemaService;

    @Override
    public void run(ApplicationArguments args) {
        configuracionSistemaService.cargarValoresPorDefecto();
        log.info("Configuración del sistema (sanciones, multas y plazos) verificada");
    }
}
