package bussinesLayer.service.impl;

import bussinesLayer.dto.ReservaSalaDTO;
import bussinesLayer.dto.SalaEstudioDTO;
import bussinesLayer.dto.SuscripcionDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.ReservaSalaService;
import bussinesLayer.service.SuscripcionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.ReservaSalaDAO;
import persistanceLayer.dao.SalaEstudioDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
class ReservaSalaServiceImpl implements ReservaSalaService {

    private static final String ESTADO_INICIAL = "PENDIENTE_PAGO";
    private static final String ESTADO_CANCELADA = "CANCELADA";
    private static final BigDecimal MINUTOS_POR_HORA = BigDecimal.valueOf(60);

    private final ReservaSalaDAO reservaSalaDAO;
    private final SalaEstudioDAO salaEstudioDAO;
    private final UsuarioDAO usuarioDAO;
    private final SuscripcionService suscripcionService;

    @Override
    public ReservaSalaDTO crearReservaSala(ReservaSalaDTO dto) {
        log.info("Creando reserva de la sala {} para el usuario {}", dto.getSalaId(), dto.getUsuarioId());

        validarReferenciasYFechas(dto);

        if (dto.getFechaInicio().isBefore(LocalDateTime.now())) {
            throw new BusinessValidationException("La fechaInicio no puede estar en el pasado");
        }
        SalaEstudioDTO sala = salaEstudioDAO.findById(dto.getSalaId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sala de estudio no encontrada con ID: " + dto.getSalaId()));
        if (!Boolean.TRUE.equals(sala.getActiva())) {
            throw new BusinessValidationException("La sala de estudio no esta activa");
        }
        if (reservaSalaDAO.existeSolape(dto.getSalaId(), dto.getFechaInicio(), dto.getFechaFin())) {
            throw new BusinessValidationException("La sala ya esta reservada en el horario solicitado");
        }

        calcularValores(dto, sala);
        if (dto.getEstado() == null || dto.getEstado().isBlank()) {
            dto.setEstado(ESTADO_INICIAL);
        }
        dto.setFechaCreacion(LocalDateTime.now());

        ReservaSalaDTO guardada = reservaSalaDAO.save(dto);
        log.info("Reserva de sala creada con ID: {} (base {}, descuento {}, total {})",
                guardada.getId(), guardada.getValorBase(), guardada.getDescuento(), guardada.getValorTotal());
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

        if (!actual.getUsuarioId().equals(dto.getUsuarioId())) {
            throw new BusinessValidationException("No se puede cambiar el usuario de una reserva");
        }
        boolean cambioHorario = !actual.getSalaId().equals(dto.getSalaId())
                || !actual.getFechaInicio().equals(dto.getFechaInicio())
                || !actual.getFechaFin().equals(dto.getFechaFin());
        if (cambioHorario) {
            throw new BusinessValidationException(
                    "No se puede cambiar la sala ni el horario de una reserva; cancele y cree una nueva");
        }

        // Los valores quedan fijados al crear la reserva: el cliente no los modifica.
        dto.setValorBase(actual.getValorBase());
        dto.setDescuento(actual.getDescuento());
        dto.setValorTotal(actual.getValorTotal());
        dto.setFechaCreacion(actual.getFechaCreacion());

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

    /**
     * el servidor calcula siempre los valores.
     * valorBase = tarifaPorHora x duracion; descuento = valorBase x porcentaje de la suscripcion vigente
     * (0 si el usuario no tiene); valorTotal = valorBase - descuento.
     */
    private void calcularValores(ReservaSalaDTO dto, SalaEstudioDTO sala) {
        long minutos = Duration.between(dto.getFechaInicio(), dto.getFechaFin()).toMinutes();
        BigDecimal valorBase = sala.getTarifaPorHora()
                .multiply(BigDecimal.valueOf(minutos))
                .divide(MINUTOS_POR_HORA, 2, RoundingMode.HALF_UP);

        BigDecimal porcentaje = suscripcionService.buscarSuscripcionVigente(dto.getUsuarioId())
                .map(SuscripcionDTO::getDescuentoSalas)
                .orElse(BigDecimal.ZERO);
        BigDecimal descuento = valorBase.multiply(porcentaje).setScale(2, RoundingMode.HALF_UP);

        dto.setValorBase(valorBase);
        dto.setDescuento(descuento);
        dto.setValorTotal(valorBase.subtract(descuento));
    }
}