package movieMentor.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OpenAiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final Logger logger = LoggerFactory.getLogger(OpenAiService.class);

    @Value("${openai.api.key}")
    private String apiKey;

    public List<String> getRecommendations(List<String> favorites, List<String> history) {
        String prompt = buildPrompt(favorites, history);
        logger.info("Generating recommendations with prompt: {}", prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-3.5-turbo");
        body.put("messages", List.of(
                Map.of("role", "system", "content", "You are a movie expert that suggests personalized movies."),
                Map.of("role", "user", "content", prompt)
        ));
        body.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.openai.com/v1/chat/completions",
                    request,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                Map choices = ((List<Map>) response.getBody().get("choices")).get(0);
                Map message = (Map) choices.get("message");
                String content = (String) message.get("content");

                List<String> recommendations = Arrays.stream(content.split("\\n"))
                        .map(line -> line.replaceAll("^[0-9]+\\.\\s*", ""))
                        .filter(title -> !title.trim().isEmpty())
                        .collect(Collectors.toList());
                return recommendations;
            } else {
                logger.error("Failed to get recommendations, status code: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error while generating recommendations", e);
        }

        return Collections.emptyList();
    }

    private String buildPrompt(List<String> favorites, List<String> history) {
        return "I liked the following movies: " + String.join(", ", favorites) +
                ". I recently watched: " + String.join(", ", history) +
                ". Please recommend me 15 movies that I might enjoy (if there are more movies from the  serice give it some priority ), only list the original movie titles (Be as precise as possible with the name of the film and don't write things like The Lord of the Rings Trilogy or fans movie. The goal is to write each film separately.) in bullet or numbered form.";
    }
}
