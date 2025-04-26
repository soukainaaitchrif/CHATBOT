package com.example.chatbottest.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class LLMService {
    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";

    public static String generateContent(Map<String, String> request) throws IOException, InterruptedException {
        // Add the model name to the request map
        request.put("model", "granite3-moe:1b");

        try {
            // Prepare JSON payload
            ObjectMapper objectMapper = new ObjectMapper();
            String payload = objectMapper.writeValueAsString(request);

            // Build the HTTP request
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(new URI(OLLAMA_API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            // Send the request
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            // Check response status
            if (response.statusCode() == 200) {
                StringBuilder fullResponse = new StringBuilder();
                String[] responseLines = response.body().split("\n");
                for (String line : responseLines) {
                    JsonNode jsonResponse = objectMapper.readTree(line);
                    JsonNode responseNode = jsonResponse.get("response");
                    if (responseNode != null) {
                        fullResponse.append(responseNode.asText());
                    }
                    if (jsonResponse.get("done").asBoolean()) {
                        break;
                    }
                }
                return fullResponse.toString();
            } else {
                return "Error: " + response.body();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to communicate with Ollama server.";
        }
    }
}