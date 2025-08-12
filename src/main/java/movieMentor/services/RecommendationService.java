package movieMentor.services;

import lombok.RequiredArgsConstructor;
import movieMentor.beans.Movie;
import movieMentor.beans.User;
import movieMentor.beans.MovieDTO;
import movieMentor.beans.UserWatchEntry;
import movieMentor.enums.TopMoviesData;
import movieMentor.models.MovieImage;
import movieMentor.repository.MovieRepository;
import movieMentor.repository.UserRepository;
import movieMentor.utils.EmbeddingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);
    private static final int OPENAI_EMBEDDING_DIMENSION = 1536; // ברירת מחדל עבור text-embedding-3-small וגם ada-002
    private final OpenAiService openAiService;
    private final TmdbService tmdbService;
    private final EmbeddingService embeddingService;
    private final EmbeddingStorageService embeddingStorageService;
    private final UserSimilarityService userSimilarityService;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final Map<String, float[]> candidateEmbeddings = new HashMap<>();
    private final List<MovieDTO> candidates = new ArrayList<>();
    @Autowired
    private CacheManager cacheManager;

    public List<String> generateRecommendations(User user) {
        // כותרות ממועדפים (עדיין ישויות Movie)
        List<String> favoriteTitles = user.getFavoriteMovies().stream()
                .map(Movie::getTitle)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // כותרות מהיסטוריה (List<UserWatchEntry> -> MovieDTO -> title)
        List<UserWatchEntry> history = user.getWatchHistory();
        int size = history != null ? history.size() : 0;

        List<String> historyTitles = (history == null ? Collections.<UserWatchEntry>emptyList() : history).stream()
                .skip(Math.max(0, size - 30)) // אחרונים עד 30
                .map(UserWatchEntry::getMovieSnapshot)   // -> MovieDTO
                .filter(Objects::nonNull)
                .map(MovieDTO::getTitle)                 // -> String
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return openAiService.getRecommendations(favoriteTitles, historyTitles);
    }

@PostConstruct
    public void initMovieCandidates() {
        logger.info("🚀 Starting pre-loading of movie candidates...");

        try {
            List<MovieDTO> candidates = new ArrayList<>();

            logger.info("retrieving now playing movies");
            candidates.addAll(tmdbService.getNowPlayingMoviesDTO());

            logger.info("retrieving upcoming movies");
            candidates.addAll(tmdbService.getUpcomingMoviesDTO());

            logger.info("retrieving top rated movies");
            candidates.addAll(tmdbService.getTopRatedMoviesDTO());

            for (TopMoviesData movieData : TopMoviesData.values()) {
                MovieDTO movie = tmdbService.getOrCreateMovieDTO(movieData.getTitle());
                if (movie != null) {
                    logger.info("💿 movie {} was successfully downloaded", movie.getTitle());
                    candidates.add(movie);

                    if (!embeddingStorageService.hasEmbedding(movie.getId())) {
                        logger.info("🎬 Adding embedding for movie from enum: {}", movie.getTitle());
                        String overview = movie.getOverview();

                        if (overview != null && !overview.trim().isEmpty()) {
                            float[] embedding = embeddingService.getEmbedding(overview);
                            if (embedding != null && embedding.length == OPENAI_EMBEDDING_DIMENSION) {
                                embeddingStorageService.addEmbedding(movie.getId(), embedding);
                                candidateEmbeddings.put(movie.getTitle(), embedding);
                            } else {
                                logger.warn("⚠️ Invalid or empty embedding for movie '{}'. Expected 768 dimensions. Skipped.", movie.getTitle());
                            }
                        } else {
                            logger.warn("⚠️ Skipping embedding for movie '{}' due to missing or empty overview.", movie.getTitle());
                        }
                    } else {
                        float[] existingEmbedding = embeddingStorageService.getEmbedding(movie.getId());
                        if (existingEmbedding != null && existingEmbedding.length == OPENAI_EMBEDDING_DIMENSION) {
                            candidateEmbeddings.put(movie.getTitle(), existingEmbedding);
                            logger.info("ℹ️ Existing embedding for movie '{}' loaded into memory", movie.getTitle());
                        } else {
                            logger.warn("⚠️ Existing embedding for movie '{}' is invalid or wrong size. Skipped.", movie.getTitle());
                        }
                    }
                } else {
                    logger.warn("❌ Could not retrieve movie from TMDB for enum entry: {}", movieData.getTitle());
                }
            }

            cacheManager.getCache("candidateMovies").put("all", new ArrayList<>(candidates));
            logger.info("📦 candidateMovies cached in Redis");

        } catch (Exception e) {
            logger.error("❌ Error while preloading movie candidates", e);
        }

        logger.info("✅ Finished preloading {} movie candidates and their embeddings.", candidateEmbeddings.size());
    }


    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationFullyReady() {
        logger.info("✅ RecommendationService and initial movies are fully ready.");
    }



    public Map<String, float[]> getCandidateEmbeddings() {
        return candidateEmbeddings;
    }

    @SuppressWarnings("unchecked")
    private Movie convertMapToMovie(Object obj) {
        try {
            Map<String, Object> map = (Map<String, Object>) obj;

            Movie movie = new Movie();
            movie.setId(Long.valueOf((Integer) map.get("id")));
            movie.setTitle((String) map.get("title"));
            movie.setOriginalTitle((String) map.get("originalTitle"));
            movie.setOverview((String) map.get("overview"));
            movie.setPosterUrl((String) map.get("posterUrl"));
            movie.setReleaseDate(LocalDate.parse((String) map.get("releaseDate")));
            movie.setPopularity(((Number) map.get("popularity")).doubleValue());
            movie.setVoteAverage(((Number) map.get("voteAverage")).doubleValue());
            movie.setVoteCount(((Number) map.get("voteCount")).intValue());

            // תרגום imageUrls (רשימת Map)
            List<Map<String, String>> imageMaps = (List<Map<String, String>>) map.get("imageUrls");
            if (imageMaps != null) {
                List<MovieImage> images = imageMaps.stream()
                        .map(m -> new MovieImage(m.get("type"), m.get("url")))
                        .collect(Collectors.toList());
                movie.setImageUrls(images);
            }

            return movie;
        } catch (Exception e) {
            logger.warn("⚠️ Failed to convert map to Movie: {}", obj, e);
            return null;
        }
    }

    public List<MovieDTO> findMostSimilarMovies(float[] userVector, List<MovieDTO> candidateMovies, int topN) {
        if (userVector == null || userVector.length == 0) {
            throw new IllegalArgumentException("User embedding is missing");
        }

        List<Map.Entry<MovieDTO, Double>> scored = new ArrayList<>();

        for (MovieDTO movie : candidateMovies) {
            float[] vector = embeddingStorageService.getEmbedding(movie.getId());
            if (vector != null && vector.length == userVector.length) {
                double similarity = EmbeddingUtils.cosineSimilarity(userVector, vector);
                scored.add(Map.entry(movie, similarity));
            }
        }

        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        return scored.stream()
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public List<MovieDTO> getRecommendationsFromSimilarUsers(User user, int topUsers) {
        List<Map<String, Object>> similarUsers = userSimilarityService.findUsersWithSimilarTaste(user, topUsers);
        Map<String, Integer> movieFrequency = new HashMap<>();

        for (Map<String, Object> userMeta : similarUsers) {
            String userId = (String) userMeta.get("user_id");
            Optional<User> similarUser = userRepository.findById(Long.parseLong(userId));

            similarUser.ifPresent(u -> {
                for (Movie fav : u.getFavoriteMovies()) {
                    movieFrequency.merge(fav.getTitle(), 1, Integer::sum);
                }
            });
        }

        // מיון הסרטים לפי שכיחות והמרה ל־Movie
        List<String> topTitles = movieFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return tmdbService.updateMovieListWithDifferences(user.getRecommendedMovies(), topTitles);
    }

    public List<MovieDTO> getCandidateMovies() {
        return candidates;
    }
}