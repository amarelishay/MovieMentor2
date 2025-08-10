package movieMentor.repository;
import movieMentor.beans.MovieDTO;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieDtoRepository extends JpaRepository<MovieDTO, Long> {
}
