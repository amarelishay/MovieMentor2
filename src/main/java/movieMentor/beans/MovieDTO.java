package movieMentor.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import movieMentor.models.TmdbMovie;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "movie_dto_cache")
@Entity
@Embeddable
public class MovieDTO implements Serializable {
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
        this.posterUrl = movie.getPosterUrl();
        this.voteAverage = movie.getVoteAverage();
    }

    public MovieDTO(Movie movie) {
        this.id = movie.getId();
        this.title = movie.getTitle();
        this.voteAverage = movie.getVoteAverage();
        setPosterUrl(movie.getPosterUrl());
    }

    public static MovieDTO toDTO(Movie movie) {
        return new MovieDTO(movie.getId(), movie.getTitle(), movie.getPosterUrl(), movie.getVoteAverage(), movie.getOverview());
    }

    public static List<MovieDTO> movieListToDtoList(List<Movie> movies) {
        return movies.stream()
                .map(movie -> new MovieDTO(movie.getId(), movie.getTitle(), movie.getPosterUrl(), movie.getVoteAverage(), movie.getOverview())).collect(Collectors.toList());
    }

    public static List<MovieDTO> TMDBmovieListToDtoList(List<TmdbMovie> movies) {
        return movies.stream()
                .map(movie -> new MovieDTO(movie.getId(), movie.getTitle(), movie.getPosterPath(), movie.getVoteAverage(), movie.getOverview())).collect(Collectors.toList());
    }

    public void setPosterUrl(String path) {
        String baseUrl = "https://image.tmdb.org/t/p/w500";
        this.posterUrl = baseUrl + path;
    }
}
