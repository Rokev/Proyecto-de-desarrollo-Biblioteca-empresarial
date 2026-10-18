package bussinesLayer.service.impl;

import bussinesLayer.dto.ReservaLibroDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.ReservaLibroService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.EjemplarDAO;
import persistanceLayer.dao.LibroDAO;
import persistanceLayer.dao.ReservaLibroDAO;
import persistanceLayer.dao.SedeDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ReservaLibroServiceImpl implements ReservaLibroService {

    private static final String ESTADO_EN_ESPERA = "EN_ESPERA";
    private static final String ESTADO_NOTIFICADA = "NOTIFICADA";
    private static final String ESTADO_CANCELADA = "CANCELADA";

    private final ReservaLibroDAO reservaLibroDAO;
    private final UsuarioDAO usuarioDAO;
    private final LibroDAO libroDAO;
    private final SedeDAO sedeDAO;
    private final EjemplarDAO ejemplarDAO;

    @Override
    public ReservaLibroDTO crearReservaLibro(ReservaLibroDTO dto) {
        log.info("Creando reserva del libro {} para el usuario {}", dto.getLibroId(), dto.getUsuarioId());

        validarReferencias(dto);

        if (reservaLibroDAO.existsByUsuarioIdAndLibroIdAndEstadoIn(
                dto.getUsuarioId(), dto.getLibroId(), List.of(ESTADO_EN_ESPERA, ESTADO_NOTIFICADA))) {
            throw new BusinessValidationException("El usuario ya tiene una reserva vigente para este libro");
        }
        if (dto.getEstado() == null || dto.getEstado().isBlank()) {
            dto.setEstado(ESTADO_EN_ESPERA);
        }

        ReservaLibroDTO guardada = reservaLibroDAO.save(dto);
        log.info("Reserva de libro creada con ID: {}", guardada.getId());
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaLibroDTO obtenerReservaLibroPorId(Long id) {
        return reservaLibroDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva de libro no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaLibroDTO> obtenerTodasLasReservasLibro() {
        return reservaLibroDAO.findAll();
    }

    @Override
    public ReservaLibroDTO actualizarReservaLibro(Long id, ReservaLibroDTO dto) {
        ReservaLibroDTO actual = reservaLibroDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva de libro no encontrada con ID: " + id));

        if (!actual.getUsuarioId().equals(dto.getUsuarioId()) || !actual.getLibroId().equals(dto.getLibroId())) {
            throw new BusinessValidationException("No se puede cambiar el usuario ni el libro de una reserva");
        }
        validarReferencias(dto);

        if (dto.getEstado() != null && ESTADO_CANCELADA.equalsIgnoreCase(dto.getEstado())
                && !ESTADO_CANCELADA.equalsIgnoreCase(actual.getEstado())
                && dto.getFechaCancelacion() == null) {
            dto.setFechaCancelacion(LocalDateTime.now());
        }

        ReservaLibroDTO actualizada = reservaLibroDAO.update(id, dto)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva de libro no encontrada con ID: " + id));
        log.info("Reserva de libro actualizada ID: {}", id);
        return actualizada;
    }

    @Override
    public void eliminarReservaLibro(Long id) {
        if (!reservaLibroDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Reserva de libro no encontrada con ID: " + id);
        }
        log.info("Reserva de libro eliminada ID: {}", id);
    }

    private void validarReferencias(ReservaLibroDTO dto) {
        if (!usuarioDAO.existsById(dto.getUsuarioId())) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + dto.getUsuarioId());
        }
        if (!libroDAO.existsById(dto.getLibroId())) {
            throw new ResourceNotFoundException("Libro no encontrado con ID: " + dto.getLibroId());
        }
        if (!sedeDAO.existsById(dto.getSedeId())) {
            throw new ResourceNotFoundException("Sede no encontrada con ID: " + dto.getSedeId());
        }
        if (dto.getEjemplarId() != null && !ejemplarDAO.existsById(dto.getEjemplarId())) {
            throw new ResourceNotFoundException("Ejemplar no encontrado con ID: " + dto.getEjemplarId());
        }
    }
}
