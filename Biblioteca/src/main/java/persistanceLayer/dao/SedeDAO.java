package persistanceLayer.dao;

import bussinesLayer.dto.SedeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.SedeEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.SedeRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de Sede: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class SedeDAO {

    private final SedeRepository sedeRepository;
    private final Mapper mapper;

    public SedeDTO save(SedeDTO dto) {
        SedeEntity entity = mapper.sedeToEntity(dto);
        entity.setId(null);
        SedeEntity guardado = sedeRepository.save(entity);
        return mapper.sedeToDto(guardado);
    }

    public Optional<SedeDTO> findById(Long id) {
        return sedeRepository.findById(id).map(mapper::sedeToDto);
    }

    public List<SedeDTO> findAll() {
        return sedeRepository.findAll().stream().map(mapper::sedeToDto).toList();
    }

    public Optional<SedeDTO> update(Long id, SedeDTO dto) {
        return sedeRepository.findById(id).map(existente -> {
            existente.setNombre(dto.getNombre());
            existente.setDireccion(dto.getDireccion());
            existente.setCiudad(dto.getCiudad());
            existente.setTelefono(dto.getTelefono());
            existente.setCorreo(dto.getCorreo());
            existente.setLatitud(dto.getLatitud());
            existente.setLongitud(dto.getLongitud());
            existente.setHoraApertura(dto.getHoraApertura());
            existente.setHoraCierre(dto.getHoraCierre());
            if (dto.getAliada() != null) {
                existente.setAliada(dto.getAliada());
            }
            if (dto.getEnOperacion() != null) {
                existente.setEnOperacion(dto.getEnOperacion());
            }
            return mapper.sedeToDto(sedeRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return sedeRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (sedeRepository.existsById(id)) {
            sedeRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<SedeDTO> findByNombreIgnoreCase(String nombre) {
        return sedeRepository.findByNombreIgnoreCase(nombre).map(mapper::sedeToDto);
    }

    public boolean existsByNombreIgnoreCase(String nombre) {
        return sedeRepository.existsByNombreIgnoreCase(nombre);
    }

    public List<SedeDTO> findByCiudadIgnoreCase(String ciudad) {
        return sedeRepository.findByCiudadIgnoreCase(ciudad).stream().map(mapper::sedeToDto).toList();
    }

    public List<SedeDTO> findByEnOperacionTrue() {
        return sedeRepository.findByEnOperacionTrue().stream().map(mapper::sedeToDto).toList();
    }

    public List<SedeDTO> findByAliada(Boolean aliada) {
        return sedeRepository.findByAliada(aliada).stream().map(mapper::sedeToDto).toList();
    }

    /** Sede en operación más cercana a unas coordenadas GPS (distancia haversine en km). */
    public Optional<SedeDTO> findMasCercana(double latitud, double longitud) {
        return sedeRepository.findByEnOperacionTrue().stream()
                .filter(s -> s.getLatitud() != null && s.getLongitud() != null)
                .min(Comparator.comparingDouble(
                        s -> distanciaKm(latitud, longitud, s.getLatitud(), s.getLongitud())))
                .map(mapper::sedeToDto);
    }

    private static double distanciaKm(double lat1, double lon1, double lat2, double lon2) {
        final double radioTierraKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return radioTierraKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
