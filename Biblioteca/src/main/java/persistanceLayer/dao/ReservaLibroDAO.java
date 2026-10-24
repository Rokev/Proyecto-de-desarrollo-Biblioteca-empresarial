package persistanceLayer.dao;

import bussinesLayer.dto.ReservaLibroDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.entity.EjemplarEntity;
import persistanceLayer.entity.LibroEntity;
import persistanceLayer.entity.ReservaLibroEntity;
import persistanceLayer.entity.SedeEntity;
import persistanceLayer.entity.UsuarioEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.ReservaLibroRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de ReservaLibro: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class ReservaLibroDAO {

    private final ReservaLibroRepository reservaLibroRepository;
    private final Mapper mapper;

    public ReservaLibroDTO save(ReservaLibroDTO dto) {
        ReservaLibroEntity entity = mapper.reservaLibroToEntity(dto);
        entity.setId(null);
        entity.setUsuario(referenciaUsuario(dto.getUsuarioId()));
        entity.setLibro(referenciaLibro(dto.getLibroId()));
        entity.setSede(referenciaSede(dto.getSedeId()));
        entity.setEjemplar(referenciaEjemplar(dto.getEjemplarId()));
        ReservaLibroEntity guardado = reservaLibroRepository.save(entity);
        return mapper.reservaLibroToDto(guardado);
    }

    public Optional<ReservaLibroDTO> findById(Long id) {
        return reservaLibroRepository.findById(id).map(mapper::reservaLibroToDto);
    }

    public List<ReservaLibroDTO> findAll() {
        return reservaLibroRepository.findAll().stream().map(mapper::reservaLibroToDto).toList();
    }

    public Optional<ReservaLibroDTO> update(Long id, ReservaLibroDTO dto) {
        return reservaLibroRepository.findById(id).map(existente -> {
            existente.setFechaReserva(dto.getFechaReserva());
            existente.setFechaNotificacion(dto.getFechaNotificacion());
            existente.setFechaLimiteRetiro(dto.getFechaLimiteRetiro());
            existente.setFechaCancelacion(dto.getFechaCancelacion());
            if (dto.getEstado() != null) {
                existente.setEstado(dto.getEstado());
            }
            existente.setUsuario(referenciaUsuario(dto.getUsuarioId()));
            existente.setLibro(referenciaLibro(dto.getLibroId()));
            existente.setSede(referenciaSede(dto.getSedeId()));
            existente.setEjemplar(referenciaEjemplar(dto.getEjemplarId()));
            return mapper.reservaLibroToDto(reservaLibroRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return reservaLibroRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (reservaLibroRepository.existsById(id)) {
            reservaLibroRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<ReservaLibroDTO> findByUsuarioIdOrderByFechaReservaDesc(Long usuarioId) {
        return reservaLibroRepository.findByUsuarioIdOrderByFechaReservaDesc(usuarioId).stream().map(mapper::reservaLibroToDto).toList();
    }

    public List<ReservaLibroDTO> findByUsuarioIdAndEstadoIgnoreCase(Long usuarioId, String estado) {
        return reservaLibroRepository.findByUsuarioIdAndEstadoIgnoreCase(usuarioId, estado).stream().map(mapper::reservaLibroToDto).toList();
    }

    public List<ReservaLibroDTO> findByLibroIdAndSedeIdAndEstadoIgnoreCaseOrderByFechaReservaAsc(Long libroId, Long sedeId, String estado) {
        return reservaLibroRepository.findByLibroIdAndSedeIdAndEstadoIgnoreCaseOrderByFechaReservaAsc(libroId, sedeId, estado).stream().map(mapper::reservaLibroToDto).toList();
    }

    public Optional<ReservaLibroDTO> findFirstByLibroIdAndSedeIdAndEstadoIgnoreCaseOrderByFechaReservaAsc(Long libroId, Long sedeId, String estado) {
        return reservaLibroRepository.findFirstByLibroIdAndSedeIdAndEstadoIgnoreCaseOrderByFechaReservaAsc(libroId, sedeId, estado).map(mapper::reservaLibroToDto);
    }

    public boolean existsByUsuarioIdAndLibroIdAndEstadoIn(Long usuarioId, Long libroId, Collection<String> estados) {
        return reservaLibroRepository.existsByUsuarioIdAndLibroIdAndEstadoIn(usuarioId, libroId, estados);
    }

    private UsuarioEntity referenciaUsuario(Long idUsuario) {
        if (idUsuario == null) {
            return null;
        }
        UsuarioEntity referencia = new UsuarioEntity();
        referencia.setId(idUsuario);
        return referencia;
    }

    private LibroEntity referenciaLibro(Long idLibro) {
        if (idLibro == null) {
            return null;
        }
        LibroEntity referencia = new LibroEntity();
        referencia.setId(idLibro);
        return referencia;
    }

    private SedeEntity referenciaSede(Long idSede) {
        if (idSede == null) {
            return null;
        }
        SedeEntity referencia = new SedeEntity();
        referencia.setId(idSede);
        return referencia;
    }

    private EjemplarEntity referenciaEjemplar(Long idEjemplar) {
        if (idEjemplar == null) {
            return null;
        }
        EjemplarEntity referencia = new EjemplarEntity();
        referencia.setId(idEjemplar);
        return referencia;
    }
}
