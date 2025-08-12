/**
 * Movie Entity - Created by Elishay Amar
 *
 * Represents a movie fetched from TMDB, including basic details, list of images, and trailer URL.
 */

package movieMentor.beans;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import movieMentor.models.MovieImage;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie implements Serializable {
    @EqualsAndHashCode.Include
    @Id
    private Long id;

    private String title;
    private String originalTitle;
    @Column(columnDefinition = "TEXT",length = 2000)
    private String overview;

    private String posterUrl;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate releaseDate;


    private Double popularity;

    private Double voteAverage;

    private Integer voteCount;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "movie_images", joinColumns = @JoinColumn(name = "movie_id"))
    @Column(name = "image_url")
    private List<MovieImage> imageUrls;


    private String trailerUrl;

    @Singular
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "movie_actor",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "actor_id"))
    private Set<Actor> actors = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinTable(
            name = "movie_genres",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
