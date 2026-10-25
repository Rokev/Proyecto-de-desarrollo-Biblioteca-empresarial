package persistanceLayer.dao;

import bussinesLayer.dto.PlazoSistemaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.PlazoSistemaEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.PlazoSistemaRepository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PlazoSistemaDAO {

    private final PlazoSistemaRepository repository;
    private final Mapper mapper;

    public List<PlazoSistemaDTO> findAll() {
        return repository.findAll().stream().map(mapper::plazoSistemaToDto).toList();
    }

    public Optional<PlazoSistemaDTO> findByClave(String clave) {
        return repository.findByClave(clave).map(mapper::plazoSistemaToDto);
    }

    /** Crea el plazo si no existe; si existe, actualiza sus valores. */
    public PlazoSistemaDTO guardar(PlazoSistemaDTO dto) {
        PlazoSistemaEntity entity = repository.findByClave(dto.getClave())
                .orElseGet(() -> {
                    PlazoSistemaEntity nuevo = mapper.plazoSistemaToEntity(dto);
                    nuevo.setId(null);
                    return nuevo;
                });
        entity.setValor(dto.getValor());
        entity.setUnidad(dto.getUnidad());
        entity.setDescripcion(dto.getDescripcion());
        return mapper.plazoSistemaToDto(repository.save(entity));
    }
}