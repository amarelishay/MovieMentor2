package movieMentor.controllers;

import movieMentor.beans.Movie;
import movieMentor.services.MovieService;
import movieMentor.services.TmdbService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(value = "Movie API", description = "Operations related to movies")
@RestController
@RequestMapping("/api/movies")
public class MovieController {

    @Autowired
    private MovieService movieService;

    @Autowired
    private TmdbService tmdbService;

    @ApiOperation(value = "Get all movies from database")
    @GetMapping
    public ResponseEntity<List<Movie>> getAllMovies() {
        return ResponseEntity.ok(movieService.getAllMovies());
    }

    @ApiOperation(value = "Add a new movie to the database")
    @PostMapping
    public ResponseEntity<Movie> addMovie(@RequestBody Movie movie) {
        Movie saved = movieService.addMovie(movie);
        return ResponseEntity.ok(saved);
    }

    @ApiOperation(value = "Search for movies via TMDB API")
    @GetMapping("/search")
    public ResponseEntity<List<Movie>> searchMovies(@RequestParam String query) {
        return ResponseEntity.ok(tmdbService.searchMovies(query));
    }

    @ApiOperation(value = "Get now-playing movies from TMDB")
    @GetMapping("/now-playing")
    public ResponseEntity<List<Movie>> getNowPlaying() {
        return ResponseEntity.ok(tmdbService.getNowPlayingMovies());
    }
    @ApiOperation(value = "Get uppcomming movies from TMDB")
    @GetMapping("/upcoming")
    public ResponseEntity<List<Movie>> getUpcoming() {
        return ResponseEntity.ok(tmdbService.getUpComingMovies());
    }
    @ApiOperation(value = "Get top-rated movies from TMDB")
    @GetMapping("/top-rated")
    public ResponseEntity<List<Movie>> getTopRated() {
        return ResponseEntity.ok(tmdbService.getTopRatedMovies());
    }

    @ApiOperation(value = "Get  movies by genre from TMDB")
    @GetMapping("/by-genre")
    public ResponseEntity<List<Movie>> getMoviesByGenre(int genreId,int page) {
        return ResponseEntity.ok(tmdbService.getMoviesByGenre(genreId,page));
    }
}
