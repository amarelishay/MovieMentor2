package movieMentor.repository;

import movieMentor.beans.Actor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActorRepository extends JpaRepository<Actor, Long> {
    Optional<Actor> findByName(String name);
    List<Actor> findAllByName(String name);
    Optional<Actor> findByTmdbId(Long tmdbId);

}
