// src/main/java/movieMentor/models/CohereGenerateRequest.java
package movieMentor.models;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class CohereGenerateRequest {
    private String prompt;
    private int max_tokens;
    private double temperature;
    private List<String> stop_sequences;
    private double k; // Optional
    private double p; // Optional
    private int num_generations; // Optional
    private String model; // Optional, e.g., "command"
    private Map<String, String> return_likelihoods; // Optional, e.g., "NONE"
}
