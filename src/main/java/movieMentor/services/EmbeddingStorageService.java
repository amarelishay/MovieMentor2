package movieMentor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmbeddingStorageService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    // זיכרון זמני לשמירת embedding לכל סרט לפי ה־ID שלו
    private final Map<Long, float[]> embeddingMap = new ConcurrentHashMap<>();

    /**
     * שומר embedding של סרט לפי ה־movieId שלו
     */
    public void addEmbedding(Long movieId, float[] vector) {
        embeddingMap.put(movieId, vector);
    }

    /**
     * מחזיר את ה־embedding של הסרט לפי ה־movieId
     */
    public float[] getEmbedding(Long movieId) {
        return embeddingMap.get(movieId);
    }

    /**
     * בודק אם כבר שמרנו embedding לסרט הזה
     */
    public boolean hasEmbedding(Long movieId) {
        return embeddingMap.containsKey(movieId);
    }
}
