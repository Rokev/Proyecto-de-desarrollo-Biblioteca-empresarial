package persistanceLayer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import persistanceLayer.entity.PreguntaFrecuente;

import java.util.List;

@Repository
public interface PreguntaFrecuenteRepository extends JpaRepository<PreguntaFrecuente, Long> {

    List<PreguntaFrecuente> findByActivaTrue();

    List<PreguntaFrecuente> findByCategoriaIgnoreCaseAndActivaTrue(String categoria);

    List<PreguntaFrecuente> findByPreguntaContainingIgnoreCaseAndActivaTrue(String texto);
}
