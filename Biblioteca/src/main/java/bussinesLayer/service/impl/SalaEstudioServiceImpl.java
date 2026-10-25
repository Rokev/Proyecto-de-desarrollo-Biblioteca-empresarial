package bussinesLayer.service.impl;
import bussinesLayer.dto.DisponibilidadSalasDTO;

import bussinesLayer.dto.SalaEstudioDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.SalaEstudioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.ReservaSalaDAO;
import persistanceLayer.dao.SalaEstudioDAO;
import persistanceLayer.dao.SedeDAO;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SalaEstudioServiceImpl implements SalaEstudioService {

    private final SalaEstudioDAO salaEstudioDAO;
    private final SedeDAO sedeDAO;
    private final ReservaSalaDAO reservaSalaDAO;

    @Override
    public SalaEstudioDTO crearSalaEstudio(SalaEstudioDTO dto) {
        log.info("Creando sala de estudio: {}", dto.getNombre());

        validarSede(dto);
        if (salaEstudioDAO.existsBySedeIdAndNombreIgnoreCase(dto.getSedeId(), dto.getNombre())) {
            throw new BusinessValidationException(
                    "Ya existe una sala con el nombre '" + dto.getNombre() + "' en la sede " + dto.getSedeId());
        }
        if (dto.getActiva() == null) {
            dto.setActiva(true);
        }

        SalaEstudioDTO guardada = salaEstudioDAO.save(dto);
        log.info("Sala de estudio creada con ID: {}", guardada.getId());
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public SalaEstudioDTO obtenerSalaEstudioPorId(Long id) {
        return salaEstudioDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sala de estudio no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalaEstudioDTO> obtenerTodasLasSalasEstudio() {
        return salaEstudioDAO.findAll();
    }

    @Override
    public SalaEstudioDTO actualizarSalaEstudio(Long id, SalaEstudioDTO dto) {
        SalaEstudioDTO actual = salaEstudioDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sala de estudio no encontrada con ID: " + id));

        validarSede(dto);
        boolean cambioClave = !actual.getSedeId().equals(dto.getSedeId())
                || !actual.getNombre().equalsIgnoreCase(dto.getNombre());
        if (cambioClave && salaEstudioDAO.existsBySedeIdAndNombreIgnoreCase(dto.getSedeId(), dto.getNombre())) {
            throw new BusinessValidationException(
                    "Ya existe una sala con el nombre '" + dto.getNombre() + "' en la sede " + dto.getSedeId());
        }

        SalaEstudioDTO actualizada = salaEstudioDAO.update(id, dto)
                .orElseThrow(() -> new ResourceNotFoundException("Sala de estudio no encontrada con ID: " + id));
        log.info("Sala de estudio actualizada ID: {}", id);
        return actualizada;
    }

    @Override
    public void eliminarSalaEstudio(Long id) {
        if (!salaEstudioDAO.existsById(id)) {
            throw new ResourceNotFoundException("Sala de estudio no encontrada con ID: " + id);
        }
        //  50: solo se elimina si no tiene reservas activas (pendientes de pago o confirmadas que no han terminado).
        if (reservaSalaDAO.existsBySalaIdAndEstadoInAndFechaFinAfter(
                id, List.of("PENDIENTE_PAGO", "CONFIRMADA"), LocalDateTime.now())) {
            throw new BusinessValidationException("No se puede eliminar la sala porque tiene reservas activas");
        }
        if (!reservaSalaDAO.findBySalaId(id).isEmpty()) {
            throw new BusinessValidationException(
                    "No se puede eliminar la sala porque tiene historial de reservas; desactivela (activa=false) en su lugar");
        }
        if (!salaEstudioDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Sala de estudio no encontrada con ID: " + id);
        }
        log.info("Sala de estudio eliminada ID: {}", id);
    }

    private void validarSede(SalaEstudioDTO dto) {
        if (!sedeDAO.existsById(dto.getSedeId())) {
            throw new ResourceNotFoundException("Sede no encontrada con ID: " + dto.getSedeId());
        }
        if (dto.getCapacidad() < 1) {
            throw new BusinessValidationException("La capacidad debe ser al menos 1");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DisponibilidadSalasDTO consultarDisponibilidadDeSede(Long sedeId, LocalDateTime inicio, LocalDateTime fin) {
        if (!sedeDAO.existsById(sedeId)) {
            throw new ResourceNotFoundException("Sede no encontrada con ID: " + sedeId);
        }
        LocalDateTime desde = inicio != null ? inicio : LocalDateTime.now();
        LocalDateTime hasta = fin != null ? fin : desde.plusHours(1);
        if (!hasta.isAfter(desde)) {
            throw new BusinessValidationException("El fin debe ser posterior al inicio");
        }
        List<DisponibilidadSalasDTO.SalaDisponibilidad> salas = salaEstudioDAO.findBySedeIdAndActivaTrue(sedeId).stream()
                .map(s -> new DisponibilidadSalasDTO.SalaDisponibilidad(s.getId(), s.getNombre(), s.getCapacidad(),
                        s.getTarifaPorHora(), !reservaSalaDAO.existeSolape(s.getId(), desde, hasta)))
                .toList();
        int libres = (int) salas.stream().filter(DisponibilidadSalasDTO.SalaDisponibilidad::isDisponible).count();
        return new DisponibilidadSalasDTO(sedeId, !salas.isEmpty(), salas.size(), libres, desde, hasta, salas);
    }
}
