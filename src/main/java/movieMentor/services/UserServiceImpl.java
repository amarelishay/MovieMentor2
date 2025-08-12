package movieMentor.services;

import lombok.RequiredArgsConstructor;
import movieMentor.beans.Movie;
import movieMentor.beans.User;
import movieMentor.beans.MovieDTO;
import movieMentor.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional

@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserRepository userRepository;
    private final TmdbService tmdbService;
    private final RecommendationService recommendationService;
    private final EmbeddingService embeddingService;
    private final EmbeddingStorageService embeddingStorageService;
    private final UserVectorClientService userVectorClient;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    @Transactional
    @CacheEvict(value = "userRecommendations", key = "#username")

    public void addFavoriteMovie(String username, String movieTitle) {
        User user = fetchUser(username);
        Movie movie = tmdbService.getOrCreateMovie(movieTitle);
        boolean added = user.getFavoriteMovies().add(movie);

        if (added) {
            logger.info("✅ Added movie '{}' to favorites for user '{}'", movieTitle, username);
            updateUserContextInVectorDB(user);  // ← עדכון FAISS
            updateRecommendations(user);
            userRepository.save(user);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "userRecommendations", key = "#username")

    public void removeFavoriteMovie(String username, Long movieId) {
        User user = fetchUser(username);
        boolean removed = user.getFavoriteMovies().removeIf(m -> m.getId().equals(movieId));

        if (removed) {
            logger.info("🗑️ Removed movie ID {} from favorites for user '{}'", movieId, username);
            updateRecommendations(user);
            updateUserContextInVectorDB(user);  // ← עדכון FAISS
        }
    }
    @Override
    @Transactional
    @CacheEvict(value = {"userRecommendations", "userHistory"}, key = "#username")
    public void addToWatchHistory(String username, String movieTitle) {
        User user = fetchUser(username);

        // משיגים צילום מצב כ-DTO (לא ישות JPA)
        MovieDTO dto = tmdbService.getOrCreateMovieDTO(movieTitle);
        if (dto == null) {
            logger.warn("⚠️ Could not resolve MovieDTO for title '{}'", movieTitle);
            return;
        }

        // המרה ל-JSON ושמירה ב-ElementCollection
        try {
            String json = objectMapper.writeValueAsString(dto);

            // דה-דופליקציה קלה: אל תוסיף אם האחרון זהה (אופציונלי אך שימושי)
            List<String> history = user.getWatchHistoryJson();
            if (history.isEmpty() || !history.get(history.size() - 1).equals(json)) {
                history.add(json);
            }

            // שמירת אורך מקסימלי (אופציונלי)
            final int LIMIT = 200;
            if (history.size() > LIMIT) {
                history.subList(0, history.size() - LIMIT).clear();
            }

            userRepository.save(user);
            logger.info("🎬 Added '{}' to watch history (JSON) of '{}'", movieTitle, username);

        } catch (Exception e) {
            logger.warn("Failed to serialize MovieDTO for history of '{}': {}", username, e.getMessage());
        }

        // דואגים ל-embedding של הסרט (ע”פ ה-ID של ה-DTO)
        if (dto.getId() != null && !embeddingStorageService.hasEmbedding(dto.getId())) {
            String overview = dto.getOverview() != null ? dto.getOverview() : "";
            float[] vector = embeddingService.getEmbedding(overview);
            if (vector != null && vector.length > 0) {
                embeddingStorageService.addEmbedding(dto.getId(), vector);
            }
        }

        // מריצים עדכון המלצות כל 5 הוספות (כמו קודם)
        if (user.getWatchHistoryJson().size() % 5 == 0) {
            logger.info("📊 Triggering recommendation update — history count divisible by 5");
            updateRecommendations(user);
        }

        // עדכון וקטור המשתמש ב-Vector DB
        updateUserContextInVectorDB(user);
    }

    private float[] buildUserProfileEmbeddingWeighted(User user) {
        // משקלים
        final float FAVORITE_WEIGHT = 2.0f;
        final float HISTORY_WEIGHT  = 1.0f;
        final int   HISTORY_LIMIT   = 30;

        List<float[]> vectors = new ArrayList<>();
        List<Float>   weights = new ArrayList<>();

        // 1) מועדפים (Movie ישויות)
        for (Movie movie : user.getFavoriteMovies()) {
            if (movie == null || movie.getId() == null) continue;
            float[] vec = embeddingStorageService.getEmbedding(movie.getId());
            if (vec != null && vec.length > 0) {
                vectors.add(vec);
                weights.add(FAVORITE_WEIGHT);
            }
        }

        // 2) היסטוריית צפייה (JSON -> MovieDTO) – 30 אחרונים
        List<String> snapshots = user.getWatchHistoryJson();
        if (snapshots != null && !snapshots.isEmpty()) {
            int from = Math.max(0, snapshots.size() - HISTORY_LIMIT);
            for (int i = from; i < snapshots.size(); i++) {
                String json = snapshots.get(i);
                if (json == null || json.isEmpty()) continue;

                try {
                    MovieDTO dto = objectMapper.readValue(json, MovieDTO.class);
                    if (dto == null || dto.getId() == null) continue;

                    float[] vec = embeddingStorageService.getEmbedding(dto.getId());
                    if (vec == null || vec.length == 0) {
                        String overview = dto.getOverview() != null ? dto.getOverview() : "";
                        vec = embeddingService.getEmbedding(overview);
                        if (vec != null && vec.length > 0) {
                            embeddingStorageService.addEmbedding(dto.getId(), vec);
                        }
                    }

                    if (vec != null && vec.length > 0) {
                        vectors.add(vec);
                        weights.add(HISTORY_WEIGHT);
                    }
                } catch (Exception ignore) {
                    // דלג על רשומה פגומה
                }
            }
        }

        if (vectors.isEmpty()) {
            logger.warn("⚠️ No valid embeddings found for user '{}'", user.getUsername());
            return new float[0];
        }

        // 3) ממוצע משוקלל
        int dim = vectors.get(0).length;
        float[] weightedSum = new float[dim];
        float totalWeight = 0f;

        for (int idx = 0; idx < vectors.size(); idx++) {
            float[] vec = vectors.get(idx);
            // הגנה ממדדים לא תואמים (אם יש)
            if (vec.length != dim) continue;

            float w = weights.get(idx);
            totalWeight += w;
            for (int d = 0; d < dim; d++) {
                weightedSum[d] += vec[d] * w;
            }
        }

        if (totalWeight == 0f) {
            logger.warn("⚠️ Total weight is zero for user '{}'", user.getUsername());
            return new float[0];
        }

        for (int d = 0; d < dim; d++) {
            weightedSum[d] /= totalWeight;
        }

        logger.info("✅ Built weighted profile embedding for '{}'", user.getUsername());
        return weightedSum;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "userRecommendations", key = "#username", unless = "#result == null || #result.isEmpty()")
    public List<MovieDTO> getRecommendations(String username) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        u.getRecommendedMovies().size();
        return u.getRecommendedMovies().stream()
                .map(MovieDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "userFavorites", key = "#username")
    public List<MovieDTO> getFavorites(String username) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        // לדרוך על ה‑LAZY לפני סיריאליזציה
        u.getFavoriteMovies().size();
        return u.getFavoriteMovies().stream()
                .map(MovieDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<MovieDTO> getHistoryDTO(String username) {
        User user = fetchUser(username);
        List<MovieDTO> history = new ArrayList<>();

        for (String json : user.getWatchHistoryJson()) {
            if (json == null || json.isEmpty()) continue;
            try {
                MovieDTO dto = objectMapper.readValue(json, MovieDTO.class);
                history.add(dto);
            } catch (Exception e) {
                logger.warn("⚠️ Failed to parse watch history entry for user '{}': {}", username, e.getMessage());
            }
        }

        return history;
    }


    @Override
    @Transactional
    @CacheEvict(value = "userRecommendations", key = "#username")
    public void setRecommendedMovies(String username, List<String> recommendedTitles) {
        User user = fetchUser(username);
        List<MovieDTO> updated = tmdbService.updateMovieListWithDifferences(user.getRecommendedMovies(), recommendedTitles);
        user.setRecommendedMovies(updated);
        userRepository.saveAndFlush(user);
        logger.info("🛠️ Manually updated recommended movies for '{}'", username);
    }
//
//    @Override
//    @Transactional
//    @CacheEvict(value = "userRecommendations", key = "#username")
//    public void updateRecommendations(User user) {
//        float[] userVector = buildUserProfileEmbeddingWeighted(user);
//        if (userVector.length == 0) {
//            logger.warn("⚠️ User '{}' has no embedding data – skipping vector-based recommendations", user.getUsername());
//        }
//        List<MovieDTO> candidateMovies = recommendationService.getCandidateMovies();
//        List<MovieDTO> similarMovies = new ArrayList<>();
//        if (userVector.length > 0) {
//            similarMovies = recommendationService.findMostSimilarMovies(userVector, candidateMovies, 10);
//            logger.info("🎯 Found {} vector-based similar movies for '{}'", similarMovies.size(), user.getUsername());
//        }
//
//        List<String> newTitles = recommendationService.generateRecommendations(user);
//        List<MovieDTO> updatedGptList = tmdbService.updateMovieListWithDifferences(user.getRecommendedMovies(), newTitles);
//        List<MovieDTO> similarTasteMovies = recommendationService.getRecommendationsFromSimilarUsers(user, 5);
//        // 5. מיזוג חכם (למשל, לשלב או להעדיף אחד מהמקורות)
//        Set<MovieDTO> finalRecommendations = new LinkedHashSet<>();
//        logger.info(" similar movies based on vector embedding '{}'", similarMovies.stream().map(MovieDTO::getTitle).collect(Collectors.toList()));
//        finalRecommendations.addAll(similarMovies);
//        logger.info(" similar movies based on chat GPT '{}'", updatedGptList.stream().map(MovieDTO::getTitle).collect(Collectors.toList()));
//        finalRecommendations.addAll(updatedGptList);
//        logger.info(" similar movies based on other users '{}'", similarTasteMovies.stream().map(MovieDTO::getTitle).collect(Collectors.toList()));
//        finalRecommendations.addAll(similarTasteMovies);
//        // 6. עדכון המשתמש ושמירה
//        user.setRecommendedMovies(new ArrayList<>(finalRecommendations));
//        userRepository.saveAndFlush(user);
//        logger.info(" user recommendations '{}'", finalRecommendations.stream().map(MovieDTO::getTitle).collect(Collectors.toList()));
//        logger.info("✅ Updated recommended movies for '{}'", user.getUsername());
//
//    }


    @Override
@Transactional
@CacheEvict(cacheNames = "userRecommendations", key = "#user.username")
public void updateRecommendations(User user) {
    final String username = user.getUsername();

    // 1) וקטור פרופיל משתמש
    float[] userVector = buildUserProfileEmbeddingWeighted(user);
    if (userVector == null || userVector.length == 0) {
        logger.warn("⚠️ User '{}' has no embedding data – skipping vector-based recommendations", username);
    }

    // 2) מועמדים + דימיון וקטורי (אופציונלי)
    List<MovieDTO> candidateMovies = recommendationService.getCandidateMovies();
    List<MovieDTO> similarMovies = Collections.emptyList();
    if (userVector != null && userVector.length > 0) {
        similarMovies = recommendationService.findMostSimilarMovies(userVector, candidateMovies, 10);
        logger.info("🎯 Found {} vector-based similar movies for '{}'", similarMovies.size(), username);
    }

    // 3) GPT titles -> עדכון לרשימת DTO (שומר שדות מלאים)
    List<String> newTitles = recommendationService.generateRecommendations(user);
    List<MovieDTO> updatedGptList =
            tmdbService.updateMovieListWithDifferences(user.getRecommendedMovies(), newTitles);

    // 4) משתמשים דומים
    List<MovieDTO> similarTasteMovies = recommendationService.getRecommendationsFromSimilarUsers(user, 5);

    // 5) מיזוג חכם: שומר סדר, מסיר כפילויות לפי id ואח"כ title
    LinkedHashMap<String, MovieDTO> merged = new LinkedHashMap<>();
    java.util.function.Consumer<MovieDTO> add = m -> {
        if (m == null) return;
        String key = dedupeKey(m);
        if (key != null && !merged.containsKey(key)) merged.put(key, m);
    };

    similarMovies.forEach(add);        // עדיפות 1
    updatedGptList.forEach(add);       // עדיפות 2
    similarTasteMovies.forEach(add);   // עדיפות 3

    // 6) חיתוך לאורך סביר
    final int MAX_RECS = 30;
    List<MovieDTO> finalRecommendations = merged.values()
            .stream()
            .limit(MAX_RECS)
            .collect(java.util.stream.Collectors.toList());

    // 7) עדכון ה-DB (ElementCollection עם OrderColumn ישמור את הסדר)
    user.getRecommendedMovies().clear();
    user.getRecommendedMovies().addAll(finalRecommendations);
    userRepository.saveAndFlush(user);

    logger.info("✅ Updated {} recommendations for '{}': {}",
            finalRecommendations.size(),
            username,
            finalRecommendations.stream().map(MovieDTO::getTitle).collect(java.util.stream.Collectors.toList()));
}

    // עוזר לדה-דופליקציה: קודם לפי id, אחרת לפי title מנורמל
    private String dedupeKey(MovieDTO m) {
        if (m.getId() != null) return "id:" + m.getId();
        if (m.getTitle() != null) return "t:" + m.getTitle().trim().toLowerCase(java.util.Locale.ROOT);
        return null;
    }

    private User fetchUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    private void updateUserContextInVectorDB(User user) {
        // 1) אוספים DTOs: מועדפים (Movie -> MovieDTO) + היסטוריה (JSON -> MovieDTO)
        List<MovieDTO> allMovies = new ArrayList<>();

        if (user.getFavoriteMovies() != null && !user.getFavoriteMovies().isEmpty()) {
            allMovies.addAll(
                    user.getFavoriteMovies().stream()
                            .filter(Objects::nonNull)
                            .map(MovieDTO::new)
                            .collect(Collectors.toList())
            );
        }

        List<String> snapshots = Optional.ofNullable(user.getWatchHistoryJson())
                .orElseGet(Collections::emptyList);

        for (String json : snapshots) {
            if (json == null || json.isEmpty()) continue;
            try {
                MovieDTO dto = objectMapper.readValue(json, MovieDTO.class);
                if (dto != null) {
                    allMovies.add(dto);
                }
            } catch (Exception e) {
                logger.warn("Failed to deserialize MovieDTO for '{}': {}", user.getUsername(), e.getMessage());
            }
        }

        // 2) ודא שקיים embedding לכל סרט (לפי id)
        for (MovieDTO movie : allMovies) {
            if (movie == null || movie.getId() == null) continue;

            if (!embeddingStorageService.hasEmbedding(movie.getId())) {
                String text = movie.getOverview() != null ? movie.getOverview() : "";
                float[] vector = embeddingService.getEmbedding(text);
                if (vector != null && vector.length > 0) {
                    embeddingStorageService.addEmbedding(movie.getId(), vector);
                }
            }
        }

        // 3) מחשבים וקטור משתמש
        float[] userVector = buildUserProfileEmbeddingWeighted(user);
        if (userVector == null || userVector.length == 0) {
            logger.warn("⛔ User '{}' has empty vector – skipping FAISS update", user.getUsername());
            return;
        }

        // 4) שולחים ל-Vector DB עם מטא־דאטה
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("favorite_count", user.getFavoriteMovies() != null ? user.getFavoriteMovies().size() : 0);
        metadata.put("watch_history_count", snapshots.size());
        metadata.put("username", user.getUsername());

        userVectorClient.storeUserVector(String.valueOf(user.getId()), userVector, metadata);
    }

    public List<Map<String, Object>> findUsersWithSimilarTaste(User user, int topK) {
        float[] userVector = buildUserProfileEmbeddingWeighted(user);
        if (userVector.length == 0) {
            logger.warn("⛔ Cannot find similar users – empty vector for '{}'", user.getUsername());
            return List.of();
        }
        return userVectorClient.findSimilarUsers(userVector, topK);
    }


}
