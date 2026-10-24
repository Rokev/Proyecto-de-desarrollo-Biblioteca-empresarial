package bussinesLayer.service.impl;

import bussinesLayer.dto.TrasladoRetornoDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.ReservaLibroService;
import bussinesLayer.service.TrasladoRetornoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.EjemplarDAO;
import persistanceLayer.dao.SedeDAO;
import persistanceLayer.dao.TrasladoRetornoDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TrasladoRetornoServiceImpl implements TrasladoRetornoService {

    private static final String TRASLADO_EN_TRANSITO = "EN_TRANSITO";
    private static final String TRASLADO_RECIBIDO = "RECIBIDO";

    private final TrasladoRetornoDAO trasladoDAO;
    private final UsuarioDAO usuarioDAO;
    private final SedeDAO sedeDAO;
    private final EjemplarDAO ejemplarDAO;
    private final ReservaLibroService reservaLibroService;

    @Override
    @Transactional(readOnly = true)
    public TrasladoRetornoDTO obtenerTrasladoPorId(Long id) {
        return trasladoDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Traslado no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrasladoRetornoDTO> obtenerTrasladosHaciaSede(Long sedeDestinoId, String estado) {
        if (!sedeDAO.existsById(sedeDestinoId)) {
            throw new ResourceNotFoundException("Sede no encontrada con ID: " + sedeDestinoId);
        }
        if (estado == null || estado.isBlank()) {
            return trasladoDAO.findBySedeDestinoIdOrderByFechaCreacionAsc(sedeDestinoId);
        }
        String normalizado = estado.trim().toUpperCase();
        if (!List.of(TRASLADO_EN_TRANSITO, TRASLADO_RECIBIDO).contains(normalizado)) {
            throw new BusinessValidationException(
                    "Estado invalido: " + estado + ". Valores permitidos: EN_TRANSITO, RECIBIDO");
        }
        return trasladoDAO.findBySedeDestinoIdAndEstadoIgnoreCaseOrderByFechaCreacionAsc(sedeDestinoId, normalizado);
    }

    @Override
    public TrasladoRetornoDTO recibirTraslado(Long trasladoId, Long bibliotecarioId) {
        // Por ahora solo se comprueba que el bibliotecario exista; el rol se validara con la seguridad.
        if (!usuarioDAO.existsById(bibliotecarioId)) {
            throw new ResourceNotFoundException("Bibliotecario no encontrado con ID: " + bibliotecarioId);
        }
        TrasladoRetornoDTO traslado = obtenerTrasladoPorId(trasladoId);
        if (!TRASLADO_EN_TRANSITO.equalsIgnoreCase(traslado.getEstado())) {
            throw new BusinessValidationException("El traslado no esta EN_TRANSITO (estado actual: "
                    + traslado.getEstado() + ")");
        }

        if (ejemplarDAO.cambiarEstadoSiEs(traslado.getEjemplarId(), "EN_TRANSITO", "DISPONIBLE") == 0) {
            throw new BusinessValidationException("El ejemplar del traslado no esta en transito");
        }
        traslado.setEstado(TRASLADO_RECIBIDO);
        traslado.setFechaRecepcion(LocalDateTime.now());
        traslado.setBibliotecarioId(bibliotecarioId);
        TrasladoRetornoDTO recibido = trasladoDAO.update(trasladoId, traslado)
                .orElseThrow(() -> new ResourceNotFoundException("Traslado no encontrado con ID: " + trasladoId));

        // Si alguien lo tenia reservado, se le aparta y se le avisa.
        reservaLibroService.asignarEjemplarAReservaEnEspera(traslado.getEjemplarId());
        log.info("Traslado {} recibido por el bibliotecario {}", trasladoId, bibliotecarioId);
        return recibido;
    }
}