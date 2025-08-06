package movieMentor.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import movieMentor.beans.Movie;
import movieMentor.dto.MovieDTO;
import movieMentor.services.MovieService;
import movieMentor.services.TmdbService;
import movieMentor.services.UserServiceImpl;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.logging.Logger;

@Api(value = "Movie API", description = "Operations related to movies")
@RestController
@RequestMapping("/api/movies")
public class MovieController {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private MovieService movieService;

    @Autowired
    private TmdbService tmdbService;

    // ========= DB endpoints (אם נשארים לשימוש פנימי/אדמין – עדיף גם כאן DTO כדי להימנע מ-LAZY) =========

    @ApiOperation(value = "Get all movies from database")
    @GetMapping
    public List<MovieDTO> getAllMovies() {
        List<Movie> movies = movieService.getAllMovies();
        return MovieDTO.toDTOlist(movies);
    }

    @ApiOperation(value = "Add a new movie to the database")
    @PostMapping
    public MovieDTO addMovie(@RequestBody Movie movie) {
        Movie saved = movieService.addMovie(movie);
        return MovieDTO.toDTO(saved);
    }

    // ========= TMDB/search =========

    @ApiOperation(value = "Search for movies via TMDB API")
    @GetMapping("/search")
    @Cacheable(value = "search", key = "#query + ':' + (#page != null ? #page : 1)")
    public List<MovieDTO> searchMovies(@RequestParam String query, @RequestParam(required = false, defaultValue = "1") int page) {
        List<MovieDTO> movies=MovieDTO.toDTOlist(tmdbService.searchMovies(query));
        // אם ה-service תומך בעמודים, תעביר page; אם לא – השאר כך
        logger.info("found movies ({}): [{}]", movies);
        return movies;
    }

    @ApiOperation(value = "Get now-playing movies from TMDB")
    @GetMapping("/now-playing")
    @Cacheable(value = "nowPlaying", key = "#page")
    public List<MovieDTO> getNowPlaying(@RequestParam(required = false, defaultValue = "1") int page) {
        List<Movie> movies = tmdbService.getNowPlayingMovies();
        return MovieDTO.toDTOlist(movies);
    }

    @ApiOperation(value = "Get upcoming movies from TMDB")
    @GetMapping("/upcoming")
    @Cacheable(value = "upcoming", key = "#page")
    public List<MovieDTO> getUpcoming(@RequestParam(required = false, defaultValue = "1") int page) {
        return MovieDTO.toDTOlist(tmdbService.getUpComingMovies());
    }

    @ApiOperation(value = "Get top-rated movies from TMDB")
    @GetMapping("/top-rated")
    @Cacheable(value = "topRated", key = "#page")
    public List<MovieDTO> getTopRated(@RequestParam(required = false, defaultValue = "1") int page) {
        return MovieDTO.toDTOlist(tmdbService.getTopRatedMovies());
    }

    @ApiOperation(value = "Get movies by genre from TMDB")
    @GetMapping("/by-genre")
    @Cacheable(value = "byGenre", key = "#genreId + ':' + #page")
    public List<MovieDTO> getMoviesByGenre(@RequestParam int genreId,
                                           @RequestParam(required = false, defaultValue = "1") int page) {
        return MovieDTO.toDTOlist(tmdbService.getMoviesByGenre(genreId, page));
    }


    @ApiOperation(value = "Get movie by id from TMDB")
    @GetMapping("/{id}")
    public Movie getMovieById(@PathVariable long id) {
        Movie movie=tmdbService.getOrCreateMovieById(id);
        logger.info("fetch movie ({}): [{}]", movie);
        return movie;
    }
}
