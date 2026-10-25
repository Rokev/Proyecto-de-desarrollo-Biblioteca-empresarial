package persistanceLayer.dao;

import bussinesLayer.dto.PoliticaMultaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.PoliticaMultaEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.PoliticaMultaRepository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PoliticaMultaDAO {

    private final PoliticaMultaRepository repository;
    private final Mapper mapper;

    public List<PoliticaMultaDTO> findAll() {
        return repository.findAll().stream().map(mapper::politicaMultaToDto).toList();
    }

    public Optional<PoliticaMultaDTO> findByConcepto(String concepto) {
        return repository.findByConcepto(concepto).map(mapper::politicaMultaToDto);
    }

    /** Crea la política del concepto si no existe; si existe, actualiza sus valores. */
    public PoliticaMultaDTO guardar(PoliticaMultaDTO dto) {
        PoliticaMultaEntity entity = repository.findByConcepto(dto.getConcepto())
                .orElseGet(() -> {
                    PoliticaMultaEntity nueva = mapper.politicaMultaToEntity(dto);
                    nueva.setId(null);
                    return nueva;
                });
        entity.setMonto(dto.getMonto());
        entity.setDescripcion(dto.getDescripcion());
        return mapper.politicaMultaToDto(repository.save(entity));
    }
}