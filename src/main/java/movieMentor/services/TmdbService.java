package movieMentor.services;

import movieMentor.beans.Actor;
import movieMentor.beans.Genre;
import movieMentor.beans.Movie;
import movieMentor.dto.MovieDTO;
import movieMentor.models.MovieImage;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TmdbService {

    /**
     * Search for movies by a textual query from the TMDB API.
     *
     * @param query the search string
     * @return a list of Movie entities with basic details and actors
     */
    List<Movie> searchMovies(String query);

//    List<MovieDTO> searchMoviesDtos(String query);

    /**
     * Get or create a movie by its title.
     * If the movie is not in the database, fetch it from TMDB and save it with its actors.
     *
     * @param title the movie title
     * @return a fully populated Movie entity
     */
    Movie getOrCreateMovie(String title);


    Movie getOrCreateMovieById(long id);

    /**
     * Fetch a list of now playing movies in Israel region.
     *
     * @return a list of now playing movies
     */
    List<Movie> getNowPlayingMovies();

    List<Movie> getUpComingMovies();

    /**
     * Fetch a list of top rated movies from TMDB.
     *
     * @return a list of top rated movies
     */
    List<Movie> getTopRatedMovies();

    /**
     * Update a user's recommended movies by replacing only the changed ones.
     *
     * @param oldList the existing list of Movie entities
     * @param newTitles the updated list of movie titles from recommendation
     * @return a new updated list of Movie entities
     */
    List<Movie> updateMovieListWithDifferences(List<Movie> oldList, List<String> newTitles);

    /**
     * Fetch the trailer YouTube embed link of a given TMDB movie ID.
     *
     * @param movieId the TMDB movie ID
     * @return the YouTube embed URL as a String
     */
    String fetchTrailerUrl(Long movieId);

    /**
     * Fetch up to 10 backdrop image paths for the movie (type = "backdrop").
     *
     * @param movieId the TMDB movie ID
     * @return list of full image URLs
     */
    List<MovieImage> fetchImagesForMovie(Long movieId);


    /**
     * Get up to 10 actors associated with the movie.
     *
     * @param movieId the TMDB movie ID
     * @return a list of Actor objects
     */
    List<Actor> getActorsForMovie(Long movieId);
     Set<Genre> resolveGenres(List<Integer> genreIds);

    List<Movie> getMoviesByGenre(int genreId,int page);

}
