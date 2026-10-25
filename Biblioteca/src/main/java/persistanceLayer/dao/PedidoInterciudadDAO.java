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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
        entity.setSedeDevolucion(referenciaSede(dto.getSedeDevolucionId()));
        entity.setRepartidor(referenciaUsuario(dto.getRepartidorId()));
        entity.setPrestamo(referenciaPrestamo(dto.getPrestamoId()));
        entity.setBibliotecario(referenciaUsuario(dto.getBibliotecarioId()));

        PedidoInterciudadEntity guardado = pedidoRepository.save(entity);
        entityManager.flush();
        entityManager.refresh(guardado);
        return mapper.pedidoInterciudadToDto(guardado);
    }

    public Optional<PedidoInterciudadDTO> findById(Long id) {
        return pedidoRepository.findById(id).map(mapper::pedidoInterciudadToDto);
    }

    public Optional<PedidoInterciudadDTO> update(Long id, PedidoInterciudadDTO dto) {
        return pedidoRepository.findById(id).map(existente -> {
            if (dto.getEstado() != null) existente.setEstado(dto.getEstado());
            existente.setFechaTransito(dto.getFechaTransito());
            existente.setFechaEntrega(dto.getFechaEntrega());
            existente.setFechaCancelacion(dto.getFechaCancelacion());
            existente.setFechaLimiteRecogida(dto.getFechaLimiteRecogida());
            existente.setFechaLimiteDevolucion(dto.getFechaLimiteDevolucion());
            existente.setRepartidor(referenciaUsuario(dto.getRepartidorId()));
            existente.setBibliotecario(referenciaUsuario(dto.getBibliotecarioId()));
            
            PedidoInterciudadEntity actualizado = pedidoRepository.save(existente);
            entityManager.flush();
            entityManager.refresh(actualizado);
            return mapper.pedidoInterciudadToDto(actualizado);
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

    public List<PedidoInterciudadDTO> findByRepartidorIdAndEstados(Long repartidorId, Collection<String> estados) {
        return pedidoRepository.findByRepartidorIdAndEstadoInOrderByFechaSolicitudAsc(repartidorId, estados).stream().map(mapper::pedidoInterciudadToDto).toList();
    }

    public List<PedidoInterciudadDTO> findBySedeOrigenAndEstados(Long sedeOrigenId, Collection<String> estados) {
        return pedidoRepository.findBySedeOrigenIdAndEstadoInOrderByFechaSolicitudAsc(sedeOrigenId, estados).stream().map(mapper::pedidoInterciudadToDto).toList();
    }

    public List<PedidoInterciudadDTO> findBySedeDevolucionAndEstados(Long sedeDevolucionId, Collection<String> estados) {
        return pedidoRepository.findBySedeDevolucionIdAndEstadoInOrderByFechaSolicitudAsc(sedeDevolucionId, estados).stream().map(mapper::pedidoInterciudadToDto).toList();
    }

    public List<PedidoInterciudadDTO> findPendientesDeTraslado() {
    return pedidoRepository.findByEstadoIgnoreCaseOrderByFechaSolicitudAsc("DEVUELTO_PENDIENTE_TRASLADO")
            .stream()
            .map(mapper::pedidoInterciudadToDto)
            .toList();
}

    private UsuarioEntity referenciaUsuario(Long id) {
        if (id == null) return null;
        UsuarioEntity ref = new UsuarioEntity();
        ref.setId(id);
        return ref;
    }

    private EjemplarEntity referenciaEjemplar(Long id) {
        if (id == null) return null;
        EjemplarEntity ref = new EjemplarEntity();
        ref.setId(id);
        return ref;
    }

    private SedeEntity referenciaSede(Long id) {
        if (id == null) return null;
        SedeEntity ref = new SedeEntity();
        ref.setId(id);
        return ref;
    }

    private PrestamoEntity referenciaPrestamo(Long id) {
        if (id == null) return null;
        PrestamoEntity ref = new PrestamoEntity();
        ref.setId(id);
        return ref;
    }
}