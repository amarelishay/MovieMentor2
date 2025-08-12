package movieMentor.services;

import lombok.RequiredArgsConstructor;
import movieMentor.beans.Movie;
import movieMentor.beans.MovieDTO;
import movieMentor.beans.User;
import movieMentor.beans.UserWatchEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserSimilarityService {

    private static final Logger logger = LoggerFactory.getLogger(UserSimilarityService.class);

    private final EmbeddingStorageService embeddingStorageService;
    private final UserVectorClientService userVectorClientService;

    public List<Map<String, Object>> findUsersWithSimilarTaste(User user, int topK) {
        float[] userVector = buildUserProfileEmbeddingWeighted(user);

        if (userVector.length == 0) {
            logger.warn("⚠️ Cannot calculate similar users – empty vector for '{}'", user.getUsername());
            return Collections.emptyList();
        }

        List<Map<String, Object>> similarUsers = userVectorClientService.findSimilarUsers(userVector, topK);
        logger.info("🤝 Found {} similar users for '{}'", similarUsers.size(), user.getUsername());

        return similarUsers;
    }

    private float[] buildUserProfileEmbeddingWeighted(User user) {
        final float FAVORITE_WEIGHT = 2.0f;
        final float HISTORY_WEIGHT = 1.0f;
        final int HISTORY_LIMIT = 30;

        List<float[]> vectors = new ArrayList<>();
        List<Float> weights = new ArrayList<>();

        for (Movie movie : user.getFavoriteMovies()) {
            float[] vector = embeddingStorageService.getEmbedding(movie.getId());
            if (vector != null && vector.length > 0) {
                vectors.add(vector);
                weights.add(FAVORITE_WEIGHT);
            }
        }

        List<UserWatchEntry> history = user.getWatchHistory();
        int start = Math.max(0, history.size() - HISTORY_LIMIT);
        List<UserWatchEntry> recentHistory = history.subList(start, history.size());

        for (UserWatchEntry movie : recentHistory) {
            float[] vector = embeddingStorageService.getEmbedding(movie.getId());
            if (vector != null && vector.length > 0) {
                vectors.add(vector);
                weights.add(HISTORY_WEIGHT);
            }
        }

        if (vectors.isEmpty()) {
            logger.warn("⚠️ No valid embeddings found for user '{}'", user.getUsername());
            return new float[0];
        }

        int dim = vectors.get(0).length;
        float[] weightedSum = new float[dim];
        float totalWeight = 0;

        for (int v = 0; v < vectors.size(); v++) {
            float[] vec = vectors.get(v);
            float weight = weights.get(v);
            totalWeight += weight;

            for (int i = 0; i < dim; i++) {
                weightedSum[i] += vec[i] * weight;
            }
        }

        for (int i = 0; i < dim; i++) {
            weightedSum[i] /= totalWeight;
        }

        logger.info("✅ Built weighted embedding for '{}'", user.getUsername());
        return weightedSum;
    }
}
