package bussinesLayer.service.impl;

import bussinesLayer.dto.MultaDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.ValidadorUsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.MultaDAO;
import persistanceLayer.dao.SancionDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ValidadorUsuarioServiceImpl implements ValidadorUsuarioService {

    private final UsuarioDAO usuarioDAO;
    private final SancionDAO sancionDAO;
    private final MultaDAO multaDAO;

    @Override
    public void validarUsuarioHabilitadoParaOperaciones(Long usuarioId) {
        var usuario = usuarioDAO.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BusinessValidationException("La cuenta del usuario esta suspendida o desactivada.");
        }

        // 1. Validar Sanciones Activas
        if (sancionDAO.existsByUsuarioIdAndTipoIgnoreCaseAndEstadoIgnoreCase(usuarioId, "BLOQUEO_PRESTAMOS", "ACTIVA") ||
            sancionDAO.existsByUsuarioIdAndTipoIgnoreCaseAndEstadoIgnoreCase(usuarioId, "SUSPENSION_CUENTA", "ACTIVA")) {
            throw new BusinessValidationException("El usuario posee una sanción activa que le impide realizar esta operación.");
        }

        // 2. Validar Multas Pendientes de Pago
        BigDecimal saldoPendiente = multaDAO.totalPendientePorUsuario(usuarioId);
        if (saldoPendiente.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessValidationException("El usuario posee multas pendientes por un total de $" + saldoPendiente + ". Debe saldarlas antes de continuar.");
        }
    }
}