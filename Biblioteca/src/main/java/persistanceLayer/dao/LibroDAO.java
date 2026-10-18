package persistanceLayer.dao;

import bussinesLayer.dto.LibroDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import persistanceLayer.entity.AutorEntity;
import persistanceLayer.entity.GeneroEntity;
import persistanceLayer.entity.LibroEntity;
import persistanceLayer.mapper.Mapper;
import persistanceLayer.repository.LibroRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Acceso a datos de Libro: recibe y devuelve DTOs, la capa de negocio nunca ve la entidad.
 */
@Repository
@RequiredArgsConstructor
@Transactional
public class LibroDAO {

    private final LibroRepository libroRepository;
    private final Mapper mapper;

    public LibroDTO save(LibroDTO dto) {
        LibroEntity entity = mapper.libroToEntity(dto);
        entity.setId(null);
        entity.setGenero(referenciaGenero(dto.getGeneroId()));
        entity.setAutores(referenciasAutores(dto.getAutoresIds()));
        LibroEntity guardado = libroRepository.save(entity);
        return mapper.libroToDto(guardado);
    }

    public Optional<LibroDTO> findById(Long id) {
        return libroRepository.findById(id).map(mapper::libroToDto);
    }

    public List<LibroDTO> findAll() {
        return libroRepository.findAll().stream().map(mapper::libroToDto).toList();
    }

    public Optional<LibroDTO> update(Long id, LibroDTO dto) {
        return libroRepository.findById(id).map(existente -> {
            existente.setIsbn(dto.getIsbn());
            existente.setTitulo(dto.getTitulo());
            existente.setIdioma(dto.getIdioma());
            existente.setAnioPublicacion(dto.getAnioPublicacion());
            existente.setEditorial(dto.getEditorial());
            existente.setSinopsis(dto.getSinopsis());
            existente.setNumeroPaginas(dto.getNumeroPaginas());
            existente.setPortadaUrl(dto.getPortadaUrl());
            existente.setGenero(referenciaGenero(dto.getGeneroId()));
            if (dto.getAutoresIds() != null) {
                existente.setAutores(referenciasAutores(dto.getAutoresIds()));
            }
            return mapper.libroToDto(libroRepository.save(existente));
        });
    }

    public boolean existsById(Long id) {
        return libroRepository.existsById(id);
    }

    public boolean deleteById(Long id) {
        if (libroRepository.existsById(id)) {
            libroRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<LibroDTO> findByIsbn(String isbn) {
        return libroRepository.findByIsbn(isbn).map(mapper::libroToDto);
    }

    public boolean existsByIsbn(String isbn) {
        return libroRepository.existsByIsbn(isbn);
    }

    public List<LibroDTO> findByTituloContainingIgnoreCase(String titulo) {
        return libroRepository.findByTituloContainingIgnoreCase(titulo).stream().map(mapper::libroToDto).toList();
    }

    public List<LibroDTO> findByIdiomaIgnoreCase(String idioma) {
        return libroRepository.findByIdiomaIgnoreCase(idioma).stream().map(mapper::libroToDto).toList();
    }

    public List<LibroDTO> findByAnioPublicacion(Integer anioPublicacion) {
        return libroRepository.findByAnioPublicacion(anioPublicacion).stream().map(mapper::libroToDto).toList();
    }

    public List<LibroDTO> findByGeneroId(Long generoId) {
        return libroRepository.findByGeneroId(generoId).stream().map(mapper::libroToDto).toList();
    }

    public List<LibroDTO> findByAutoresId(Long autorId) {
        return libroRepository.findByAutoresId(autorId).stream().map(mapper::libroToDto).toList();
    }

    public List<LibroDTO> findDistinctByAutoresNombreContainingIgnoreCase(String nombreAutor) {
        return libroRepository.findDistinctByAutoresNombreContainingIgnoreCase(nombreAutor).stream().map(mapper::libroToDto).toList();
    }

    public List<LibroDTO> buscar(String titulo, String autor, String idioma, Integer anio, Long generoId) {
        return libroRepository.buscar(titulo, autor, idioma, anio, generoId).stream().map(mapper::libroToDto).toList();
    }

    private GeneroEntity referenciaGenero(Long idGenero) {
        if (idGenero == null) {
            return null;
        }
        GeneroEntity referencia = new GeneroEntity();
        referencia.setId(idGenero);
        return referencia;
    }

    private List<AutorEntity> referenciasAutores(List<Long> ids) {
        if (ids == null) {
            return new ArrayList<>();
        }
        return ids.stream().distinct().map(id -> {
            AutorEntity referencia = new AutorEntity();
            referencia.setId(id);
            return referencia;
        }).collect(Collectors.toCollection(ArrayList::new));
    }
}
