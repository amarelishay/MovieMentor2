package movieMentor.repository;

import movieMentor.beans.Movie;
import movieMentor.beans.User;
import movieMentor.beans.UserWatchEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserWatchEntryRepository extends JpaRepository<UserWatchEntry, Long> {
    List<UserWatchEntry> findTop100ByUserOrderByWatchedAtDesc(User user);
    boolean existsByUserAndMovie(User user, Movie movie);

    boolean existsByUserAndMovieId(User user, Long id);
}

