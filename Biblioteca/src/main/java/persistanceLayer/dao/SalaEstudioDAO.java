package persistanceLayer.dao;

import bussinesLayer.dto.SalaEstudioDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.SalaEstudioEntity;
import persistanceLayer.entity.SedeEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.SalaEstudioRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de SalaEstudio: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class SalaEstudioDAO {

    private final SalaEstudioRepository salaEstudioRepository;
    private final Mapper mapper;

    public SalaEstudioDTO save(SalaEstudioDTO dto) {
        SalaEstudioEntity entity = mapper.salaEstudioToEntity(dto);
        entity.setId(null);
        entity.setSede(referenciaSede(dto.getSedeId()));
        SalaEstudioEntity guardado = salaEstudioRepository.save(entity);
        return mapper.salaEstudioToDto(guardado);
    }

    public Optional<SalaEstudioDTO> findById(Long id) {
        return salaEstudioRepository.findById(id).map(mapper::salaEstudioToDto);
    }

    public List<SalaEstudioDTO> findAll() {
        return salaEstudioRepository.findAll().stream().map(mapper::salaEstudioToDto).toList();
    }

    public Optional<SalaEstudioDTO> update(Long id, SalaEstudioDTO dto) {
        return salaEstudioRepository.findById(id).map(existente -> {
            existente.setNombre(dto.getNombre());
            existente.setCapacidad(dto.getCapacidad());
            existente.setDescripcion(dto.getDescripcion());
            existente.setTarifaPorHora(dto.getTarifaPorHora());
            if (dto.getActiva() != null) {
                existente.setActiva(dto.getActiva());
            }
            existente.setSede(referenciaSede(dto.getSedeId()));
            return mapper.salaEstudioToDto(salaEstudioRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return salaEstudioRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (salaEstudioRepository.existsById(id)) {
            salaEstudioRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<SalaEstudioDTO> findBySedeId(Long sedeId) {
        return salaEstudioRepository.findBySedeId(sedeId).stream().map(mapper::salaEstudioToDto).toList();
    }

    public List<SalaEstudioDTO> findBySedeIdAndActivaTrue(Long sedeId) {
        return salaEstudioRepository.findBySedeIdAndActivaTrue(sedeId).stream().map(mapper::salaEstudioToDto).toList();
    }

    public List<SalaEstudioDTO> findByActivaTrue() {
        return salaEstudioRepository.findByActivaTrue().stream().map(mapper::salaEstudioToDto).toList();
    }

    public List<SalaEstudioDTO> findByCapacidadGreaterThanEqualAndActivaTrue(Integer capacidad) {
        return salaEstudioRepository.findByCapacidadGreaterThanEqualAndActivaTrue(capacidad).stream().map(mapper::salaEstudioToDto).toList();
    }

    public boolean existsBySedeIdAndNombreIgnoreCase(Long sedeId, String nombre) {
        return salaEstudioRepository.existsBySedeIdAndNombreIgnoreCase(sedeId, nombre);
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
