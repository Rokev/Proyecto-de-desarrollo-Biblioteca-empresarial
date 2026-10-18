package bussinesLayer.service.impl;

import bussinesLayer.dto.PagoDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.PagoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.MetodoPagoDAO;
import persistanceLayer.dao.MultaDAO;
import persistanceLayer.dao.PagoDAO;
import persistanceLayer.dao.ReservaSalaDAO;
import persistanceLayer.dao.SuscripcionDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PagoServiceImpl implements PagoService {

    private static final String CONCEPTO_MULTA = "MULTA";
    private static final String CONCEPTO_SUSCRIPCION = "SUSCRIPCION";
    private static final String CONCEPTO_RESERVA_SALA = "RESERVA_SALA";

    private final PagoDAO pagoDAO;
    private final UsuarioDAO usuarioDAO;
    private final MetodoPagoDAO metodoPagoDAO;
    private final MultaDAO multaDAO;
    private final SuscripcionDAO suscripcionDAO;
    private final ReservaSalaDAO reservaSalaDAO;

    @Override
    public PagoDTO crearPago(PagoDTO pagoDTO) {
        log.info("Creando pago con referencia: {}", pagoDTO.getReferencia());

        if (pagoDAO.existsByReferencia(pagoDTO.getReferencia())) {
            throw new BusinessValidationException(
                    "Ya existe un pago registrado con la referencia: " + pagoDTO.getReferencia());
        }
        validarPago(pagoDTO);

        PagoDTO guardado = pagoDAO.save(pagoDTO);
        log.info("Pago creado con ID: {}", guardado.getId());
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public PagoDTO obtenerPagoPorId(Long id) {
        return pagoDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoDTO> obtenerTodosLosPagos() {
        return pagoDAO.findAll();
    }

    @Override
    public PagoDTO actualizarPago(Long id, PagoDTO pagoDTO) {
        if (!pagoDAO.existsById(id)) {
            throw new ResourceNotFoundException("Pago no encontrado con ID: " + id);
        }

        pagoDAO.findByReferencia(pagoDTO.getReferencia())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new BusinessValidationException(
                            "Ya existe otro pago registrado con la referencia: " + pagoDTO.getReferencia());
                });
        validarPago(pagoDTO);

        PagoDTO actualizado = pagoDAO.update(id, pagoDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado con ID: " + id));
        log.info("Pago actualizado ID: {}", id);
        return actualizado;
    }

    @Override
    public void eliminarPago(Long id) {
        if (!pagoDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Pago no encontrado con ID: " + id);
        }
        log.info("Pago eliminado ID: {}", id);
    }

    private void validarPago(PagoDTO dto) {
        if (!usuarioDAO.existsById(dto.getUsuarioId())) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + dto.getUsuarioId());
        }
        if (metodoPagoDAO.findByIdAndUsuarioId(dto.getMetodoPagoId(), dto.getUsuarioId()).isEmpty()) {
            throw new BusinessValidationException(
                    "El metodo de pago " + dto.getMetodoPagoId() + " no existe o no pertenece al usuario " + dto.getUsuarioId());
        }

        String concepto = dto.getConcepto().toUpperCase();
        switch (concepto) {
            case CONCEPTO_MULTA -> {
                if (dto.getMultaId() == null) {
                    throw new BusinessValidationException("El campo multaId es obligatorio para pagos de MULTA");
                }
                if (!multaDAO.existsById(dto.getMultaId())) {
                    throw new ResourceNotFoundException("Multa no encontrada con ID: " + dto.getMultaId());
                }
            }
            case CONCEPTO_SUSCRIPCION -> {
                if (dto.getSuscripcionId() == null) {
                    throw new BusinessValidationException("El campo suscripcionId es obligatorio para pagos de SUSCRIPCION");
                }
                if (!suscripcionDAO.existsById(dto.getSuscripcionId())) {
                    throw new ResourceNotFoundException("Suscripcion no encontrada con ID: " + dto.getSuscripcionId());
                }
            }
            case CONCEPTO_RESERVA_SALA -> {
                if (dto.getReservaSalaId() == null) {
                    throw new BusinessValidationException("El campo reservaSalaId es obligatorio para pagos de RESERVA_SALA");
                }
                if (!reservaSalaDAO.existsById(dto.getReservaSalaId())) {
                    throw new ResourceNotFoundException("Reserva de sala no encontrada con ID: " + dto.getReservaSalaId());
                }
            }
            default -> throw new BusinessValidationException(
                    "Concepto invalido: " + dto.getConcepto() + ". Valores permitidos: MULTA, SUSCRIPCION, RESERVA_SALA");
        }
        dto.setConcepto(concepto);
    }
}
