package persistanceLayer.dao;

import bussinesLayer.dto.TarifaEnvioDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.TarifaEnvioEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.TarifaEnvioRepository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TarifaEnvioDAO {

    private final TarifaEnvioRepository tarifaEnvioRepository;
    private final Mapper mapper;

    public TarifaEnvioDTO save(TarifaEnvioDTO dto) {
        TarifaEnvioEntity entity = mapper.tarifaEnvioToEntity(dto);
        entity.setId(null);
        return mapper.tarifaEnvioToDto(tarifaEnvioRepository.save(entity));
    }

    public Optional<TarifaEnvioDTO> findById(Long id) {
        return tarifaEnvioRepository.findById(id).map(mapper::tarifaEnvioToDto);
    }

    public List<TarifaEnvioDTO> findAll() {
        return tarifaEnvioRepository.findAll().stream().map(mapper::tarifaEnvioToDto).toList();
    }

    public Optional<TarifaEnvioDTO> findByPar(String ciudadA, String ciudadB) {
        return tarifaEnvioRepository.findByPar(ciudadA, ciudadB).map(mapper::tarifaEnvioToDto);
    }

    public Optional<TarifaEnvioDTO> update(Long id, TarifaEnvioDTO dto) {
        return tarifaEnvioRepository.findById(id).map(existente -> {
            existente.setCiudadA(dto.getCiudadA());
            existente.setCiudadB(dto.getCiudadB());
            existente.setTarifa(dto.getTarifa());
            return mapper.tarifaEnvioToDto(tarifaEnvioRepository.save(existente));
        });
    }

    public boolean deleteById(Long id) {
        if (tarifaEnvioRepository.existsById(id)) {
            tarifaEnvioRepository.deleteById(id);
            return true;
        }
        return false;
    }
}