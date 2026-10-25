package bussinesLayer.service.impl;

import bussinesLayer.dto.AuditoriaDTO;
import bussinesLayer.dto.UsuarioDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.AuditoriaDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuditoriaServiceImpl implements AuditoriaService {

    private static final int TAMANIO_MAXIMO = 200;

    private final AuditoriaDAO auditoriaDAO;
    private final UsuarioDAO usuarioDAO;

    @Override
    public AuditoriaDTO registrar(Long usuarioId, String accion, String modulo, Long entidadId, String descripcion) {
        if (accion == null || accion.isBlank()) {
            throw new BusinessValidationException("La acción de auditoría es obligatoria");
        }
        if (modulo == null || modulo.isBlank()) {
            throw new BusinessValidationException("El módulo de auditoría es obligatorio");
        }
        AuditoriaDTO dto = new AuditoriaDTO();
        dto.setFecha(LocalDateTime.now());
        dto.setUsuarioId(usuarioId);
        dto.setUsuarioNombre(resolverNombre(usuarioId));
        dto.setAccion(accion.trim().toUpperCase());
        dto.setModulo(modulo.trim().toUpperCase());
        dto.setEntidadId(entidadId);
        dto.setDescripcion(recortar(descripcion == null || descripcion.isBlank() ? "(sin descripción)" : descripcion));

        AuditoriaDTO guardado = auditoriaDAO.save(dto);
        log.info("Auditoría [{}|{}] usuario={} entidad={}", guardado.getModulo(), guardado.getAccion(),
                usuarioId, entidadId);
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public AuditoriaDTO obtenerPorId(Long id) {
        return auditoriaDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de auditoría no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public AuditoriaDAO.Pagina consultar(Long usuarioId, LocalDate desde, LocalDate hasta,
                                         String accion, String modulo, int pagina, int tamanio) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new BusinessValidationException("La fecha 'hasta' no puede ser anterior a la fecha 'desde'");
        }
        if (pagina < 0) {
            throw new BusinessValidationException("La página no puede ser negativa");
        }
        if (tamanio < 1 || tamanio > TAMANIO_MAXIMO) {
            throw new BusinessValidationException("El tamaño de página debe estar entre 1 y " + TAMANIO_MAXIMO);
        }
        LocalDateTime inicio = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime finExclusivo = hasta != null ? hasta.plusDays(1).atStartOfDay() : null;
        return auditoriaDAO.buscar(usuarioId, inicio, finExclusivo, vacioANulo(accion), vacioANulo(modulo),
                pagina, tamanio);
    }

    private String resolverNombre(Long usuarioId) {
        if (usuarioId == null) {
            return "SISTEMA";
        }
        return usuarioDAO.findById(usuarioId)
                .map(UsuarioDTO::getNombreUsuario)
                .orElse("usuario#" + usuarioId);
    }

    private String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String recortar(String texto) {
        return texto.length() <= 500 ? texto : texto.substring(0, 497) + "...";
    }
}
