package movieMentor.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserVectorClientService {

    private static final Logger logger = LoggerFactory.getLogger(UserVectorClientService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${qdrant.api.url}")
    private String qdrantApiUrl;

    @Value("${qdrant.api.key:}") // קבלת ה-API Key
    private String qdrantApiKey;

    @Value("${qdrant.collection.name}")
    private String qdrantCollectionName;

    // *** גודל הווקטור הנדרש, כעת 1536 ***
    private static final int REQUIRED_EMBEDDING_DIM = 1536;

    @PostConstruct
    public void init() {
        createCollectionIfNotExists();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // הוסף את מפתח ה-API לכותרות אם הוא קיים
        if (qdrantApiKey != null && !qdrantApiKey.isEmpty()) {
            headers.set("api-key", qdrantApiKey);
        }
        return headers;
    }

    private void createCollectionIfNotExists() {
        String url = qdrantApiUrl + "/collections/" + qdrantCollectionName;
        HttpHeaders headers = createHeaders();

        try {
            restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            logger.info("✅ Qdrant collection '{}' already exists.", qdrantCollectionName);
            return;
        } catch (HttpClientErrorException.NotFound e) {
            logger.info("ℹ️ Qdrant collection '{}' not found, creating it...", qdrantCollectionName);
        } catch (Exception e) {
            logger.error("❌ Error checking Qdrant collection existence: {}", e.getMessage());
        }

        Map<String, Object> vectorsConfig = new HashMap<>();
        vectorsConfig.put("size", REQUIRED_EMBEDDING_DIM); // שימוש במימד 384
        vectorsConfig.put("distance", "Cosine");

        Map<String, Object> collectionConfig = new HashMap<>();
        collectionConfig.put("vectors", vectorsConfig);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(collectionConfig, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("✅ Qdrant collection '{}' created successfully.", qdrantCollectionName);
            } else {
                logger.error("❌ Failed to create Qdrant collection '{}'. Status: {}, Body: {}",
                        qdrantCollectionName, response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            logger.error("❌ Exception while creating Qdrant collection '{}': {}", qdrantCollectionName, e.getMessage());
        }
    }

    public void storeUserVector(String userId, float[] embedding, Map<String, Object> metadata) {
        if (embedding == null || embedding.length != REQUIRED_EMBEDDING_DIM) {
            throw new IllegalArgumentException("❌ Embedding must have " + REQUIRED_EMBEDDING_DIM + " values, but got: " +
                    (embedding == null ? "null" : embedding.length));
        }

        String url = qdrantApiUrl + "/collections/" + qdrantCollectionName + "/points";
        HttpHeaders headers = createHeaders();

        Object formattedUserId;
        try {
            formattedUserId = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            formattedUserId = userId;
        }

        Map<String, Object> point = new HashMap<>();
        point.put("id", formattedUserId);
        point.put("vector", embedding);
        point.put("payload", metadata);

        List<Map<String, Object>> points = Collections.singletonList(point);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("points", points);
        requestBody.put("wait", true);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("✅ Stored vector for user '{}' in Qdrant.", userId);
            } else {
                logger.error("❌ Failed to store vector for user '{}'. Status: {}, Body: {}",
                        userId, response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to store vector for user " + userId + " in Qdrant.");
            }
        } catch (Exception e) {
            logger.error("❌ Exception while storing vector for user '{}': {}", userId, e.getMessage());
            throw new RuntimeException("Failed to store vector for user " + userId, e);
        }
    }

    public List<Map<String, Object>> findSimilarUsers(float[] embedding, int topK) {
        if (embedding == null || embedding.length != REQUIRED_EMBEDDING_DIM) {
            throw new IllegalArgumentException("❌ Embedding must have " + REQUIRED_EMBEDDING_DIM + " values, but got: " +
                    (embedding == null ? "null" : embedding.length));
        }

        String url = qdrantApiUrl + "/collections/" + qdrantCollectionName + "/points/search";
        HttpHeaders headers = createHeaders();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("vector", embedding);
        requestBody.put("limit", topK);
        requestBody.put("with_payload", true);
        requestBody.put("with_vectors", false);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode resultNode = root.get("result");

                if (resultNode != null && resultNode.isArray()) {
                    List<Map<String, Object>> similarUsers = new ArrayList<>();
                    for (JsonNode pointNode : resultNode) {
                        Map<String, Object> userMeta = new HashMap<>();
                        userMeta.put("user_id", pointNode.get("id").asText());
                        if (pointNode.has("payload")) {
                            Map<String, Object> payloadMap = objectMapper.convertValue(pointNode.get("payload"),
                                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                            userMeta.putAll(payloadMap);
                        }
                        userMeta.put("score", pointNode.get("score").asDouble());
                        similarUsers.add(userMeta);
                    }
                    logger.info("✅ Found {} similar users in Qdrant.", similarUsers.size());
                    return similarUsers;
                } else {
                    logger.warn("⚠️ Qdrant search result is not an array or is null.");
                    return Collections.emptyList();
                }
            } else {
                logger.error("❌ Failed to find similar users. Status: {}, Body: {}",
                        response.getStatusCode(), response.getBody());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            logger.error("❌ Exception while finding similar users: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}