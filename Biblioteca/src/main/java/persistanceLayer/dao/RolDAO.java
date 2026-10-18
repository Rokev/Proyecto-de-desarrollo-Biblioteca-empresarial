package persistanceLayer.dao;

import bussinesLayer.dto.RolDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.RolEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.RolRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de Rol: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class RolDAO {

    private final RolRepository rolRepository;
    private final Mapper mapper;

    public RolDTO save(RolDTO dto) {
        RolEntity entity = mapper.rolToEntity(dto);
        entity.setId(null);
        RolEntity guardado = rolRepository.save(entity);
        return mapper.rolToDto(guardado);
    }

    public Optional<RolDTO> findById(Long id) {
        return rolRepository.findById(id).map(mapper::rolToDto);
    }

    public List<RolDTO> findAll() {
        return rolRepository.findAll().stream().map(mapper::rolToDto).toList();
    }

    public Optional<RolDTO> update(Long id, RolDTO dto) {
        return rolRepository.findById(id).map(existente -> {
            existente.setNombre(dto.getNombre());
            existente.setDescripcion(dto.getDescripcion());
            return mapper.rolToDto(rolRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return rolRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (rolRepository.existsById(id)) {
            rolRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<RolDTO> findByNombreIgnoreCase(String nombre) {
        return rolRepository.findByNombreIgnoreCase(nombre).map(mapper::rolToDto);
    }

    public boolean existsByNombreIgnoreCase(String nombre) {
        return rolRepository.existsByNombreIgnoreCase(nombre);
    }
}
