package movieMentor.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import movieMentor.models.TmdbMovie;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "movie_dto_cache")
@Entity
@Embeddable
@Builder
public class MovieDTO implements java.io.Serializable {

    @Id
    private Long id;
    private String title;
    private String posterUrl;
    private Double voteAverage;
    @Column(columnDefinition = "TEXT",length = 2000)
    private String overview;

    public MovieDTO(MovieDTO movie) {
        this.id = movie.getId();
        this.title = movie.getTitle();
        this.voteAverage = movie.getVoteAverage();
        setPosterUrl(movie.getPosterUrl());
    }

    public MovieDTO(String title, String posterUrl, Double voteAverage, String overview) {
        this.title = title;
        setPosterUrl(posterUrl);
        this.voteAverage = voteAverage;
        this.overview = overview;
    }

    public MovieDTO(Movie movie) {
        this.id = movie.getId();
        this.title = movie.getTitle();
        this.voteAverage = movie.getVoteAverage();
        setPosterUrl(movie.getPosterUrl());
    }

    public MovieDTO(Long tmdbId, String title) {
        this.id=tmdbId;
        this.title=title;
    }

    public static MovieDTO toDTO(Movie movie) {
        return new MovieDTO(movie.getId(), movie.getTitle(), movie.getPosterUrl(), movie.getVoteAverage(), movie.getOverview());
    }

    public static List<MovieDTO> movieListToDtoList(List<Movie> movies) {
        return movies.stream()
                .map(movie -> new MovieDTO(movie.getId(), movie.getTitle(), movie.getPosterUrl(), movie.getVoteAverage(), movie.getOverview())).collect(Collectors.toList());
    }

    public static List<MovieDTO> TMDBmovieListToDtoList(List<TmdbMovie> movies) {
        List<MovieDTO> movieDTOS= new ArrayList<>();
        for (TmdbMovie movie:movies) {
            MovieDTO movieDTO=MovieDTO.builder().id(movie.getId())
                    .overview(movie.getOverview())
                    .title(movie.getTitle())
                    .build();
            movieDTO.setPosterUrl(movie.getPosterPath());
            movieDTOS.add(movieDTO);
        }
        return movieDTOS;
    }
    public static List<Movie> DtoMovieListToMovieList(List<MovieDTO> dtoMovies) {
        List<Movie> movies=new ArrayList<>();
        for (MovieDTO dto:dtoMovies) {
            movies.add(Movie.builder().id(dto.id).originalTitle(dto.getTitle()).overview(dto.getOverview()).posterUrl(dto.getPosterUrl()).build());
        }
        return movies;
    }
    public static List<MovieDTO> userWatchToMovieDTOList(List<UserWatchEntry> entries) {
        if (entries == null) return new ArrayList<>();
        return entries.stream()
                .map(UserWatchEntry::getMovie)   // כי movie הוא @Embedded MovieDTO
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ממיר: List<UserWatchEntry> -> List<Movie> (לשימוש במקומות שדורשים Movie)
    public static List<Movie> userWatchToMovieList(List<UserWatchEntry> entries) {
        return DtoMovieListToMovieList(userWatchToMovieDTOList(entries));
    }
    public static MovieDTO UserWatchEntrytoMovieDTO(UserWatchEntry entry) {
        return entry.getMovieSnapshot();
    }
    public static List<MovieDTO> UserWatchtoMovieDTOList(List<UserWatchEntry> entries) {
       List<MovieDTO> movieDTOS=new ArrayList<>();
        for (UserWatchEntry userWatchEntry:entries) {
            movieDTOS.add(UserWatchEntrytoMovieDTO(userWatchEntry));
        }
        return movieDTOS;
    }

    public void setPosterUrl(String path) {
        String baseUrl = "https://image.tmdb.org/t/p/w500";
        this.posterUrl = baseUrl + path;
    }
}
