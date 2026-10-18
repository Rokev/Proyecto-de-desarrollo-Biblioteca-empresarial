package persistanceLayer.dao;

import bussinesLayer.dto.EjemplarDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.EjemplarEntity;
import persistanceLayer.entity.LibroEntity;
import persistanceLayer.entity.SedeEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.EjemplarRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de Ejemplar: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class EjemplarDAO {

    private final EjemplarRepository ejemplarRepository;
    private final Mapper mapper;

    public EjemplarDTO save(EjemplarDTO dto) {
        EjemplarEntity entity = mapper.ejemplarToEntity(dto);
        entity.setId(null);
        entity.setLibro(referenciaLibro(dto.getLibroId()));
        entity.setSede(referenciaSede(dto.getSedeId()));
        EjemplarEntity guardado = ejemplarRepository.save(entity);
        return mapper.ejemplarToDto(guardado);
    }

    public Optional<EjemplarDTO> findById(Long id) {
        return ejemplarRepository.findById(id).map(mapper::ejemplarToDto);
    }

    public List<EjemplarDTO> findAll() {
        return ejemplarRepository.findAll().stream().map(mapper::ejemplarToDto).toList();
    }

    public Optional<EjemplarDTO> update(Long id, EjemplarDTO dto) {
        return ejemplarRepository.findById(id).map(existente -> {
            existente.setCodigoBarras(dto.getCodigoBarras());
            if (dto.getEstado() != null) {
                existente.setEstado(dto.getEstado());
            }
            existente.setLibro(referenciaLibro(dto.getLibroId()));
            existente.setSede(referenciaSede(dto.getSedeId()));
            return mapper.ejemplarToDto(ejemplarRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return ejemplarRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (ejemplarRepository.existsById(id)) {
            ejemplarRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<EjemplarDTO> findByCodigoBarras(String codigoBarras) {
        return ejemplarRepository.findByCodigoBarras(codigoBarras).map(mapper::ejemplarToDto);
    }

    public boolean existsByCodigoBarras(String codigoBarras) {
        return ejemplarRepository.existsByCodigoBarras(codigoBarras);
    }

    public List<EjemplarDTO> findByLibroId(Long libroId) {
        return ejemplarRepository.findByLibroId(libroId).stream().map(mapper::ejemplarToDto).toList();
    }

    public List<EjemplarDTO> findBySedeId(Long sedeId) {
        return ejemplarRepository.findBySedeId(sedeId).stream().map(mapper::ejemplarToDto).toList();
    }

    public List<EjemplarDTO> findByLibroIdAndSedeId(Long libroId, Long sedeId) {
        return ejemplarRepository.findByLibroIdAndSedeId(libroId, sedeId).stream().map(mapper::ejemplarToDto).toList();
    }

    public List<EjemplarDTO> findByLibroIdAndEstadoIgnoreCase(Long libroId, String estado) {
        return ejemplarRepository.findByLibroIdAndEstadoIgnoreCase(libroId, estado).stream().map(mapper::ejemplarToDto).toList();
    }

    public Optional<EjemplarDTO> findFirstByLibroIdAndSedeIdAndEstadoIgnoreCase(Long libroId, Long sedeId, String estado) {
        return ejemplarRepository.findFirstByLibroIdAndSedeIdAndEstadoIgnoreCase(libroId, sedeId, estado).map(mapper::ejemplarToDto);
    }

    public long countByLibroIdAndSedeIdAndEstadoIgnoreCase(Long libroId, Long sedeId, String estado) {
        return ejemplarRepository.countByLibroIdAndSedeIdAndEstadoIgnoreCase(libroId, sedeId, estado);
    }

    public boolean existsBySedeId(Long sedeId) {
        return ejemplarRepository.existsBySedeId(sedeId);
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
}
