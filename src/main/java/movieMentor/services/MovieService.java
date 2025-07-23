// Written by Elishay Amar
package movieMentor.services;

import movieMentor.beans.Movie;
import java.util.List;

/**
 * Service interface for managing movie-related operations.
 * Provides methods to fetch, add, and delete movies.
 */
public interface MovieService {

    /**
     * Retrieve all movies from the database.
     * @return List of all movies.
     */
    List<Movie> getAllMovies();

    /**
     * Retrieve a movie by its unique ID.
     * @param id Movie ID.
     * @return Movie object if found.
     */
    Movie getMovieById(Long id);

    /**
     * Add a new movie to the database.
     * @param movie Movie object to save.
     * @return The saved Movie.
     */
    Movie addMovie(Movie movie);

    /**
     * Delete a movie by its ID.
     * @param id ID of the movie to delete.
     */
    void deleteMovie(Long id);
}
