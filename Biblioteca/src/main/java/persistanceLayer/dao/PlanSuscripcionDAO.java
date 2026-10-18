package persistanceLayer.dao;

import bussinesLayer.dto.PlanSuscripcionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.PlanSuscripcionEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.PlanSuscripcionRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de PlanSuscripcion: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class PlanSuscripcionDAO {

    private final PlanSuscripcionRepository planSuscripcionRepository;
    private final Mapper mapper;

    public PlanSuscripcionDTO save(PlanSuscripcionDTO dto) {
        PlanSuscripcionEntity entity = mapper.planSuscripcionToEntity(dto);
        entity.setId(null);
        PlanSuscripcionEntity guardado = planSuscripcionRepository.save(entity);
        return mapper.planSuscripcionToDto(guardado);
    }

    public Optional<PlanSuscripcionDTO> findById(Long id) {
        return planSuscripcionRepository.findById(id).map(mapper::planSuscripcionToDto);
    }

    public List<PlanSuscripcionDTO> findAll() {
        return planSuscripcionRepository.findAll().stream().map(mapper::planSuscripcionToDto).toList();
    }

    public Optional<PlanSuscripcionDTO> update(Long id, PlanSuscripcionDTO dto) {
        return planSuscripcionRepository.findById(id).map(existente -> {
            existente.setNombre(dto.getNombre());
            existente.setDescripcion(dto.getDescripcion());
            existente.setBeneficios(dto.getBeneficios());
            existente.setPrecio(dto.getPrecio());
            existente.setDuracionMeses(dto.getDuracionMeses());
            if (dto.getActivo() != null) {
                existente.setActivo(dto.getActivo());
            }
            return mapper.planSuscripcionToDto(planSuscripcionRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return planSuscripcionRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (planSuscripcionRepository.existsById(id)) {
            planSuscripcionRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<PlanSuscripcionDTO> findByNombreIgnoreCase(String nombre) {
        return planSuscripcionRepository.findByNombreIgnoreCase(nombre).map(mapper::planSuscripcionToDto);
    }

    public boolean existsByNombreIgnoreCase(String nombre) {
        return planSuscripcionRepository.existsByNombreIgnoreCase(nombre);
    }

    public List<PlanSuscripcionDTO> findByActivoTrue() {
        return planSuscripcionRepository.findByActivoTrue().stream().map(mapper::planSuscripcionToDto).toList();
    }
}
