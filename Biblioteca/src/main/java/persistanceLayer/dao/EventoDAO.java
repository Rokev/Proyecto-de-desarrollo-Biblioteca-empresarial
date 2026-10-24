package persistanceLayer.dao;

import bussinesLayer.dto.EventoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.entity.EventoEntity;
import persistanceLayer.entity.SedeEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.EventoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de Evento: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class EventoDAO {

    private final EventoRepository eventoRepository;
    private final Mapper mapper;

    public EventoDTO save(EventoDTO dto) {
        EventoEntity entity = mapper.eventoToEntity(dto);
        entity.setId(null);
        entity.setSede(referenciaSede(dto.getSedeId()));
        EventoEntity guardado = eventoRepository.save(entity);
        return mapper.eventoToDto(guardado);
    }

    public Optional<EventoDTO> findById(Long id) {
        return eventoRepository.findById(id).map(mapper::eventoToDto);
    }

    public List<EventoDTO> findAll() {
        return eventoRepository.findAll().stream().map(mapper::eventoToDto).toList();
    }

    public Optional<EventoDTO> update(Long id, EventoDTO dto) {
        return eventoRepository.findById(id).map(existente -> {
            existente.setTitulo(dto.getTitulo());
            existente.setDescripcion(dto.getDescripcion());
            existente.setFechaInicio(dto.getFechaInicio());
            existente.setFechaFin(dto.getFechaFin());
            existente.setCupoMaximo(dto.getCupoMaximo());
            existente.setCuposDisponibles(dto.getCuposDisponibles());
            if (dto.getEstado() != null) {
                existente.setEstado(dto.getEstado());
            }
            if (dto.getExclusivo() != null) {
                existente.setExclusivo(dto.getExclusivo());
            }
            existente.setSede(referenciaSede(dto.getSedeId()));
            return mapper.eventoToDto(eventoRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return eventoRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (eventoRepository.existsById(id)) {
            eventoRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<EventoDTO> findBySedeIdOrderByFechaInicioAsc(Long sedeId) {
        return eventoRepository.findBySedeIdOrderByFechaInicioAsc(sedeId).stream().map(mapper::eventoToDto).toList();
    }

    public List<EventoDTO> findByEstadoIgnoreCase(String estado) {
        return eventoRepository.findByEstadoIgnoreCase(estado).stream().map(mapper::eventoToDto).toList();
    }

    public List<EventoDTO> findByFechaInicioGreaterThanEqualAndEstadoIgnoreCaseOrderByFechaInicioAsc(LocalDateTime desde, String estado) {
        return eventoRepository.findByFechaInicioGreaterThanEqualAndEstadoIgnoreCaseOrderByFechaInicioAsc(desde, estado).stream().map(mapper::eventoToDto).toList();
    }

    @Transactional
    public int descontarCupos(Long id, int cantidad) {
        return eventoRepository.descontarCupos(id, cantidad);
    }

    @Transactional
    public int liberarCupos(Long id, int cantidad) {
        return eventoRepository.liberarCupos(id, cantidad);
    }

    private SedeEntity referenciaSede(Long idSede) {
        if (idSede == null) {
            return null;
        }
        SedeEntity referencia = new SedeEntity();
        referencia.setId(idSede);
        return referencia;
    }
}
