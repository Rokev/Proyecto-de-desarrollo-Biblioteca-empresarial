package persistanceLayer.dao;

import bussinesLayer.dto.TrasladoRetornoDTO;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.DevolucionEntity;
import persistanceLayer.entity.EjemplarEntity;
import persistanceLayer.entity.SedeEntity;
import persistanceLayer.entity.TrasladoRetornoEntity;
import persistanceLayer.entity.UsuarioEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.TrasladoRetornoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de TrasladoRetorno: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class TrasladoRetornoDAO {

    private final TrasladoRetornoRepository trasladoRepository;
    private final Mapper mapper;
    private final EntityManager entityManager;

    public TrasladoRetornoDTO save(TrasladoRetornoDTO dto) {
        TrasladoRetornoEntity entity = mapper.trasladoRetornoToEntity(dto);
        entity.setId(null);
        entity.setEjemplar(referenciaEjemplar(dto.getEjemplarId()));
        entity.setSedeActual(referenciaSede(dto.getSedeActualId()));
        entity.setSedeDestino(referenciaSede(dto.getSedeDestinoId()));
        entity.setDevolucion(referenciaDevolucion(dto.getDevolucionId()));
        entity.setBibliotecarioRecepcion(referenciaUsuario(dto.getBibliotecarioId()));
        TrasladoRetornoEntity guardado = trasladoRepository.save(entity);
        // Las relaciones son referencias solo con id: se recarga para devolver el DTO completo.
        entityManager.flush();
        entityManager.refresh(guardado);
        return mapper.trasladoRetornoToDto(guardado);
    }

    public Optional<TrasladoRetornoDTO> findById(Long id) {
        return trasladoRepository.findById(id).map(mapper::trasladoRetornoToDto);
    }

    /** Solo cambia lo que varia en la vida del traslado: estado, recepcion y bibliotecario. */
    public Optional<TrasladoRetornoDTO> update(Long id, TrasladoRetornoDTO dto) {
        return trasladoRepository.findById(id).map(existente -> {
            if (dto.getEstado() != null) {
                existente.setEstado(dto.getEstado());
            }
            existente.setFechaRecepcion(dto.getFechaRecepcion());
            existente.setBibliotecarioRecepcion(referenciaUsuario(dto.getBibliotecarioId()));
            return mapper.trasladoRetornoToDto(trasladoRepository.save(existente));
        });
    }

    public List<TrasladoRetornoDTO> findBySedeDestinoIdOrderByFechaCreacionAsc(Long sedeDestinoId) {
        return trasladoRepository.findBySedeDestinoIdOrderByFechaCreacionAsc(sedeDestinoId).stream().map(mapper::trasladoRetornoToDto).toList();
    }

    public List<TrasladoRetornoDTO> findBySedeDestinoIdAndEstadoIgnoreCaseOrderByFechaCreacionAsc(Long sedeDestinoId, String estado) {
        return trasladoRepository.findBySedeDestinoIdAndEstadoIgnoreCaseOrderByFechaCreacionAsc(sedeDestinoId, estado).stream().map(mapper::trasladoRetornoToDto).toList();
    }

    private EjemplarEntity referenciaEjemplar(Long id) {
        if (id == null) {
            return null;
        }
        EjemplarEntity referencia = new EjemplarEntity();
        referencia.setId(id);
        return referencia;
    }

    private SedeEntity referenciaSede(Long id) {
        if (id == null) {
            return null;
        }
        SedeEntity referencia = new SedeEntity();
        referencia.setId(id);
        return referencia;
    }

    private DevolucionEntity referenciaDevolucion(Long id) {
        if (id == null) {
            return null;
        }
        DevolucionEntity referencia = new DevolucionEntity();
        referencia.setId(id);
        return referencia;
    }

    private UsuarioEntity referenciaUsuario(Long id) {
        if (id == null) {
            return null;
        }
        UsuarioEntity referencia = new UsuarioEntity();
        referencia.setId(id);
        return referencia;
    }
}