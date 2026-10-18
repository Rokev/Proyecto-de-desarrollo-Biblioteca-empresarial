package persistanceLayer.dao;

import bussinesLayer.dto.GeneroDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.GeneroEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.GeneroRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de Genero: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class GeneroDAO {

    private final GeneroRepository generoRepository;
    private final Mapper mapper;

    public GeneroDTO save(GeneroDTO dto) {
        GeneroEntity entity = mapper.generoToEntity(dto);
        entity.setId(null);
        GeneroEntity guardado = generoRepository.save(entity);
        return mapper.generoToDto(guardado);
    }

    public Optional<GeneroDTO> findById(Long id) {
        return generoRepository.findById(id).map(mapper::generoToDto);
    }

    public List<GeneroDTO> findAll() {
        return generoRepository.findAll().stream().map(mapper::generoToDto).toList();
    }

    public Optional<GeneroDTO> update(Long id, GeneroDTO dto) {
        return generoRepository.findById(id).map(existente -> {
            existente.setNombre(dto.getNombre());
            existente.setDescripcion(dto.getDescripcion());
            return mapper.generoToDto(generoRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return generoRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (generoRepository.existsById(id)) {
            generoRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<GeneroDTO> findByNombreIgnoreCase(String nombre) {
        return generoRepository.findByNombreIgnoreCase(nombre).map(mapper::generoToDto);
    }

    public boolean existsByNombreIgnoreCase(String nombre) {
        return generoRepository.existsByNombreIgnoreCase(nombre);
    }
}
