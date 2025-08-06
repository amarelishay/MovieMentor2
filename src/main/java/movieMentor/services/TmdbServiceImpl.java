package movieMentor.services;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import movieMentor.beans.Actor;
import movieMentor.beans.Genre;
import movieMentor.beans.Movie;
import movieMentor.enums.GenreEnum;
import movieMentor.models.MovieImage;
import movieMentor.models.MovieSearchResponse;
import movieMentor.models.TmdbMovie;
import movieMentor.repository.ActorRepository;
import movieMentor.repository.GenreRepository;
import movieMentor.repository.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Transactional
@Service
@RequiredArgsConstructor
public class TmdbServiceImpl implements TmdbService {
    private static final Logger logger = LoggerFactory.getLogger(TmdbServiceImpl.class);
    private final GenreRepository genreRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final MovieRepository movieRepository;
    private final ActorRepository actorRepository;
    private final EmbeddingStorageService embeddingStorageService;
    private final EmbeddingService embeddingService;
    @Value("${tmdb.api.token}")
    private String apiToken;
    @Value("${tmdb.api.base-url}")
    private String apiBaseUrl;

    @Override
    public List<Movie> searchMovies(String query) {
        List<Movie> localResults = movieRepository.findByTitleContainingIgnoreCase(query);

        // תמיד נחזיר גם את מה שכבר יש
        List<Movie> results = new ArrayList<>(localResults);

        // נמשוך רק אם אין תוצאה מדויקת במסד (ולא סתם חלקי)
        boolean hasExactMatch = localResults.stream()
                .anyMatch(m -> m.getTitle().equalsIgnoreCase(query));

        if (!hasExactMatch) {
            // נמשוך וניתן למטודה הקיימת לטפל בשמירה
            Movie fetched = getOrCreateMovie(query);
            if (fetched != null && localResults.stream().noneMatch(m -> m.getId().equals(fetched.getId()))) {
                results.add(fetched);
            }
        }

        return results;
    }


    @Override
    public Movie getOrCreateMovie(String title) {
        return movieRepository.findAllByTitle(title).stream()
                .findFirst()
                .orElseGet(() -> {
                    List<Movie> movies = searchMovies(title);
                    if (movies.isEmpty()) {
                        logger.info("Movie not found in TMDB: {}", title);
                    }
                    return saveMovie(movies.get(0));
                });
    }


    @Override
    public synchronized Movie getOrCreateMovieById(long id) {
        return movieRepository.findById(id)
                .orElseGet(() -> {
                    String url = apiBaseUrl + "/movie/" + id + "?append_to_response=credits,images,videos";
                    try {
                        ResponseEntity<TmdbMovie> response = restTemplate.exchange(
                                url,
                                HttpMethod.GET,
                                createRequestEntity(),
                                TmdbMovie.class
                        );

                        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                            TmdbMovie tmdbMovie = response.getBody();
                            Movie movie = buildMovieFromTmdb(tmdbMovie);
                            return saveMovie(movie);
                        }
                    } catch (Exception e) {
                        logger.error("❌ Failed to fetch movie by ID {}: {}", id, e.getMessage());
                    }

                    throw new RuntimeException("Movie not found in TMDB: " + id);
                });
    }


    @Override
    @Cacheable("nowPlaying")
    public List<Movie> getNowPlayingMovies() {
        String url = apiBaseUrl + "/movie/now_playing?language=en-US&page=1";
        List<Movie> fetchedMovies = fetchMoviesFromTmdb(url);

        if (fetchedMovies == null || fetchedMovies.isEmpty()) {
            logger.warn("No movies fetched from TMDB now playing endpoint.");
            return Collections.emptyList();
        }

        List<Movie> savedMovies = new ArrayList<>();
        for (Movie movie : fetchedMovies) {
            try {
                Movie saved = saveMovie(movie); // שימוש במטודה שלך ששומרת סרט אחד
                savedMovies.add(saved);
            } catch (Exception e) {
                logger.warn("Failed to save movie with ID {}: {}", movie.getId(), e.getMessage());
            }
        }

        return savedMovies;
    }


    @Cacheable(value = "upComing")
    @Override
    public List<Movie> getUpComingMovies() {
        String url = apiBaseUrl + "/movie/upcoming?region=IL";
        return fetchMovieListFromTmdbJson(url);
    }

    @Override
    public List<Movie> getTopRatedMovies() {
        String url = apiBaseUrl + "/movie/top_rated?language=en-US&page=1";
        return fetchMovieListFromTmdbJson(url);
    }

    @Override
    public List<Movie> updateMovieListWithDifferences(List<Movie> oldList, List<String> newTitles) {
        List<String> currentTitles = oldList.stream()
                .map(Movie::getTitle)
                .collect(Collectors.toList());

        List<Movie> updatedList = new ArrayList<>();

        int index = 0;

        for (String newTitle : newTitles) {
            if (currentTitles.contains(newTitle)) {
                // שמור את הסרט הקיים (לפי אותו כותר)
                Movie movie = oldList.stream()
                        .filter(m -> m.getTitle().equals(newTitle))
                        .findFirst()
                        .orElseThrow();
                updatedList.add(movie);
            } else if (index < oldList.size()) {
                // החלף סרט שאינו קיים ברשימה החדשה
                Movie movie = getOrCreateMovie(newTitle);
                updatedList.add(movie);
            } else {
                // רשימה חדשה ארוכה יותר – הוסף
                updatedList.add(getOrCreateMovie(newTitle));
            }
            index++;
        }

        return updatedList;
    }


    @Override
    public String fetchTrailerUrl(Long movieId) {
        String url = apiBaseUrl + "/movie/" + movieId + "/videos?language=en-US";
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, createRequestEntity(), JsonNode.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                for (JsonNode video : response.getBody().get("results")) {
                    if ("Trailer".equals(video.get("type").asText()) && "YouTube".equals(video.get("site").asText())) {
                        return "https://www.youtube.com/embed/" + video.get("key").asText();
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch trailer for movieId {}: {}", movieId, e.getMessage());
        }
        return "";
    }

    @Override
    public List<MovieImage> fetchImagesForMovie(Long movieId) {
        String url = apiBaseUrl + "/movie/" + movieId + "/images";
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, createRequestEntity(), JsonNode.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<MovieImage> images = new ArrayList<>();
                Map<String, JsonNode> sections = Map.of(
                        "backdrop", response.getBody().get("backdrops"),
                        "poster", response.getBody().get("posters"),
                        "logo", response.getBody().get("logos")
                );

                sections.forEach((type, arrayNode) -> {
                    if (arrayNode != null && arrayNode.isArray()) {
                        int count = 0;
                        for (JsonNode node : arrayNode) {
                            if (count >= 10) break;
                            if (node.hasNonNull("file_path")) {
                                String path = node.get("file_path").asText();
                                images.add(MovieImage.builder()
                                        .type(type)
                                        .url("https://image.tmdb.org/t/p/w780" + path)
                                        .build());
                                count++;
                            }
                        }
                    }
                });

                return images;
            }
        } catch (Exception e) {
            logger.warn("❌ Failed to fetch images for movieId {}: {}", movieId, e.getMessage());
        }
        return Collections.emptyList();
    }


    @Override
    public List<Actor> getActorsForMovie(Long movieId) {
        String url = apiBaseUrl + "/movie/" + movieId + "/credits";
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, createRequestEntity(), JsonNode.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Actor> actors = new ArrayList<>();
                JsonNode cast = response.getBody().get("cast");
                for (int i = 0; i < Math.min(24, cast.size()); i++) {
                    JsonNode actorNode = cast.get(i);
                    String name = actorNode.get("name").asText();
                    String imagePath = actorNode.hasNonNull("profile_path") ? actorNode.get("profile_path").asText() : null;

                    Long tmdbId = actorNode.get("id").asLong();
                    Long id = actorNode.get("id").asLong();

                    Actor actor = actorRepository.findByTmdbId(tmdbId).orElseGet(() -> {
                        Actor newActor = new Actor();
                        newActor.setId(id);
//                        newActor.setTmdbId(tmdbId);
                        newActor.setName(name);
                        newActor.setImageUrl(imagePath != null ? "https://image.tmdb.org/t/p/w500" + imagePath : null);
                        return actorRepository.saveAndFlush(newActor);
                    });

                    actors.add(actor);
                }
                return actors;
            }
        } catch (Exception e) {
            logger.warn("Error fetching actors for movieId {}: {}", movieId, e.getMessage());
        }
        return Collections.emptyList();
    }

    @Override
    public Set<Genre> resolveGenres(List<Integer> genreIds) {
        Set<Genre> genres = new HashSet<>();
        for (Integer generId : genreIds) {
            Genre genre = new Genre((generId.longValue()), GenreEnum.fromId(generId));
            genres.add(genre);
            if (!genreRepository.existsById(generId.longValue())) {
                genreRepository.saveAndFlush(genre);
            }
        }
        return genres;
    }

    @Cacheable(value = "byGenre", key = "#genreId + '-' + #page")
    @Override
    public List<Movie> getMoviesByGenre(int genreId, int page) {
        String url = apiBaseUrl +
                "/discover/movie?language=en-US&with_genres=" + genreId +
                "&sort_by=popularity.desc&page=" + page;

        return fetchMovieListFromTmdbJson(url);
    }

    // ------- Private Helpers -------
    @Transactional
    public Movie saveMovie(Movie movie) {
        return movieRepository.saveAndFlush(movie);
    }
    private List<Movie> fetchMoviesFromTmdb(String url) {
        try {
            ResponseEntity<MovieSearchResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, createRequestEntity(), MovieSearchResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<TmdbMovie> results = response.getBody().getResults();
                if (results == null || results.isEmpty()) {
                    return Collections.emptyList();
                }

                return results.stream()
                        .map(tmdbMovie -> movieRepository.findById(tmdbMovie.getId())
                                .orElseGet(() -> buildMovieFromTmdb(tmdbMovie)))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            logger.error("Error fetching movies from TMDB: {}", e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    private List<Movie> fetchMovieListFromTmdbJson(String url) {
        ResponseEntity<MovieSearchResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                createRequestEntity(),
                MovieSearchResponse.class
        );

        return response.getBody().getResults().stream()
                .map(this::buildMovieFromTmdb)
                .collect(Collectors.toList());
    }


    public List<Movie> prepareCandidateMovies() {
        List<Movie> candidates = new ArrayList<>();
        candidates.addAll(getTopRatedMovies());
        candidates.addAll(getNowPlayingMovies());

        for (Movie movie : candidates) {
            Long id = movie.getId();
            if (!embeddingStorageService.hasEmbedding(id)) {
                String overview = movie.getOverview();
                if (overview != null && !overview.isBlank()) {
                    float[] vector = embeddingService.getEmbedding(overview);
                    embeddingStorageService.addEmbedding(id, vector);
                    logger.info("🧠 Embedded candidate movie '{}'", movie.getTitle());
                }
            }
        }

        return candidates;
    }

    private Movie buildMovieFromTmdb(TmdbMovie tmdbMovie) {
        return Movie.builder()
                .id(tmdbMovie.getId())
                .title(tmdbMovie.getTitle())
                .originalTitle(tmdbMovie.getOriginalTitle())
                .overview(tmdbMovie.getOverview())
                .posterUrl("https://image.tmdb.org/t/p/w500" + tmdbMovie.getPosterPath())
                .releaseDate(parseDate(tmdbMovie.getReleaseDate()))
                .popularity(tmdbMovie.getPopularity())
                .voteAverage(tmdbMovie.getVoteAverage())
                .voteCount(tmdbMovie.getVoteCount())
                .trailerUrl(fetchTrailerUrl(tmdbMovie.getId()))
                .imageUrls(fetchImagesForMovie(tmdbMovie.getId()))
                .actors(getActorsForMovie(tmdbMovie.getId()))
                .genres(resolveGenres(tmdbMovie.getGenreIds()))
                .build();
    }


    private LocalDate parseDate(String dateStr) {
        try {
            return (dateStr == null || dateStr.isBlank()) ? null : LocalDate.parse(dateStr);
        } catch (Exception e) {
            logger.warn("Failed to parse date: {}", dateStr);
            return null;
        }
    }

    private HttpEntity<String> createRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiToken);
        headers.set("Accept", "application/json");
        return new HttpEntity<>(headers);
    }
}
