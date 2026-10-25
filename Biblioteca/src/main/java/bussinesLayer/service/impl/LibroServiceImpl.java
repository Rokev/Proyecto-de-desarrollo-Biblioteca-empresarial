package bussinesLayer.service.impl;

import bussinesLayer.dto.LibroDTO;
import bussinesLayer.exception.BusinessValidationException;
import bussinesLayer.exception.ResourceNotFoundException;
import bussinesLayer.service.LibroService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.dao.AutorDAO;
import persistanceLayer.dao.EjemplarDAO;
import persistanceLayer.dao.PrestamoDAO;
import persistanceLayer.dao.ReservaLibroDAO;
import persistanceLayer.dao.GeneroDAO;
import persistanceLayer.dao.LibroDAO;

import java.time.Year;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class LibroServiceImpl implements LibroService {

    private final LibroDAO libroDAO;
    private final GeneroDAO generoDAO;
    private final AutorDAO autorDAO;
    private final EjemplarDAO ejemplarDAO;
    private final PrestamoDAO prestamoDAO;
    private final ReservaLibroDAO reservaLibroDAO;

    /** Prestamos que cuentan como activos */
    private static final List<String> PRESTAMOS_ACTIVOS = List.of("SOLICITADO", "ACTIVO", "VENCIDO");
    /** Reservas de libro que cuentan como pendientes. */
    private static final List<String> RESERVAS_PENDIENTES = List.of("EN_ESPERA", "NOTIFICADA");

    @Override
    public LibroDTO crearLibro(LibroDTO libroDTO) {
        log.info("Creando libro con ISBN: {}", libroDTO.getIsbn());

        if (libroDAO.existsByIsbn(libroDTO.getIsbn())) {
            throw new BusinessValidationException(
                    "Ya existe un libro registrado con el ISBN: " + libroDTO.getIsbn());
        }
        validarReferencias(libroDTO);

        LibroDTO guardado = libroDAO.save(libroDTO);
        log.info("Libro creado con ID: {}", guardado.getId());
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public LibroDTO obtenerLibroPorId(Long id) {
        return libroDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LibroDTO> obtenerTodosLosLibros() {
        return libroDAO.findAll();
    }

    @Override
    public LibroDTO actualizarLibro(Long id, LibroDTO libroDTO) {
        if (!libroDAO.existsById(id)) {
            throw new ResourceNotFoundException("Libro no encontrado con ID: " + id);
        }

        libroDAO.findByIsbn(libroDTO.getIsbn())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new BusinessValidationException(
                            "Ya existe otro libro registrado con el ISBN: " + libroDTO.getIsbn());
                });
        validarReferencias(libroDTO);

        LibroDTO actualizado = libroDAO.update(id, libroDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con ID: " + id));
        log.info("Libro actualizado ID: {}", id);
        return actualizado;
    }

    @Override
    public void eliminarLibro(Long id) {
        if (!libroDAO.existsById(id)) {
            throw new ResourceNotFoundException("Libro no encontrado con ID: " + id);
        }
        if (prestamoDAO.existsByEjemplarLibroIdAndEstadoIn(id, PRESTAMOS_ACTIVOS)) {
            throw new BusinessValidationException("No se puede eliminar el libro porque tiene prestamos activos");
        }
        if (reservaLibroDAO.existsByLibroIdAndEstadoIn(id, RESERVAS_PENDIENTES)) {
            throw new BusinessValidationException("No se puede eliminar el libro porque tiene reservas pendientes");
        }
        if (!ejemplarDAO.findByLibroId(id).isEmpty()) {
            throw new BusinessValidationException(
                    "No se puede eliminar el libro porque todavia tiene ejemplares registrados; elimine primero los ejemplares");
        }
        if (!libroDAO.deleteById(id)) {
            throw new ResourceNotFoundException("Libro no encontrado con ID: " + id);
        }
        log.info("Libro eliminado ID: {}", id);
    }

    private void validarReferencias(LibroDTO dto) {
        if (dto.getAnioPublicacion() > Year.now().getValue()) {
            throw new BusinessValidationException("El anioPublicacion no puede ser futuro");
        }
        if (!generoDAO.existsById(dto.getGeneroId())) {
            throw new ResourceNotFoundException("Genero no encontrado con ID: " + dto.getGeneroId());
        }
        if (dto.getAutoresIds() != null) {
            for (Long autorId : dto.getAutoresIds()) {
                if (!autorDAO.existsById(autorId)) {
                    throw new ResourceNotFoundException("Autor no encontrado con ID: " + autorId);
                }
            }
        }
    }
}
