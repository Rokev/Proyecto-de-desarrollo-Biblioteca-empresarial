package bussinesLayer.service.impl;
import java.util.ArrayList;
import java.time.LocalDateTime;
import persistanceLayer.dao.UsuarioDAO;
import persistanceLayer.dao.PedidoInterciudadDAO;
import persistanceLayer.dao.ReservaLibroDAO;
import persistanceLayer.dao.PrestamoDAO;
import persistanceLayer.dao.NotificacionDAO;
import bussinesLayer.dto.NotificacionDTO;

import bussinesLayer.dto.SedeDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.SedeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.EjemplarDAO;
import persistanceLayer.dao.SedeDAO;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SedeServiceImpl implements SedeService {

    private final SedeDAO sedeDAO;
    private final EjemplarDAO ejemplarDAO;
    private final PrestamoDAO prestamoDAO;
    private final ReservaLibroDAO reservaLibroDAO;
    private final PedidoInterciudadDAO pedidoInterciudadDAO;
    private final NotificacionDAO notificacionDAO;
    private final UsuarioDAO usuarioDAO;

    private static final List<String> PRESTAMOS_ACTIVOS = List.of("SOLICITADO", "ACTIVO", "VENCIDO");
    private static final List<String> RESERVAS_PENDIENTES = List.of("EN_ESPERA", "NOTIFICADA");
    private static final List<String> PEDIDOS_EN_CURSO = List.of("PENDIENTE_PAGO", "SOLICITADO", "EN_TRANSITO", "ENTREGADO",
            "NO_RECIBIDO", "EN_ESPERA_DE_RECOGIDA", "DEVUELTO_PENDIENTE_TRASLADO", "EN_CAMINO_SEDE_ORIGEN");

    @Override
    public SedeDTO crearSede(SedeDTO sedeDTO) {
        log.info("Creando sede: {}", sedeDTO.getNombre());

        if (sedeDAO.existsByNombreIgnoreCase(sedeDTO.getNombre())) {
            throw new BusinessValidationException("Ya existe una sede registrada con el nombre: " + sedeDTO.getNombre());
        }
        validarSede(sedeDTO);

        if (sedeDTO.getAliada() == null) {
            sedeDTO.setAliada(false);
        }
        if (sedeDTO.getEnOperacion() == null) {
            sedeDTO.setEnOperacion(true);
        }

        SedeDTO guardada = sedeDAO.save(sedeDTO);
        log.info("Sede creada con ID: {}", guardada.getId());
        return guardada;
    }

    @Override
    @Transactional(readOnly = true)
    public SedeDTO obtenerSedePorId(Long id) {
        return sedeDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SedeDTO> obtenerTodasLasSedes() {
        return sedeDAO.findAll();
    }

    @Override
    public SedeDTO actualizarSede(Long id, SedeDTO sedeDTO) {
        if (!sedeDAO.existsById(id)) {
            throw new ResourceNotFoundException("Sede no encontrada con ID: " + id);
        }

        sedeDAO.findByNombreIgnoreCase(sedeDTO.getNombre())
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new BusinessValidationException(
                            "Ya existe otra sede registrada con el nombre: " + sedeDTO.getNombre());
                });
        validarSede(sedeDTO);

        SedeDTO actualizada = sedeDAO.update(id, sedeDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada con ID: " + id));
        log.info("Sede actualizada ID: {}", id);
        return actualizada;
    }

    @Override
    public void eliminarSede(Long id) {
        if (!sedeDAO.existsById(id)) {
            throw new ResourceNotFoundException("Sede no encontrada con ID: " + id);
        }
        if (ejemplarDAO.existsBySedeId(id)) {
            throw new BusinessValidationException("No se puede eliminar la sede porque tiene ejemplares asociados");
        }
        sedeDAO.deleteById(id);
        log.info("Sede eliminada ID: {}", id);
    }

    private void validarSede(SedeDTO dto) {
        if (dto.getHoraApertura() != null && dto.getHoraCierre() != null
                && !dto.getHoraCierre().isAfter(dto.getHoraApertura())) {
            throw new BusinessValidationException("La horaCierre debe ser posterior a la horaApertura");
        }
        if ((dto.getLatitud() == null) != (dto.getLongitud() == null)) {
            throw new BusinessValidationException("Debe indicar latitud y longitud juntas o ninguna");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SedeDTO> obtenerSedesActivas() {
        return sedeDAO.findByEnOperacionTrue();
    }

    // noRollbackFor: la notificacion al administrador debe guardarse aunque la desactivacion se bloquee.
    @Override
    @Transactional(noRollbackFor = BusinessValidationException.class)
    public SedeDTO desactivarSede(Long id, Long administradorId) {
        SedeDTO sede = sedeDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada con ID: " + id));
        if (!Boolean.TRUE.equals(sede.getEnOperacion())) {
            throw new BusinessValidationException("La sede ya esta fuera de servicio");
        }
        List<String> impedimentos = new ArrayList<>();
        long prestamos = prestamoDAO.findBySedeRetiroId(id).stream()
                .filter(p -> PRESTAMOS_ACTIVOS.contains(p.getEstado().toUpperCase())).count();
        if (prestamos > 0) {
            impedimentos.add(prestamos + " prestamo(s) activo(s)");
        }
        long reservas = reservaLibroDAO.findBySedeIdOrderByFechaReservaDesc(id).stream()
                .filter(r -> RESERVAS_PENDIENTES.contains(r.getEstado().toUpperCase())).count();
        if (reservas > 0) {
            impedimentos.add(reservas + " reserva(s) pendiente(s)");
        }
        long pedidos = pedidoInterciudadDAO.findBySedeOrigenAndEstados(id, PEDIDOS_EN_CURSO).size()
                + pedidoInterciudadDAO.findBySedeDevolucionAndEstados(id, PEDIDOS_EN_CURSO).size();
        if (pedidos > 0) {
            impedimentos.add(pedidos + " pedido(s) interciudad en curso (como sede de origen o de devolucion)");
        }
        if (!impedimentos.isEmpty()) {
            String mensaje = "No se puede desactivar la sede " + sede.getNombre() + " porque tiene "
                    + String.join(", ", impedimentos);
            if (administradorId != null && usuarioDAO.existsById(administradorId)) {
                NotificacionDTO n = new NotificacionDTO();
                n.setUsuarioId(administradorId);
                n.setTipo("SEDE_DESACTIVACION_BLOQUEADA");
                n.setTitulo("No se pudo desactivar la sede");
                n.setMensaje(mensaje);
                n.setCanal("APP");
                n.setFechaCreacion(LocalDateTime.now());
                n.setLeida(false);
                notificacionDAO.save(n);
            }
            throw new BusinessValidationException(mensaje);
        }
        sede.setEnOperacion(false);
        SedeDTO desactivada = sedeDAO.update(id, sede)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada con ID: " + id));
        log.info("Sede desactivada ID: {}", id);
        return desactivada;
    }

    @Override
    public SedeDTO reactivarSede(Long id) {
        SedeDTO sede = sedeDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada con ID: " + id));
        if (Boolean.TRUE.equals(sede.getEnOperacion())) {
            throw new BusinessValidationException("La sede ya esta activa");
        }
        sede.setEnOperacion(true);
        SedeDTO reactivada = sedeDAO.update(id, sede)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada con ID: " + id));
        log.info("Sede reactivada ID: {}", id);
        return reactivada;
    }
}
