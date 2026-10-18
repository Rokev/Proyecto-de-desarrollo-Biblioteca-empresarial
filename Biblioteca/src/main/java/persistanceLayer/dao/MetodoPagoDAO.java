package persistanceLayer.dao;

import bussinesLayer.dto.MetodoPagoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.entity.MetodoPagoEntity;
import persistanceLayer.entity.UsuarioEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.MetodoPagoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de MetodoPago: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class MetodoPagoDAO {

    private final MetodoPagoRepository metodoPagoRepository;
    private final Mapper mapper;

    public MetodoPagoDTO save(MetodoPagoDTO dto) {
        MetodoPagoEntity entity = mapper.metodoPagoToEntity(dto);
        entity.setId(null);
        entity.setUsuario(referenciaUsuario(dto.getUsuarioId()));
        MetodoPagoEntity guardado = metodoPagoRepository.save(entity);
        return mapper.metodoPagoToDto(guardado);
    }

    public Optional<MetodoPagoDTO> findById(Long id) {
        return metodoPagoRepository.findById(id).map(mapper::metodoPagoToDto);
    }

    public List<MetodoPagoDTO> findAll() {
        return metodoPagoRepository.findAll().stream().map(mapper::metodoPagoToDto).toList();
    }

    public Optional<MetodoPagoDTO> update(Long id, MetodoPagoDTO dto) {
        return metodoPagoRepository.findById(id).map(existente -> {
            existente.setTipo(dto.getTipo());
            existente.setMarca(dto.getMarca());
            existente.setUltimosCuatroDigitos(dto.getUltimosCuatroDigitos());
            existente.setTitular(dto.getTitular());
            if (dto.getTokenPasarela() != null) {
                existente.setTokenPasarela(dto.getTokenPasarela());
            }
            if (dto.getPredeterminado() != null) {
                existente.setPredeterminado(dto.getPredeterminado());
            }
            existente.setUsuario(referenciaUsuario(dto.getUsuarioId()));
            return mapper.metodoPagoToDto(metodoPagoRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return metodoPagoRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (metodoPagoRepository.existsById(id)) {
            metodoPagoRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<MetodoPagoDTO> findByUsuarioId(Long usuarioId) {
        return metodoPagoRepository.findByUsuarioId(usuarioId).stream().map(mapper::metodoPagoToDto).toList();
    }

    public Optional<MetodoPagoDTO> findByIdAndUsuarioId(Long id, Long usuarioId) {
        return metodoPagoRepository.findByIdAndUsuarioId(id, usuarioId).map(mapper::metodoPagoToDto);
    }

    public Optional<MetodoPagoDTO> findFirstByUsuarioIdAndPredeterminadoTrue(Long usuarioId) {
        return metodoPagoRepository.findFirstByUsuarioIdAndPredeterminadoTrue(usuarioId).map(mapper::metodoPagoToDto);
    }

    public long countByUsuarioId(Long usuarioId) {
        return metodoPagoRepository.countByUsuarioId(usuarioId);
    }

    @Transactional
    public int quitarPredeterminado(Long usuarioId) {
        return metodoPagoRepository.quitarPredeterminado(usuarioId);
    }

    private UsuarioEntity referenciaUsuario(Long idUsuario) {
        if (idUsuario == null) {
            return null;
        }
        UsuarioEntity referencia = new UsuarioEntity();
        referencia.setId(idUsuario);
        return referencia;
    }
}
