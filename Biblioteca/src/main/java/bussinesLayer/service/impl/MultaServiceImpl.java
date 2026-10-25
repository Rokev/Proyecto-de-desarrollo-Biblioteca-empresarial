package bussinesLayer.service.impl;

import bussinesLayer.dto.MultaDTO;
import bussinesLayer.dto.NotificacionDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.ConfiguracionSistemaService;
import bussinesLayer.service.MultaService;
import bussinesLayer.util.ConceptoMulta;
import bussinesLayer.util.InfraccionSancion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.MultaDAO;
import persistanceLayer.dao.NotificacionDAO;
import persistanceLayer.dao.PrestamoDAO;
import persistanceLayer.dao.SancionDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MultaServiceImpl implements MultaService {

    private static final String ESTADO_INICIAL = "PENDIENTE";

    private final MultaDAO multaDAO;
    private final UsuarioDAO usuarioDAO;
    private final PrestamoDAO prestamoDAO;
    private final NotificacionDAO notificacionDAO;
    private final SancionDAO sancionDAO;
    private final ConfiguracionSistemaService configuracionService;

    @Override
    public MultaDTO crearMulta(MultaDTO multaDTO) {
        log.info("Creando multa para el usuario: {}", multaDTO.getUsuarioId());

        validarReferencias(multaDTO);

        if (multaDTO.getEstado() == null || multaDTO.getEstado().isBlank()) {
            multaDTO.setEstado(ESTADO_INICIAL);
        }
        if (multaDTO.getFechaGeneracion() == null) {
            multaDTO.setFechaGeneracion(LocalDateTime.now());
        }

        MultaDTO guardada = multaDAO.save(multaDTO);
        log.info("Multa creada con ID: {}", guardada.getId());
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public MultaDTO obtenerMultaPorId(Long id) {
        return multaDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Multa no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MultaDTO> obtenerTodasLasMultas() {
        return multaDAO.findAll();
    }

    @Override
    public MultaDTO actualizarMulta(Long id, MultaDTO multaDTO) {
        if (!multaDAO.existsById(id)) {
            throw new ResourceNotFoundException("Multa no encontrada con ID: " + id);
        }

        validarReferencias(multaDTO);

        MultaDTO actualizada = multaDAO.update(id, multaDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Multa no encontrada con ID: " + id));
        log.info("Multa actualizada ID: {}", id);
        return actualizada;
    }

    @Override
    public void eliminarMulta(Long id) {
        if (!multaDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Multa no encontrada con ID: " + id);
        }
        log.info("Multa eliminada ID: {}", id);
    }


    @Override
    @Transactional(readOnly = true)
    public List<MultaDTO> obtenerMultasPorUsuario(Long usuarioId, String estado) {
        if (!usuarioDAO.existsById(usuarioId)) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId);
        }
        if (estado == null || estado.isBlank()) {
            return multaDAO.findByUsuarioIdOrderByFechaGeneracionDesc(usuarioId);
        }
        return multaDAO.findByUsuarioIdAndEstadoIgnoreCase(usuarioId, estado);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal obtenerSaldoPendienteUsuario(Long usuarioId) {
        if (!usuarioDAO.existsById(usuarioId)) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId);
        }
        return multaDAO.totalPendientePorUsuario(usuarioId);
    }

    @Override
    public MultaDTO generarMultaPorDanioOPerdida(Long usuarioId, Long prestamoId, String tipo, BigDecimal monto, String motivo) {
        boolean perdido = "PERDIDO".equalsIgnoreCase(tipo);
        if (!perdido && !"DANADO".equalsIgnoreCase(tipo)) {
            throw new BusinessValidationException("El tipo debe ser DANADO o PERDIDO");
        }
        if (!usuarioDAO.existsById(usuarioId)) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId);
        }
        if (prestamoId != null && !prestamoDAO.existsById(prestamoId)) {
            throw new ResourceNotFoundException("Préstamo no encontrado con ID: " + prestamoId);
        }
        //  75: sin monto explicito se cobra el configurado por el administrador.
        if (monto == null) {
            monto = configuracionService.montoMulta(perdido ? ConceptoMulta.LIBRO_PERDIDO : ConceptoMulta.LIBRO_DANADO);
        }
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("El monto de la multa debe ser mayor a 0");
        }

        MultaDTO multa = new MultaDTO();
        multa.setUsuarioId(usuarioId);
        multa.setPrestamoId(prestamoId);
        multa.setMonto(monto);
        multa.setMotivo(motivo != null && !motivo.isBlank() ? motivo : "Multa por ejemplar dañado o perdido");
        multa.setEstado(ESTADO_INICIAL);
        multa.setFechaGeneracion(LocalDateTime.now());

        MultaDTO guardada = multaDAO.save(multa);

        //  74: sancion segun la politica de la infraccion (por defecto no aplica sancion en daño/perdida).
        configuracionService.construirSancion(perdido ? InfraccionSancion.LIBRO_PERDIDO : InfraccionSancion.LIBRO_DANADO,
                usuarioId, prestamoId, (perdido ? "Libro perdido" : "Libro dañado")
                        + (prestamoId != null ? " (prestamo #" + prestamoId + ")" : ""))
                .ifPresent(sancionDAO::save);

        // Notificar al usuario
        NotificacionDTO notif = new NotificacionDTO();
        notif.setUsuarioId(usuarioId);
        notif.setTipo("MULTA_GENERADA");
        notif.setTitulo("Multa aplicada a tu cuenta");
        notif.setMensaje("Se ha registrado una multa de $" + monto + " por el siguiente motivo: " + multa.getMotivo());
        notif.setCanal("APP");
        notif.setFechaCreacion(LocalDateTime.now());
        notif.setLeida(false);
        notificacionDAO.save(notif);

        log.info("Multa por daño/pérdida generada con ID {} para usuario {}", guardada.getId(), usuarioId);
        return guardada;
    }

    private void validarReferencias(MultaDTO dto) {
        if (!usuarioDAO.existsById(dto.getUsuarioId())) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + dto.getUsuarioId());
        }
        if (dto.getPrestamoId() != null && !prestamoDAO.existsById(dto.getPrestamoId())) {
            throw new ResourceNotFoundException("Préstamo no encontrado con ID: " + dto.getPrestamoId());
        }
    }
}
