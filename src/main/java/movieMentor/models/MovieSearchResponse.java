package movieMentor.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import movieMentor.beans.Actor;

import java.util.List;

@Data
@NoArgsConstructor
public class MovieSearchResponse {
    private int page;
    private List<TmdbMovie> results;
    private int total_pages;
    private int total_results;
}
