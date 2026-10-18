package bussinesLayer.service.impl;

import bussinesLayer.dto.ReservaSalaDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.ReservaSalaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.ReservaSalaDAO;
import persistanceLayer.dao.SalaEstudioDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ReservaSalaServiceImpl implements ReservaSalaService {

    private static final String ESTADO_INICIAL = "PENDIENTE_PAGO";
    private static final String ESTADO_CANCELADA = "CANCELADA";

    private final ReservaSalaDAO reservaSalaDAO;
    private final SalaEstudioDAO salaEstudioDAO;
    private final UsuarioDAO usuarioDAO;

    @Override
    public ReservaSalaDTO crearReservaSala(ReservaSalaDTO dto) {
        log.info("Creando reserva de la sala {} para el usuario {}", dto.getSalaId(), dto.getUsuarioId());

        validarReferenciasYFechas(dto);

        if (dto.getFechaInicio().isBefore(LocalDateTime.now())) {
            throw new BusinessValidationException("La fechaInicio no puede estar en el pasado");
        }
        var sala = salaEstudioDAO.findById(dto.getSalaId()).orElseThrow();
        if (!Boolean.TRUE.equals(sala.getActiva())) {
            throw new BusinessValidationException("La sala de estudio no esta activa");
        }
        if (reservaSalaDAO.existeSolape(dto.getSalaId(), dto.getFechaInicio(), dto.getFechaFin())) {
            throw new BusinessValidationException("La sala ya esta reservada en el horario solicitado");
        }

        calcularValores(dto);
        if (dto.getEstado() == null || dto.getEstado().isBlank()) {
            dto.setEstado(ESTADO_INICIAL);
        }
        if (dto.getFechaCreacion() == null) {
            dto.setFechaCreacion(LocalDateTime.now());
        }

        ReservaSalaDTO guardada = reservaSalaDAO.save(dto);
        log.info("Reserva de sala creada con ID: {}", guardada.getId());
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaSalaDTO obtenerReservaSalaPorId(Long id) {
        return reservaSalaDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva de sala no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaSalaDTO> obtenerTodasLasReservasSala() {
        return reservaSalaDAO.findAll();
    }

    @Override
    public ReservaSalaDTO actualizarReservaSala(Long id, ReservaSalaDTO dto) {
        ReservaSalaDTO actual = reservaSalaDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva de sala no encontrada con ID: " + id));

        validarReferenciasYFechas(dto);

        boolean cambioHorario = !actual.getSalaId().equals(dto.getSalaId())
                || !actual.getFechaInicio().equals(dto.getFechaInicio())
                || !actual.getFechaFin().equals(dto.getFechaFin());
        if (cambioHorario) {
            throw new BusinessValidationException(
                    "No se puede cambiar la sala ni el horario de una reserva; cancele y cree una nueva");
        }

        calcularValores(dto);
        if (dto.getEstado() != null && ESTADO_CANCELADA.equalsIgnoreCase(dto.getEstado())
                && !ESTADO_CANCELADA.equalsIgnoreCase(actual.getEstado())
                && dto.getFechaCancelacion() == null) {
            dto.setFechaCancelacion(LocalDateTime.now());
        }

        ReservaSalaDTO actualizada = reservaSalaDAO.update(id, dto)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva de sala no encontrada con ID: " + id));
        log.info("Reserva de sala actualizada ID: {}", id);
        return actualizada;
    }

    @Override
    public void eliminarReservaSala(Long id) {
        if (!reservaSalaDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Reserva de sala no encontrada con ID: " + id);
        }
        log.info("Reserva de sala eliminada ID: {}", id);
    }

    private void validarReferenciasYFechas(ReservaSalaDTO dto) {
        if (!usuarioDAO.existsById(dto.getUsuarioId())) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + dto.getUsuarioId());
        }
        if (!salaEstudioDAO.existsById(dto.getSalaId())) {
            throw new ResourceNotFoundException("Sala de estudio no encontrada con ID: " + dto.getSalaId());
        }
        if (!dto.getFechaFin().isAfter(dto.getFechaInicio())) {
            throw new BusinessValidationException("La fechaFin debe ser posterior a la fechaInicio");
        }
    }

    /** El valor total siempre se calcula en el servidor: valorBase - descuento. */
    private void calcularValores(ReservaSalaDTO dto) {
        if (dto.getDescuento() == null) {
            dto.setDescuento(BigDecimal.ZERO);
        }
        if (dto.getDescuento().compareTo(dto.getValorBase()) > 0) {
            throw new BusinessValidationException("El descuento no puede superar el valorBase");
        }
        dto.setValorTotal(dto.getValorBase().subtract(dto.getDescuento()));
    }
}
