package persistanceLayer.dao;

import bussinesLayer.dto.PagoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.MetodoPagoEntity;
import persistanceLayer.entity.MultaEntity;
import persistanceLayer.entity.PagoEntity;
import persistanceLayer.entity.ReservaSalaEntity;
import persistanceLayer.entity.SuscripcionEntity;
import persistanceLayer.entity.UsuarioEntity;
import persistanceLayer.entity.PedidoInterciudadEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.PagoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de Pago: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class PagoDAO {

    private final PagoRepository pagoRepository;
    private final Mapper mapper;

    public PagoDTO save(PagoDTO dto) {
        PagoEntity entity = mapper.pagoToEntity(dto);
        entity.setId(null);
        entity.setUsuario(referenciaUsuario(dto.getUsuarioId()));
        entity.setMetodoPago(referenciaMetodoPago(dto.getMetodoPagoId()));
        entity.setMulta(referenciaMulta(dto.getMultaId()));
        entity.setSuscripcion(referenciaSuscripcion(dto.getSuscripcionId()));
        entity.setReservaSala(referenciaReservaSala(dto.getReservaSalaId()));
        entity.setPedidoInterciudad(referenciaPedidoInterciudad(dto.getPedidoInterciudadId()));
        PagoEntity guardado = pagoRepository.save(entity);
        return mapper.pagoToDto(guardado);
    }

    public Optional<PagoDTO> findById(Long id) {
        return pagoRepository.findById(id).map(mapper::pagoToDto);
    }

    public List<PagoDTO> findAll() {
        return pagoRepository.findAll().stream().map(mapper::pagoToDto).toList();
    }

    public Optional<PagoDTO> update(Long id, PagoDTO dto) {
        return pagoRepository.findById(id).map(existente -> {
            existente.setMonto(dto.getMonto());
            existente.setFechaPago(dto.getFechaPago());
            existente.setConcepto(dto.getConcepto());
            existente.setEstado(dto.getEstado());
            existente.setReferencia(dto.getReferencia());
            existente.setUsuario(referenciaUsuario(dto.getUsuarioId()));
            existente.setMetodoPago(referenciaMetodoPago(dto.getMetodoPagoId()));
            existente.setMulta(referenciaMulta(dto.getMultaId()));
            existente.setSuscripcion(referenciaSuscripcion(dto.getSuscripcionId()));
            existente.setReservaSala(referenciaReservaSala(dto.getReservaSalaId()));
            existente.setPedidoInterciudad(referenciaPedidoInterciudad(dto.getPedidoInterciudadId()));
            return mapper.pagoToDto(pagoRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return pagoRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (pagoRepository.existsById(id)) {
            pagoRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<PagoDTO> findByUsuarioIdOrderByFechaPagoDesc(Long usuarioId) {
        return pagoRepository.findByUsuarioIdOrderByFechaPagoDesc(usuarioId).stream().map(mapper::pagoToDto).toList();
    }

    public Optional<PagoDTO> findByReferencia(String referencia) {
        return pagoRepository.findByReferencia(referencia).map(mapper::pagoToDto);
    }

    public boolean existsByReferencia(String referencia) {
        return pagoRepository.existsByReferencia(referencia);
    }

    public List<PagoDTO> findByMultaId(Long multaId) {
        return pagoRepository.findByMultaId(multaId).stream().map(mapper::pagoToDto).toList();
    }

    public List<PagoDTO> findBySuscripcionId(Long suscripcionId) {
        return pagoRepository.findBySuscripcionId(suscripcionId).stream().map(mapper::pagoToDto).toList();
    }

    public List<PagoDTO> findByReservaSalaId(Long reservaSalaId) {
        return pagoRepository.findByReservaSalaId(reservaSalaId).stream().map(mapper::pagoToDto).toList();
    }

    public List<PagoDTO> findByConceptoIgnoreCase(String concepto) {
        return pagoRepository.findByConceptoIgnoreCase(concepto).stream().map(mapper::pagoToDto).toList();
    }

    public List<PagoDTO> findByEstadoIgnoreCase(String estado) {
        return pagoRepository.findByEstadoIgnoreCase(estado).stream().map(mapper::pagoToDto).toList();
    }

    public Optional<PagoDTO> findEnvioPagadoPorPedido(Long pedidoId) {
        return pagoRepository.findEnviosPagadosPorPedido(pedidoId).stream().findFirst().map(mapper::pagoToDto);
    }

    private PedidoInterciudadEntity referenciaPedidoInterciudad(Long idPedido) {
        if (idPedido == null) {
            return null;
        }
        PedidoInterciudadEntity referencia = new PedidoInterciudadEntity();
        referencia.setId(idPedido);
        return referencia;
    }

    private UsuarioEntity referenciaUsuario(Long idUsuario) {
        if (idUsuario == null) {
            return null;
        }
        UsuarioEntity referencia = new UsuarioEntity();
        referencia.setId(idUsuario);
        return referencia;
    }

    private MetodoPagoEntity referenciaMetodoPago(Long idMetodoPago) {
        if (idMetodoPago == null) {
            return null;
        }
        MetodoPagoEntity referencia = new MetodoPagoEntity();
        referencia.setId(idMetodoPago);
        return referencia;
    }

    private MultaEntity referenciaMulta(Long idMulta) {
        if (idMulta == null) {
            return null;
        }
        MultaEntity referencia = new MultaEntity();
        referencia.setId(idMulta);
        return referencia;
    }

    private SuscripcionEntity referenciaSuscripcion(Long idSuscripcion) {
        if (idSuscripcion == null) {
            return null;
        }
        SuscripcionEntity referencia = new SuscripcionEntity();
        referencia.setId(idSuscripcion);
        return referencia;
    }

    private ReservaSalaEntity referenciaReservaSala(Long idReservaSala) {
        if (idReservaSala == null) {
            return null;
        }
        ReservaSalaEntity referencia = new ReservaSalaEntity();
        referencia.setId(idReservaSala);
        return referencia;
    }
}
