// Written by Elishay Amar
package movieMentor.services;

import lombok.RequiredArgsConstructor;
import movieMentor.beans.Movie;
import movieMentor.repository.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of MovieService interface.
 * Handles business logic related to movies.
 */
@RequiredArgsConstructor
@Service
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private static final Logger logger = LoggerFactory.getLogger(MovieServiceImpl.class);

    @Cacheable("allMovies")
    @Override
    public List<Movie> getAllMovies() {
        logger.info("Fetching all movies from database");
        return movieRepository.findAll();
    }
    @Cacheable(value = "movies", key = "#id")
    @Override
    public Movie getMovieById(Long id) {
        logger.info("Fetching movie by ID: {}", id);
        return movieRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Movie not found with ID: {}", id);
                    return new RuntimeException("Movie not found with ID: " + id);
                });
    }

    @Override
    @CacheEvict(value = "allMovies", allEntries = true)
    @CachePut(value = "movies", key = "#result.id")
    public Movie addMovie(Movie movie) {
        logger.info("Adding a new movie: {}", movie.getTitle());
        Movie savedMovie = movieRepository.save(movie);
        logger.info("Movie added successfully with ID: {}", savedMovie.getId());
        return savedMovie;
    }

    @Override
    @CacheEvict(value = "movies", key = "#id")
    public void deleteMovie(Long id) {
        logger.info("Deleting movie with ID: {}", id);
        movieRepository.deleteById(id);
        logger.info("Movie deleted successfully with ID: {}", id);
    }
}
