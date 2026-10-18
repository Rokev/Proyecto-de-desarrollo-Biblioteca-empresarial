package persistanceLayer.dao;

import bussinesLayer.dto.TokenRecuperacionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.entity.TokenRecuperacionEntity;
import persistanceLayer.entity.UsuarioEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.TokenRecuperacionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de TokenRecuperacion: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class TokenRecuperacionDAO {

    private final TokenRecuperacionRepository tokenRecuperacionRepository;
    private final Mapper mapper;

    public TokenRecuperacionDTO save(TokenRecuperacionDTO dto) {
        TokenRecuperacionEntity entity = mapper.tokenRecuperacionToEntity(dto);
        entity.setId(null);
        entity.setUsuario(referenciaUsuario(dto.getUsuarioId()));
        TokenRecuperacionEntity guardado = tokenRecuperacionRepository.save(entity);
        return mapper.tokenRecuperacionToDto(guardado);
    }

    public Optional<TokenRecuperacionDTO> findById(Long id) {
        return tokenRecuperacionRepository.findById(id).map(mapper::tokenRecuperacionToDto);
    }

    public List<TokenRecuperacionDTO> findAll() {
        return tokenRecuperacionRepository.findAll().stream().map(mapper::tokenRecuperacionToDto).toList();
    }

    public Optional<TokenRecuperacionDTO> update(Long id, TokenRecuperacionDTO dto) {
        return tokenRecuperacionRepository.findById(id).map(existente -> {
            existente.setToken(dto.getToken());
            existente.setFechaCreacion(dto.getFechaCreacion());
            existente.setFechaExpiracion(dto.getFechaExpiracion());
            if (dto.getUsado() != null) {
                existente.setUsado(dto.getUsado());
            }
            existente.setUsuario(referenciaUsuario(dto.getUsuarioId()));
            return mapper.tokenRecuperacionToDto(tokenRecuperacionRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return tokenRecuperacionRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (tokenRecuperacionRepository.existsById(id)) {
            tokenRecuperacionRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<TokenRecuperacionDTO> findByToken(String token) {
        return tokenRecuperacionRepository.findByToken(token).map(mapper::tokenRecuperacionToDto);
    }

    public Optional<TokenRecuperacionDTO> findByTokenAndUsadoFalse(String token) {
        return tokenRecuperacionRepository.findByTokenAndUsadoFalse(token).map(mapper::tokenRecuperacionToDto);
    }

    public List<TokenRecuperacionDTO> findByUsuarioIdAndUsadoFalse(Long usuarioId) {
        return tokenRecuperacionRepository.findByUsuarioIdAndUsadoFalse(usuarioId).stream().map(mapper::tokenRecuperacionToDto).toList();
    }

    @Transactional
    public int consumirToken(String token, LocalDateTime ahora) {
        return tokenRecuperacionRepository.consumirToken(token, ahora);
    }

    @Transactional
    public int invalidarTokensDeUsuario(Long usuarioId) {
        return tokenRecuperacionRepository.invalidarTokensDeUsuario(usuarioId);
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
