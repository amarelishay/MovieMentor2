package movieMentor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import movieMentor.beans.Movie;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieDTO {
    private Long id;
    private String title;
    private String posterUrl;
    private Double voteAverage;

    public MovieDTO(MovieDTO movie) {
        this.id = movie.getId();
        this.title = movie.getTitle();
        this.posterUrl = movie.getPosterUrl();
        this.voteAverage = movie.getVoteAverage();
    }

    public static MovieDTO toDTO(Movie movie) {
        return new MovieDTO(movie.getId(), movie.getTitle(), movie.getPosterUrl(), movie.getVoteAverage());
    }
    public static List<MovieDTO> toDTOlist(List<Movie> movies) {
        return movies.stream()
                .map(movie -> new MovieDTO(movie.getId(), movie.getTitle(), movie.getPosterUrl(), movie.getVoteAverage())).collect(Collectors.toList());
    }
    public MovieDTO(Movie movie) {
        this.id = movie.getId();
        this.title = movie.getTitle();
        this.posterUrl = movie.getPosterUrl();
        this.voteAverage = movie.getVoteAverage();
    }
}
