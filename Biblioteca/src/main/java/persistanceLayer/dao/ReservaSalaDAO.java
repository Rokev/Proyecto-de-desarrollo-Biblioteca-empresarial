package persistanceLayer.dao;

import bussinesLayer.dto.ReservaSalaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.ReservaSalaEntity;
import persistanceLayer.entity.SalaEstudioEntity;
import persistanceLayer.entity.UsuarioEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.ReservaSalaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de ReservaSala: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class ReservaSalaDAO {

    private final ReservaSalaRepository reservaSalaRepository;
    private final Mapper mapper;

    public ReservaSalaDTO save(ReservaSalaDTO dto) {
        ReservaSalaEntity entity = mapper.reservaSalaToEntity(dto);
        entity.setId(null);
        entity.setUsuario(referenciaUsuario(dto.getUsuarioId()));
        entity.setSala(referenciaSala(dto.getSalaId()));
        ReservaSalaEntity guardado = reservaSalaRepository.save(entity);
        return mapper.reservaSalaToDto(guardado);
    }

    public Optional<ReservaSalaDTO> findById(Long id) {
        return reservaSalaRepository.findById(id).map(mapper::reservaSalaToDto);
    }

    public List<ReservaSalaDTO> findAll() {
        return reservaSalaRepository.findAll().stream().map(mapper::reservaSalaToDto).toList();
    }

    public Optional<ReservaSalaDTO> update(Long id, ReservaSalaDTO dto) {
        return reservaSalaRepository.findById(id).map(existente -> {
            existente.setFechaInicio(dto.getFechaInicio());
            existente.setFechaFin(dto.getFechaFin());
            if (dto.getEstado() != null) {
                existente.setEstado(dto.getEstado());
            }
            existente.setValorBase(dto.getValorBase());
            if (dto.getDescuento() != null) {
                existente.setDescuento(dto.getDescuento());
            }
            existente.setValorTotal(dto.getValorTotal());
            existente.setFechaCreacion(dto.getFechaCreacion());
            existente.setFechaCancelacion(dto.getFechaCancelacion());
            existente.setUsuario(referenciaUsuario(dto.getUsuarioId()));
            existente.setSala(referenciaSala(dto.getSalaId()));
            return mapper.reservaSalaToDto(reservaSalaRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return reservaSalaRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (reservaSalaRepository.existsById(id)) {
            reservaSalaRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<ReservaSalaDTO> findByUsuarioIdOrderByFechaInicioDesc(Long usuarioId) {
        return reservaSalaRepository.findByUsuarioIdOrderByFechaInicioDesc(usuarioId).stream().map(mapper::reservaSalaToDto).toList();
    }

    public List<ReservaSalaDTO> findBySalaId(Long salaId) {
        return reservaSalaRepository.findBySalaId(salaId).stream().map(mapper::reservaSalaToDto).toList();
    }

    public List<ReservaSalaDTO> findByEstadoIgnoreCase(String estado) {
        return reservaSalaRepository.findByEstadoIgnoreCase(estado).stream().map(mapper::reservaSalaToDto).toList();
    }

    public boolean existsBySalaIdAndEstadoInAndFechaFinAfter(Long salaId, Collection<String> estados, LocalDateTime ahora) {
        return reservaSalaRepository.existsBySalaIdAndEstadoInAndFechaFinAfter(salaId, estados, ahora);
    }

    /** true si la sala ya tiene una reserva vigente que se cruza con el rango pedido. */
    public boolean existeSolape(Long salaId, LocalDateTime inicio, LocalDateTime fin) {
        return reservaSalaRepository.contarSolapadas(salaId, inicio, fin) > 0;
    }

    private UsuarioEntity referenciaUsuario(Long idUsuario) {
        if (idUsuario == null) {
            return null;
        }
        UsuarioEntity referencia = new UsuarioEntity();
        referencia.setId(idUsuario);
        return referencia;
    }

    private SalaEstudioEntity referenciaSala(Long idSala) {
        if (idSala == null) {
            return null;
        }
        SalaEstudioEntity referencia = new SalaEstudioEntity();
        referencia.setId(idSala);
        return referencia;
    }
}
