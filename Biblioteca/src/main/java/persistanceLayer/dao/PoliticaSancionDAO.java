package persistanceLayer.dao;

import bussinesLayer.dto.PoliticaSancionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.PoliticaSancionEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.PoliticaSancionRepository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PoliticaSancionDAO {

    private final PoliticaSancionRepository repository;
    private final Mapper mapper;

    public List<PoliticaSancionDTO> findAll() {
        return repository.findAll().stream().map(mapper::politicaSancionToDto).toList();
    }

    public Optional<PoliticaSancionDTO> findByInfraccion(String infraccion) {
        return repository.findByInfraccion(infraccion).map(mapper::politicaSancionToDto);
    }

    /** Crea la política de la infracción si no existe; si existe, actualiza sus valores. */
    public PoliticaSancionDTO guardar(PoliticaSancionDTO dto) {
        PoliticaSancionEntity entity = repository.findByInfraccion(dto.getInfraccion())
                .orElseGet(() -> {
                    PoliticaSancionEntity nueva = mapper.politicaSancionToEntity(dto);
                    nueva.setId(null);
                    return nueva;
                });
        entity.setTipoSancion(dto.getTipoSancion());
        entity.setDuracionDias(dto.getDuracionDias());
        entity.setActiva(dto.getActiva());
        entity.setDescripcion(dto.getDescripcion());
        return mapper.politicaSancionToDto(repository.save(entity));
    }
}