package movieMentor.services;

import lombok.RequiredArgsConstructor;
import movieMentor.beans.Movie;
import movieMentor.beans.User;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);
    private final OpenAiService openAiService;
    private final TmdbService tmdbService;
    private final EmbeddingService embeddingService;
    private final EmbeddingStorageService embeddingStorageService;
    private final UserSimilarityService userSimilarityService;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final Map<String, float[]> candidateEmbeddings = new HashMap<>();
    private final List<Movie> candidates = new ArrayList<>();
    @Autowired
    private CacheManager cacheManager;
    private static final int OPENAI_EMBEDDING_DIMENSION = 1536; // ברירת מחדל עבור text-embedding-3-small וגם ada-002

    public List<String> generateRecommendations(User user) {
        List<String> favoriteTitles = user.getFavoriteMovies().stream()
                .map(Movie::getTitle)
                .collect(Collectors.toList());

        List<String> historyTitles = user.getWatchHistory().stream()
                .skip(Math.max(0, user.getWatchHistory().size() - 30))
                .map(Movie::getTitle)
                .collect(Collectors.toList());

        return openAiService.getRecommendations(favoriteTitles, historyTitles);

    }

//    @PostConstruct
    public void initMovieCandidates() {
        logger.info("🚀 Starting pre-loading of movie candidates...");

        try {
            // הוספת סרטים מ-TMDB
            List<Movie> all = new ArrayList<>();
            logger.info("retrieving now playing movies");
            candidates.addAll(tmdbService.getNowPlayingMovies());
            logger.info("retrieving upcoming movies");
            candidates.addAll(tmdbService.getUpComingMovies());
            logger.info("retrieving top rated movies");
            candidates.addAll(tmdbService.getTopRatedMovies());


            // הוספת סרטים מה-enum
            for (TopMoviesData movieData : TopMoviesData.values()) {
                Movie movie = tmdbService.getOrCreateMovie(movieData.getTitle());
                logger.info("💿 movie {} was successfully downloaded ", movie.getTitle());
                if (movie != null) { // ודא שהסרט נמצא לפני הוספתו
                    candidates.add(movie);

                    if (!embeddingStorageService.hasEmbedding(movie.getId())) {
                        logger.info("🎬 Adding embedding for movie from enum: {}", movie.getTitle());
                        String movieOverview = movie.getOverview(); // קבל את התיאור

                        // **** התיקון לטיפול ב"Missing text" וגודל ה-embedding ****
                        if (movieOverview != null && !movieOverview.trim().isEmpty()) {
                            float[] embedding = embeddingService.getEmbedding(movieOverview);
                            if (embedding != null && embedding.length == OPENAI_EMBEDDING_DIMENSION) {
                                embeddingStorageService.addEmbedding(movie.getId(), embedding);
                                candidateEmbeddings.put(movie.getTitle(), embedding);
                            } else {
                                logger.warn("⚠️ Invalid or empty embedding for movie '{}'. Expected 768 dimensions. Skipped.", movie.getTitle());
                            }
                        } else {
                            logger.warn("⚠️ Skipping embedding for movie '{}' due to missing or empty overview.", movie.getTitle());
                        }
                        // ******************************************************
                    } else {
                        logger.info("ℹ️ Embedding already exists for movie: {}", movie.getTitle());
                        // אם ה-embedding כבר קיים, ודא שהוא נכנס גם ל-candidateEmbeddings map
                        float[] existingEmbedding = embeddingStorageService.getEmbedding(movie.getId());
                        // ודא שגם ה-embedding הקיים בגודל 768
                        if (existingEmbedding != null && existingEmbedding.length == OPENAI_EMBEDDING_DIMENSION) {
                            candidateEmbeddings.put(movie.getTitle(), existingEmbedding);
                        } else {
                            logger.warn("⚠️ Existing embedding for movie '{}' is invalid or wrong size. Not added to candidateEmbeddings map.", movie.getTitle());
                        }
                    }
                } else {
                    logger.warn("❌ Could not retrieve movie from TMDB for enum entry: {}", movieData.getTitle());
                }
            }
            cacheManager.getCache("candidateMovies").put("all", new ArrayList<>(candidates));
            logger.info("📦 candidateMovies cached in Redis");
            int saved = 0;
            for (Movie movie : candidates) {
                if (!movieRepository.existsById(movie.getId())) {
                    movieRepository.saveAndFlush(movie);
                    saved++;
                }
            }
        } catch (Exception e) {
            logger.error("❌ Error while preloading movie candidates", e);
        }

        logger.info("🎯 Finished preloading {} movie candidates and their embeddings.", candidateEmbeddings.size());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationFullyReady() {
        logger.info("✅ RecommendationService and initial movies are fully ready.");
    }

    public List<Movie> getCandidateMovies() {
        return new ArrayList<>(candidates);
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

    public List<Movie> findMostSimilarMovies(float[] userVector, List<Movie> candidateMovies, int topN) {
        if (userVector == null || userVector.length == 0) {
            throw new IllegalArgumentException("User embedding is missing");
        }

        List<Map.Entry<Movie, Double>> scored = new ArrayList<>();

        for (Movie movie : candidateMovies) {
            float[] vector = embeddingStorageService.getEmbedding(movie.getId());
            // ודא שגודל הוקטור תואם לוקטור המשתמש (כעת 768)
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

    public List<Movie> getRecommendationsFromSimilarUsers(User user, int topUsers) {
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
}