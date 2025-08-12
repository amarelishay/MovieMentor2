package movieMentor.services;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import movieMentor.beans.Actor;
import movieMentor.beans.Genre;
import movieMentor.beans.Movie;
import movieMentor.beans.MovieDTO;
import movieMentor.enums.GenreEnum;
import movieMentor.models.MovieImage;
import movieMentor.models.MovieSearchResponse;
import movieMentor.models.TmdbMovie;
import movieMentor.repository.ActorRepository;
import movieMentor.repository.GenreRepository;
import movieMentor.repository.MovieDtoRepository;
import movieMentor.repository.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
    private final MovieDtoRepository movieDtoRepository;
    private static final int DB_OVERVIEW_MAX = 255;

    @Autowired
    @Qualifier("redisTemplate")
    private final RedisTemplate<String, Object> redisTemplate;
    @Value("${tmdb.api.token}")
    private String apiToken;
    @Value("${tmdb.api.base-url}")
    private String apiBaseUrl;

    @Override
    public List<Movie> searchMovies(String query) {
        String url = apiBaseUrl + "/search/movie?page=1&query=" + UriUtils.encode(query, StandardCharsets.UTF_8);
        return fetchMoviesFromTmdb(url);
    }

    @Override
    public List<Movie> searchMoviesDTO(String query) {
        String url = apiBaseUrl + "/search/movie?page=1&query=" + UriUtils.encode(query, StandardCharsets.UTF_8);
        return fetchMoviesFromTmdb(url);
    }

    @Override
    public Movie getOrCreateMovie(String title) {
        return movieRepository.findAllByTitle(title).stream()
                .findFirst()
                .orElseGet(() -> {
                    List<Movie> movies = searchMovies(title);
                    if (movies.isEmpty()) {
                        throw new RuntimeException("Movie not found in TMDB: " + title);
                    }
                    return movieRepository.saveAndFlush(movies.get(0));
                });
    }

    //    @Override
//    public MovieDTO getOrCreateMovieDTO(String title) {
//        String cacheKey = "movieDTO:title:" + title.toLowerCase();
//
//        try {
//            MovieDTO cached = (MovieDTO) redisTemplate.opsForValue().get(cacheKey);
//            if (cached != null) {
//                logger.info("✅ Found MovieDTO in Redis for title: {}", title);
//                return cached;
//            }
//        } catch (Exception e) {
//            logger.error("❌ Redis error while checking cache for title {}: {}", title, e.getMessage());
//        }
//
//        try {
//            List<MovieDTO> searchResults = MovieDTO.movieListToDtoList(searchMovies(title));
//
//            if (searchResults.isEmpty()) {
//                logger.warn("❌ No results found in TMDB for title: {}", title);
//                throw new RuntimeException("Movie not found: " + title);
//            }
//
//            MovieDTO movieDTO = searchResults.get(0); // שימוש ישיר בתוצאה
//            redisTemplate.opsForValue().set(cacheKey, movieDTO, Duration.ofHours(6));
//            logger.info("✅ Cached MovieDTO in Redis for title: {}", title);
//
//            return movieDTO;
//
//        } catch (Exception e) {
//            logger.error("❌ Error fetching MovieDTO for title {}: {}", title, e.getMessage());
//            throw new RuntimeException("Failed to get or create MovieDTO: " + title);
//        }
//    }
    @Transactional
    @Override
    public MovieDTO getOrCreateMovieDTO(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("title is required");
        }
        String normalized = title.trim();
        String cacheKey = "movieDTO:title:" + normalized.toLowerCase();

        // קאש → החזר אם יש
        try {
            MovieDTO cached = (MovieDTO) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                logger.info("✅ Found MovieDTO in Redis for title: {}", normalized);
                return cached; // ⬅️ ללא שמירה ל-DB (מטרה: לא לשמור כל סרט)
            }
        } catch (Exception e) {
            logger.error("❌ Redis error while checking cache for title {}: {}", normalized, e.getMessage());
        }

        // TMDB → המרה ל-DTO → קאש → החזרה (ללא DB)
        try {
            List<MovieDTO> searchResults = MovieDTO.movieListToDtoList(searchMovies(normalized));
            if (searchResults == null || searchResults.isEmpty()) {
                logger.warn("❌ No results found in TMDB for title: {}", normalized);
                throw new RuntimeException("Movie not found: " + normalized);
            }

            MovieDTO movieDTO = searchResults.get(0);

            try {
                redisTemplate.opsForValue().set(cacheKey, movieDTO, java.time.Duration.ofHours(6));
                logger.info("✅ Cached MovieDTO in Redis for title: {}", normalized);
            } catch (Exception e) {
                logger.error("❌ Redis error while setting cache for title {}: {}", normalized, e.getMessage());
            }

            return movieDTO;

        } catch (Exception e) {
            logger.error("❌ Error fetching MovieDTO for title {}: {}", normalized, e.getMessage());
            throw new RuntimeException("Failed to get or create MovieDTO: " + normalized);
        }
    }




    @Cacheable(value = "nowPlayingMoviesDTO", unless = "#result == null or #result.isEmpty()")
    @Override
    public List<MovieDTO> getNowPlayingMoviesDTO() {
        String url = apiBaseUrl + "/movie/now_playing?language=en-US&page=1";

        try {
            ResponseEntity<MovieSearchResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, createRequestEntity(), MovieSearchResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<TmdbMovie> results = response.getBody().getResults();
                if (results == null || results.isEmpty()) return Collections.emptyList();
                return MovieDTO.TMDBmovieListToDtoList(results);
            }

        } catch (Exception e) {
            logger.error("❌ Failed to fetch now playing movies: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    @Transactional
    @Cacheable(value = "topRatedMoviesDTO", unless = "#result == null or #result.isEmpty()")
    @Override
    public List<MovieDTO> getTopRatedMoviesDTO() {
        String url = apiBaseUrl + "/movie/top_rated?language=en-US&page=1";
        try {
            ResponseEntity<MovieSearchResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, createRequestEntity(), MovieSearchResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<TmdbMovie> results = response.getBody().getResults();
                if (results == null || results.isEmpty()) return Collections.emptyList();
                return MovieDTO.TMDBmovieListToDtoList(results);
            }
        } catch (Exception e) {
            logger.error("❌ Failed to fetch top rated movies: {}", e.getMessage());
        }
        return Collections.emptyList();
    }


    @Cacheable(value = "upcomingMoviesDTO", unless = "#result == null or #result.isEmpty()")
    @Override
    public List<MovieDTO> getUpcomingMoviesDTO() {
        String url = apiBaseUrl + "/movie/upcoming?language=en-US&region=IL";
        try {
            ResponseEntity<MovieSearchResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, createRequestEntity(), MovieSearchResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<TmdbMovie> results = response.getBody().getResults();
                if (results == null || results.isEmpty()) return Collections.emptyList();
                return MovieDTO.TMDBmovieListToDtoList(results);
            }
        } catch (Exception e) {
            logger.error("❌ Failed to fetch upcoming movies: {}", e.getMessage());
        }
        return Collections.emptyList();
    }


    @Override
    public List<MovieDTO> updateMovieListWithDifferences(List<MovieDTO> oldList, List<String> newTitles) {
        // שמות הכותרים הקיימים ברשימה הישנה
        List<String> currentTitles = oldList.stream()
                .map(MovieDTO::getTitle)
                .collect(java.util.stream.Collectors.toList());

        List<MovieDTO> updatedList = new java.util.ArrayList<>();
        int index = 0;

        for (String newTitle : newTitles) {
            try {
                if (currentTitles.contains(newTitle)) {
                    // השאר את ה-DTO הקיים (לא שומרים שוב)
                    MovieDTO movie = oldList.stream()
                            .filter(m -> m.getTitle().equals(newTitle))
                            .findFirst()
                            .orElseThrow();
                    updatedList.add(movie);
                } else {
                    // זהו סרט "חדש" לרשימת ההמלצות → משוך DTO (קאש/TMDB) ואז פרסיסט ל-DB
                    MovieDTO dto = getOrCreateMovieDTO(newTitle); // ⬅️ משיגים DTO (ללא DB)
                    if (dto == null) {
                        logger.warn("⚠️ Skipping null DTO for title '{}'", newTitle);
                    } else {
                        // upsert ל-DB (שומרים רק המלצות)
                        MovieDTO persisted = movieDtoRepository.findById(dto.getId())
                                .map(db -> {
                                    db.setTitle(dto.getTitle());
                                    db.setPosterUrl(dto.getPosterUrl());
                                    db.setVoteAverage(dto.getVoteAverage());
                                    // חיתוך overview ל-255 כדי לא להפיל MySQL
                                    String ov = dto.getOverview();
                                    if (ov != null && ov.length() > 255) ov = ov.substring(0, 255);
                                    db.setOverview(ov);
                                    return db;
                                })
                                .orElseGet(() -> {
                                    // חיתוך overview גם ב-insert ראשון
                                    String ov = dto.getOverview();
                                    if (ov != null && ov.length() > 255) {
                                        dto.setOverview(ov.substring(0, 255));
                                    }
                                    return dto;
                                });

                        persisted = movieDtoRepository.save(persisted);

                        // עדכן קאש עם הגרסה הנשמרת
                        try {
                            String cacheKey = "movieDTO:title:" + newTitle.toLowerCase();
                            redisTemplate.opsForValue().set(cacheKey, persisted, java.time.Duration.ofHours(6));
                        } catch (Exception e) {
                            logger.error("❌ Redis error caching persisted title {}: {}", newTitle, e.getMessage());
                        }

                        updatedList.add(persisted);
                    }
                }
            } catch (RuntimeException ex) {
                // לא מפילים את כל הרשימה על כותר בודד
                logger.warn("⚠️ Skipping title '{}' due to error: {}", newTitle, ex.getMessage());
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
        if (genreIds==null||genreIds.isEmpty())
        {
            return Collections.EMPTY_SET;
        }
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

    @Cacheable(value = "moviesByGenreDTO", key = "#genreId + '-' + #page", unless = "#result == null or #result.isEmpty()")
    @Override
    public List<MovieDTO> getMoviesByGenreDTO(int genreId, int page) {
        String url = apiBaseUrl +
                "/discover/movie?language=en-US&with_genres=" + genreId +
                "&sort_by=popularity.desc&page=" + page;
        try {
            ResponseEntity<MovieSearchResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, createRequestEntity(), MovieSearchResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<TmdbMovie> results = response.getBody().getResults();
                if (results == null || results.isEmpty()) return Collections.emptyList();
                return MovieDTO.TMDBmovieListToDtoList(results);
            }
        } catch (Exception e) {
            logger.error("❌ Failed to fetch movies by genre {} page {}: {}", genreId, page, e.getMessage());
        }
        return Collections.emptyList();
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


//    public List<Movie> prepareCandidateMovies() {
//        List<Movie> candidates = new ArrayList<>();
//        candidates.addAll(getTopRatedMovies());
//        candidates.addAll(getNowPlayingMovies());
//
//        for (Movie movie : candidates) {
//            Long id = movie.getId();
//            if (!embeddingStorageService.hasEmbedding(id)) {
//                String overview = movie.getOverview();
//                if (overview != null && !overview.isBlank()) {
//                    float[] vector = embeddingService.getEmbedding(overview);
//                    embeddingStorageService.addEmbedding(id, vector);
//                    logger.info("🧠 Embedded candidate movie '{}'", movie.getTitle());
//                }
//            }
//        }
//
//        return candidates;
//    }

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
