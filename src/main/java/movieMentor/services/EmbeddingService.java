// EmbeddingService.java
package movieMentor.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import javax.annotation.PostConstruct; // לוודא שזה מיובא

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // קבועים ספציפיים ל-OpenAI
    private static final String OPENAI_EMBED_URL = "https://api.openai.com/v1/embeddings";
    // מודל מומלץ: text-embedding-3-small מציע איזון טוב בין ביצועים לעלות
    // חלופות: "text-embedding-ada-002" (דור קודם), "text-embedding-3-large" (ליישומים מתקדמים)
    private static final String OPENAI_EMBEDDING_MODEL = "text-embedding-3-small";
    private static final int OPENAI_EMBEDDING_DIMENSION = 1536; // ברירת מחדל עבור text-embedding-3-small וגם ada-002

    @Value("${openai.api.key.vector}") // קריאת מפתח ה-API מקובץ application.properties
    private String openAiApiKey;

    /**
     * מחלץ וקטור הטמעה עבור טקסט נתון באמצעות OpenAI API.
     *
     * @param text הטקסט עבורו יש לחלץ הטמעה.
     * @return מערך של floats המייצג את וקטור ההטמעה, או מערך ריק במקרה של כשל.
     */
    public float[] getEmbedding(String text) {
        // רסאמצ'ק הוסר כיוון שהלוג יספיק לרוב המקרים
        // boolean resCheck = false;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey); // OpenAI משתמש באימות Bearer token

            // OpenAI מצפה ל"input" (מחרוזת או מערך מחרוזות) ול"model"
            String requestBody = objectMapper.createObjectNode()
                    .put("input", text)
                    .put("model", OPENAI_EMBEDDING_MODEL)
                    .toString();

            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    OPENAI_EMBED_URL,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                // ההטמעות של OpenAI נמצאות תחת data[0].embedding
                JsonNode singleEmbeddingNode = root.path("data").path(0).path("embedding");

                if (singleEmbeddingNode == null || !singleEmbeddingNode.isArray() || singleEmbeddingNode.size() != OPENAI_EMBEDDING_DIMENSION) {
                    log.error("❌ מערך הטמעת OpenAI אינו תקין או בגודל שגוי: ציפינו ל-{}, קיבלנו {}",
                            OPENAI_EMBEDDING_DIMENSION, singleEmbeddingNode != null ? singleEmbeddingNode.size() : "null");
                    return new float[0];
                }

                float[] embedding = new float[OPENAI_EMBEDDING_DIMENSION];
                for (int i = 0; i < singleEmbeddingNode.size(); i++) {
                    embedding[i] = (float) singleEmbeddingNode.get(i).asDouble();
                }

                return embedding;
            } else {
                log.error("❌ נכשלה קבלת הטמעה מ-OpenAI: סטטוס {}, גוף תגובה: {}",
                        response.getStatusCode(), response.getBody());
            }
        } catch (ResourceAccessException e) {
            log.error("❌ שגיאת רשת בחיבור ל-OpenAI API: {}", e.getMessage());
        } catch (Exception e) {
            log.error("❌ שגיאה בקבלת הטמעה מ-OpenAI", e);
        }

        return new float[0];
    }
}