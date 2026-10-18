package persistanceLayer.dao;

import bussinesLayer.dto.AutorDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.AutorEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.AutorRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de Autor: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class AutorDAO {

    private final AutorRepository autorRepository;
    private final Mapper mapper;

    public AutorDTO save(AutorDTO dto) {
        AutorEntity entity = mapper.autorToEntity(dto);
        entity.setId(null);
        AutorEntity guardado = autorRepository.save(entity);
        return mapper.autorToDto(guardado);
    }

    public Optional<AutorDTO> findById(Long id) {
        return autorRepository.findById(id).map(mapper::autorToDto);
    }

    public List<AutorDTO> findAll() {
        return autorRepository.findAll().stream().map(mapper::autorToDto).toList();
    }

    public Optional<AutorDTO> update(Long id, AutorDTO dto) {
        return autorRepository.findById(id).map(existente -> {
            existente.setNombre(dto.getNombre());
            existente.setNacionalidad(dto.getNacionalidad());
            existente.setBiografia(dto.getBiografia());
            return mapper.autorToDto(autorRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return autorRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (autorRepository.existsById(id)) {
            autorRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<AutorDTO> findByNombreContainingIgnoreCase(String nombre) {
        return autorRepository.findByNombreContainingIgnoreCase(nombre).stream().map(mapper::autorToDto).toList();
    }

    public boolean existsByNombreIgnoreCase(String nombre) {
        return autorRepository.existsByNombreIgnoreCase(nombre);
    }
}
