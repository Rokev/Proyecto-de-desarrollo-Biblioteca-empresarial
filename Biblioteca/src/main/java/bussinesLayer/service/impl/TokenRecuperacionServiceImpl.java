package bussinesLayer.service.impl;

import bussinesLayer.dto.TokenRecuperacionDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.TokenRecuperacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.TokenRecuperacionDAO;
import persistanceLayer.dao.UsuarioDAO;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TokenRecuperacionServiceImpl implements TokenRecuperacionService {

    private final TokenRecuperacionDAO tokenRecuperacionDAO;
    private final UsuarioDAO usuarioDAO;

    @Override
    public TokenRecuperacionDTO crearTokenRecuperacion(TokenRecuperacionDTO dto) {
        log.info("Creando token de recuperacion para el usuario: {}", dto.getUsuarioId());

        if (tokenRecuperacionDAO.findByToken(dto.getToken()).isPresent()) {
            throw new BusinessValidationException("Ya existe un token de recuperacion con el valor indicado");
        }
        validar(dto);

        // Solo un token vigente por usuario: se invalidan los anteriores.
        tokenRecuperacionDAO.invalidarTokensDeUsuario(dto.getUsuarioId());
        dto.setUsado(false);

        TokenRecuperacionDTO guardado = tokenRecuperacionDAO.save(dto);
        log.info("Token de recuperacion creado con ID: {}", guardado.getId());
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public TokenRecuperacionDTO obtenerTokenRecuperacionPorId(Long id) {
        return tokenRecuperacionDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Token de recuperacion no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TokenRecuperacionDTO> obtenerTodosLosTokensRecuperacion() {
        return tokenRecuperacionDAO.findAll();
    }

    @Override
    public TokenRecuperacionDTO actualizarTokenRecuperacion(Long id, TokenRecuperacionDTO dto) {
        if (!tokenRecuperacionDAO.existsById(id)) {
            throw new ResourceNotFoundException("Token de recuperacion no encontrado con ID: " + id);
        }

        tokenRecuperacionDAO.findByToken(dto.getToken())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new BusinessValidationException("Ya existe otro token de recuperacion con el valor indicado");
                });
        validar(dto);

        TokenRecuperacionDTO actualizado = tokenRecuperacionDAO.update(id, dto)
                .orElseThrow(() -> new ResourceNotFoundException("Token de recuperacion no encontrado con ID: " + id));
        log.info("Token de recuperacion actualizado ID: {}", id);
        return actualizado;
    }

    @Override
    public void eliminarTokenRecuperacion(Long id) {
        if (!tokenRecuperacionDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Token de recuperacion no encontrado con ID: " + id);
        }
        log.info("Token de recuperacion eliminado ID: {}", id);
    }

    private void validar(TokenRecuperacionDTO dto) {
        if (!usuarioDAO.existsById(dto.getUsuarioId())) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + dto.getUsuarioId());
        }
        if (!dto.getFechaExpiracion().isAfter(dto.getFechaCreacion())) {
            throw new BusinessValidationException("La fechaExpiracion debe ser posterior a la fechaCreacion");
        }
    }
}
