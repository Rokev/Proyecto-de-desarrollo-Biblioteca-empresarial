package persistanceLayer.dao;

import bussinesLayer.dto.PedidoInterciudadDTO;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.EjemplarEntity;
import persistanceLayer.entity.PedidoInterciudadEntity;
import persistanceLayer.entity.PrestamoEntity;
import persistanceLayer.entity.SedeEntity;
import persistanceLayer.entity.UsuarioEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.PedidoInterciudadRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de PedidoInterciudad: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class PedidoInterciudadDAO {

    private final PedidoInterciudadRepository pedidoRepository;
    private final Mapper mapper;
    private final EntityManager entityManager;

    public PedidoInterciudadDTO save(PedidoInterciudadDTO dto) {
        PedidoInterciudadEntity entity = mapper.pedidoInterciudadToEntity(dto);
        entity.setId(null);
        entity.setUsuario(referenciaUsuario(dto.getUsuarioId()));
        entity.setEjemplar(referenciaEjemplar(dto.getEjemplarId()));
        entity.setSedeOrigen(referenciaSede(dto.getSedeOrigenId()));
        entity.setPrestamo(referenciaPrestamo(dto.getPrestamoId()));
        entity.setBibliotecario(referenciaUsuario(dto.getBibliotecarioId()));
        PedidoInterciudadEntity guardado = pedidoRepository.save(entity);
        // Las relaciones son referencias solo con id: se recarga para devolver el DTO completo.
        entityManager.flush();
        entityManager.refresh(guardado);
        return mapper.pedidoInterciudadToDto(guardado);
    }

    public Optional<PedidoInterciudadDTO> findById(Long id) {
        return pedidoRepository.findById(id).map(mapper::pedidoInterciudadToDto);
    }

    /** Solo actualiza lo que cambia durante la vida del pedido: estado, fechas y bibliotecario. */
    public Optional<PedidoInterciudadDTO> update(Long id, PedidoInterciudadDTO dto) {
        return pedidoRepository.findById(id).map(existente -> {
            if (dto.getEstado() != null) {
                existente.setEstado(dto.getEstado());
            }
            existente.setFechaTransito(dto.getFechaTransito());
            existente.setFechaDisponible(dto.getFechaDisponible());
            existente.setFechaRetiro(dto.getFechaRetiro());
            existente.setFechaCancelacion(dto.getFechaCancelacion());
            existente.setBibliotecario(referenciaUsuario(dto.getBibliotecarioId()));
            return mapper.pedidoInterciudadToDto(pedidoRepository.save(existente));
        });
    }

    public List<PedidoInterciudadDTO> findByUsuarioIdOrderByFechaSolicitudDesc(Long usuarioId) {
        return pedidoRepository.findByUsuarioIdOrderByFechaSolicitudDesc(usuarioId).stream().map(mapper::pedidoInterciudadToDto).toList();
    }

    public List<PedidoInterciudadDTO> findByUsuarioIdAndEstadoIgnoreCaseOrderByFechaSolicitudDesc(Long usuarioId, String estado) {
        return pedidoRepository.findByUsuarioIdAndEstadoIgnoreCaseOrderByFechaSolicitudDesc(usuarioId, estado).stream().map(mapper::pedidoInterciudadToDto).toList();
    }

    public List<PedidoInterciudadDTO> findByEstadoIgnoreCaseOrderByFechaSolicitudAsc(String estado) {
        return pedidoRepository.findByEstadoIgnoreCaseOrderByFechaSolicitudAsc(estado).stream().map(mapper::pedidoInterciudadToDto).toList();
    }

    private UsuarioEntity referenciaUsuario(Long id) {
        if (id == null) {
            return null;
        }
        UsuarioEntity referencia = new UsuarioEntity();
        referencia.setId(id);
        return referencia;
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

    private PrestamoEntity referenciaPrestamo(Long id) {
        if (id == null) {
            return null;
        }
        PrestamoEntity referencia = new PrestamoEntity();
        referencia.setId(id);
        return referencia;
    }
}