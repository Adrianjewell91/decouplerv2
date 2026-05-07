package com.decoupler.impl.llm;

import com.decoupler.interfaces.LlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * {@link LlmClient} that calls a locally running Ollama instance.
 *
 * <p>Default endpoint: {@code http://localhost:11434/api/generate}
 * Default model:    {@code qwen3-coder:30b}
 * Default timeout:  300 s
 */
public class OllamaLlmClient implements LlmClient {

    private static final String DEFAULT_ENDPOINT = "http://localhost:11434/api/generate";
    private static final String DEFAULT_MODEL = "qwen3-coder:30b";

    private final String endpoint;
    private final String model;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public OllamaLlmClient() {
        this(DEFAULT_ENDPOINT, DEFAULT_MODEL);
    }

    public OllamaLlmClient(String endpoint, String model) {
        this.endpoint = endpoint;
        this.model = model;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.mapper = new ObjectMapper();
    }

    @Override
    public String generate(String prompt, Duration timeout) {
        String requestBody;
        try {
            requestBody = mapper.writeValueAsString(new OllamaRequest(model, prompt, false));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialise Ollama request", e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("HTTP call to Ollama failed: " + e.getMessage(), e);
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Ollama returned HTTP " + response.statusCode() + ": " + response.body());
        }

        try {
            JsonNode root = mapper.readTree(response.body());
            return root.path("response").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Ollama response: " + response.body(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Request DTO
    // -------------------------------------------------------------------------

    record OllamaRequest(String model, String prompt, boolean stream) {}
}
