package com.krm.ReseachAssistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krm.ReseachAssistant.GeminiResponse;
import com.krm.ReseachAssistant.ResearchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class ResearchService {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public ResearchService(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    public String processContent(ResearchRequest researchRequest) {
        String prompt = buildPrompt(researchRequest);
        Map<String, Object> request = buildRequest(prompt);
        String response = getResponse(request);
        return extractTextFromResponse(response);
    }

    private String buildPrompt(ResearchRequest request) {
        StringBuilder prompt = new StringBuilder();
        switch (request.getOperations()) {
            case "summarize":
                prompt.append("You are an expert research assistant. Carefully read the following content and generate a clear, concise, and insightful summary that captures the key ideas and intent: ");
                break;
            case "suggest":
                prompt.append("You are a domain expert tasked with generating thoughtful and practical suggestions based on the following content. Analyze the context, identify challenges or gaps, and propose meaningful, actionable ideas to improve or enhance the subject matter: ");
                break;
            default:
                throw new IllegalArgumentException("Unknown argument: " + request.getOperations());
        }
        prompt.append(request.getContent());
        return prompt.toString();
    }

    private Map<String, Object> buildRequest(String prompt) {
        return Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );
    }

    private String getResponse(Map<String, Object> request) {
        return webClient.post()
                .uri(geminiApiUrl + geminiApiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private String extractTextFromResponse(String response) {
        try {
            GeminiResponse geminiResponse = objectMapper.readValue(response, GeminiResponse.class);

            if (geminiResponse.getCandidates() != null && !geminiResponse.getCandidates().isEmpty()) {
                GeminiResponse.Candidate firstCandidate = geminiResponse.getCandidates().get(0);

                if (firstCandidate.getContent() != null &&
                        firstCandidate.getContent().getParts() != null &&
                        !firstCandidate.getContent().getParts().isEmpty() &&
                        firstCandidate.getContent().getParts().get(0).getText() != null) {

                    return firstCandidate.getContent().getParts().get(0).getText();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse or extract text from Gemini response", e);
        }

        return "No response text available.";
    }
}
