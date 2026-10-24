package persistanceLayer.dao;

import bussinesLayer.dto.SuscripcionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.PlanSuscripcionEntity;
import persistanceLayer.entity.SuscripcionEntity;
import persistanceLayer.entity.UsuarioEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.SuscripcionRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de Suscripcion: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
public class SuscripcionDAO {

    private final SuscripcionRepository suscripcionRepository;
    private final Mapper mapper;

    public SuscripcionDTO save(SuscripcionDTO dto) {
        SuscripcionEntity entity = mapper.suscripcionToEntity(dto);
        entity.setId(null);
        entity.setUsuario(referenciaUsuario(dto.getUsuarioId()));
        entity.setPlan(referenciaPlan(dto.getPlanId()));
        SuscripcionEntity guardado = suscripcionRepository.save(entity);
        return mapper.suscripcionToDto(guardado);
    }

    public Optional<SuscripcionDTO> findById(Long id) {
        return suscripcionRepository.findById(id).map(mapper::suscripcionToDto);
    }

    public List<SuscripcionDTO> findAll() {
        return suscripcionRepository.findAll().stream().map(mapper::suscripcionToDto).toList();
    }

    public Optional<SuscripcionDTO> update(Long id, SuscripcionDTO dto) {
        return suscripcionRepository.findById(id).map(existente -> {
            existente.setFechaInicio(dto.getFechaInicio());
            existente.setFechaFin(dto.getFechaFin());
            if (dto.getEstado() != null) {
                existente.setEstado(dto.getEstado());
            }
            existente.setValorMensual(dto.getValorMensual());
            if (dto.getDescuentoSalas() != null) {
                existente.setDescuentoSalas(dto.getDescuentoSalas());
            }
            if (dto.getRenovacionAutomatica() != null) {
                existente.setRenovacionAutomatica(dto.getRenovacionAutomatica());
            }
            existente.setFechaCancelacion(dto.getFechaCancelacion());
            if (dto.getMesesConsecutivos() != null) {
                existente.setMesesConsecutivos(dto.getMesesConsecutivos());
            }
            existente.setUsuario(referenciaUsuario(dto.getUsuarioId()));
            existente.setPlan(referenciaPlan(dto.getPlanId()));
            return mapper.suscripcionToDto(suscripcionRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return suscripcionRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (suscripcionRepository.existsById(id)) {
            suscripcionRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<SuscripcionDTO> findByUsuarioId(Long usuarioId) {
        return suscripcionRepository.findByUsuarioId(usuarioId).stream().map(mapper::suscripcionToDto).toList();
    }

    public List<SuscripcionDTO> findByPlanId(Long planId) {
        return suscripcionRepository.findByPlanId(planId).stream().map(mapper::suscripcionToDto).toList();
    }

    public boolean existsByPlanIdAndEstadoIgnoreCase(Long planId, String estado) {
        return suscripcionRepository.existsByPlanIdAndEstadoIgnoreCase(planId, estado);
    }

    public boolean existsByUsuarioIdAndEstadoIgnoreCase(Long usuarioId, String estado) {
        return suscripcionRepository.existsByUsuarioIdAndEstadoIgnoreCase(usuarioId, estado);
    }

    public boolean existsByUsuarioIdAndEstadoIn(Long usuarioId, Collection<String> estados) {
        return suscripcionRepository.existsByUsuarioIdAndEstadoIn(usuarioId, estados);
    }

    public Optional<SuscripcionDTO> findFirstByUsuarioIdAndEstadoInAndFechaFinGreaterThanEqualOrderByFechaFinDesc(Long usuarioId, Collection<String> estados, LocalDate fecha) {
        return suscripcionRepository.findFirstByUsuarioIdAndEstadoInAndFechaFinGreaterThanEqualOrderByFechaFinDesc(usuarioId, estados, fecha).map(mapper::suscripcionToDto);
    }

    public List<SuscripcionDTO> findByEstadoIgnoreCaseAndFechaFinBefore(String estado, LocalDate fecha) {
        return suscripcionRepository.findByEstadoIgnoreCaseAndFechaFinBefore(estado, fecha).stream().map(mapper::suscripcionToDto).toList();
    }

public Optional<SuscripcionDTO> findByUsuarioIdAndEstado(Long usuarioId, String estado) {
        return suscripcionRepository.findByUsuarioId(usuarioId).stream()
                .filter(s -> estado.equalsIgnoreCase(s.getEstado()))
                .findFirst()
                .map(mapper::suscripcionToDto);
    }

    private UsuarioEntity referenciaUsuario(Long idUsuario) {
        if (idUsuario == null) {
            return null;
        }
        UsuarioEntity referencia = new UsuarioEntity();
        referencia.setId(idUsuario);
        return referencia;
    }

    private PlanSuscripcionEntity referenciaPlan(Long idPlan) {
        if (idPlan == null) {
            return null;
        }
        PlanSuscripcionEntity referencia = new PlanSuscripcionEntity();
        referencia.setId(idPlan);
        return referencia;
    }
}
