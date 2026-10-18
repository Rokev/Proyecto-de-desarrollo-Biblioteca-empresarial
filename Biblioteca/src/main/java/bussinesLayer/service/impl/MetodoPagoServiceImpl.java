package bussinesLayer.service.impl;

import bussinesLayer.dto.MetodoPagoDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.MetodoPagoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.MetodoPagoDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class MetodoPagoServiceImpl implements MetodoPagoService {

    private final MetodoPagoDAO metodoPagoDAO;
    private final UsuarioDAO usuarioDAO;

    @Override
    public MetodoPagoDTO crearMetodoPago(MetodoPagoDTO dto) {
        log.info("Creando metodo de pago para el usuario: {}", dto.getUsuarioId());

        if (!usuarioDAO.existsById(dto.getUsuarioId())) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + dto.getUsuarioId());
        }

        // El primer metodo de un usuario siempre queda como predeterminado.
        boolean primero = metodoPagoDAO.countByUsuarioId(dto.getUsuarioId()) == 0;
        if (primero) {
            dto.setPredeterminado(true);
        } else if (Boolean.TRUE.equals(dto.getPredeterminado())) {
            metodoPagoDAO.quitarPredeterminado(dto.getUsuarioId());
        } else {
            dto.setPredeterminado(false);
        }

        MetodoPagoDTO guardado = metodoPagoDAO.save(dto);
        log.info("Metodo de pago creado con ID: {}", guardado.getId());
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public MetodoPagoDTO obtenerMetodoPagoPorId(Long id) {
        return metodoPagoDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Metodo de pago no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MetodoPagoDTO> obtenerTodosLosMetodosPago() {
        return metodoPagoDAO.findAll();
    }

    @Override
    public MetodoPagoDTO actualizarMetodoPago(Long id, MetodoPagoDTO dto) {
        MetodoPagoDTO actual = metodoPagoDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Metodo de pago no encontrado con ID: " + id));

        if (!actual.getUsuarioId().equals(dto.getUsuarioId())) {
            throw new BusinessValidationException("No se puede cambiar el usuario propietario del metodo de pago");
        }

        if (Boolean.TRUE.equals(dto.getPredeterminado()) && !Boolean.TRUE.equals(actual.getPredeterminado())) {
            metodoPagoDAO.quitarPredeterminado(dto.getUsuarioId());
        } else if (Boolean.FALSE.equals(dto.getPredeterminado()) && Boolean.TRUE.equals(actual.getPredeterminado())) {
            throw new BusinessValidationException(
                    "Debe existir un metodo predeterminado; marque otro como predeterminado en su lugar");
        }

        MetodoPagoDTO actualizado = metodoPagoDAO.update(id, dto)
                .orElseThrow(() -> new ResourceNotFoundException("Metodo de pago no encontrado con ID: " + id));
        log.info("Metodo de pago actualizado ID: {}", id);
        return actualizado;
    }

    @Override
    public void eliminarMetodoPago(Long id) {
        if (!metodoPagoDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Metodo de pago no encontrado con ID: " + id);
        }
        log.info("Metodo de pago eliminado ID: {}", id);
    }
}
