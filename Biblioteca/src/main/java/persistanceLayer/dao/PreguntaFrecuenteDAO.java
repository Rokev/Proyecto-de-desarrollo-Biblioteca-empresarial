package persistanceLayer.dao;

import bussinesLayer.dto.PreguntaFrecuenteDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.PreguntaFrecuente;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.PreguntaFrecuenteRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de PreguntaFrecuente: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class PreguntaFrecuenteDAO {

    private final PreguntaFrecuenteRepository preguntaFrecuenteRepository;
    private final Mapper mapper;

    public PreguntaFrecuenteDTO save(PreguntaFrecuenteDTO dto) {
        PreguntaFrecuente entity = mapper.preguntaFrecuenteToEntity(dto);
        entity.setId(null);
        PreguntaFrecuente guardado = preguntaFrecuenteRepository.save(entity);
        return mapper.preguntaFrecuenteToDto(guardado);
    }

    public Optional<PreguntaFrecuenteDTO> findById(Long id) {
        return preguntaFrecuenteRepository.findById(id).map(mapper::preguntaFrecuenteToDto);
    }

    public List<PreguntaFrecuenteDTO> findAll() {
        return preguntaFrecuenteRepository.findAll().stream().map(mapper::preguntaFrecuenteToDto).toList();
    }

    public Optional<PreguntaFrecuenteDTO> update(Long id, PreguntaFrecuenteDTO dto) {
        return preguntaFrecuenteRepository.findById(id).map(existente -> {
            existente.setPregunta(dto.getPregunta());
            existente.setRespuesta(dto.getRespuesta());
            existente.setCategoria(dto.getCategoria());
            if (dto.getActiva() != null) {
                existente.setActiva(dto.getActiva());
            }
            return mapper.preguntaFrecuenteToDto(preguntaFrecuenteRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return preguntaFrecuenteRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (preguntaFrecuenteRepository.existsById(id)) {
            preguntaFrecuenteRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<PreguntaFrecuenteDTO> findByActivaTrue() {
        return preguntaFrecuenteRepository.findByActivaTrue().stream().map(mapper::preguntaFrecuenteToDto).toList();
    }

    public List<PreguntaFrecuenteDTO> findByCategoriaIgnoreCaseAndActivaTrue(String categoria) {
        return preguntaFrecuenteRepository.findByCategoriaIgnoreCaseAndActivaTrue(categoria).stream().map(mapper::preguntaFrecuenteToDto).toList();
    }

    public List<PreguntaFrecuenteDTO> findByPreguntaContainingIgnoreCaseAndActivaTrue(String texto) {
        return preguntaFrecuenteRepository.findByPreguntaContainingIgnoreCaseAndActivaTrue(texto).stream().map(mapper::preguntaFrecuenteToDto).toList();
    }
}
