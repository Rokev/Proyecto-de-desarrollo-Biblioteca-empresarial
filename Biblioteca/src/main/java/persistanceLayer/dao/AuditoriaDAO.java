package persistanceLayer.dao;

import bussinesLayer.dto.AuditoriaDTO;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.AuditoriaEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.AuditoriaRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos de Auditoría: recibe y devuelve DTOs. El log es solo de lectura/inserción:
 * no hay update ni delete a propósito.
 */
@Repository
@RequiredArgsConstructor
public class AuditoriaDAO {

    private final AuditoriaRepository auditoriaRepository;
    private final Mapper mapper;

    public AuditoriaDTO save(AuditoriaDTO dto) {
        AuditoriaEntity entity = mapper.auditoriaToEntity(dto);
        entity.setId(null);
        return mapper.auditoriaToDto(auditoriaRepository.save(entity));
    }

    public Optional<AuditoriaDTO> findById(Long id) {
        return auditoriaRepository.findById(id).map(mapper::auditoriaToDto);
    }

    /** Resultado de una consulta paginada: la página y el total de registros que cumplen el filtro. */
    public record Pagina(List<AuditoriaDTO> contenido, long total) {
    }

    /**
     * Busca registros combinando solo los filtros que no sean nulos, del más reciente al más antiguo.
     * {@code desde} es inclusivo y {@code hasta} exclusivo (el servicio ya lo convierte a inicio del día siguiente).
     */
    public Pagina buscar(Long usuarioId, LocalDateTime desde, LocalDateTime hasta,
                         String accion, String modulo, int pagina, int tamanio) {
        Specification<AuditoriaEntity> spec = (root, query, cb) -> {
            List<Predicate> filtros = new ArrayList<>();
            if (usuarioId != null) {
                filtros.add(cb.equal(root.get("usuarioId"), usuarioId));
            }
            if (desde != null) {
                filtros.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("fecha"), desde));
            }
            if (hasta != null) {
                filtros.add(cb.lessThan(root.<LocalDateTime>get("fecha"), hasta));
            }
            if (accion != null) {
                filtros.add(cb.equal(cb.upper(root.<String>get("accion")), accion.toUpperCase()));
            }
            if (modulo != null) {
                filtros.add(cb.equal(cb.upper(root.<String>get("modulo")), modulo.toUpperCase()));
            }
            return cb.and(filtros.toArray(new Predicate[0]));
        };
        Page<AuditoriaEntity> resultado = auditoriaRepository.findAll(spec,
                PageRequest.of(pagina, tamanio, Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id"))));
        return new Pagina(resultado.getContent().stream().map(mapper::auditoriaToDto).toList(),
                resultado.getTotalElements());
    }
}