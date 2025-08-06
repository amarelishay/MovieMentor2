package movieMentor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TmdbSearchResponse {
    private Long id;
    private String title;
    private String poster_path;
    private Double vote_average;
    private List<MovieDTO> results;

}
