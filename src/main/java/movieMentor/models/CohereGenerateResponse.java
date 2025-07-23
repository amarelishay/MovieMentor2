package movieMentor.models;

import lombok.Data;

import java.util.List;

public class CohereGenerateResponse {
    private List<CohereGeneration> generations;

    @Data
    public static class CohereGeneration {
        private String text;
        private String id;
        private List<TokenLikelihood> token_likelihoods; // Optional
    }

    @Data
    public static class TokenLikelihood {
        private String token;
        private Double likelihood;
    }
}