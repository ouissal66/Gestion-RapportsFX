package com.audit.auditaifx.service;

import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class GeminiService {
    private String apiKey;
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-pro:generateContent?key=";
    private final HttpClient httpClient;

    public GeminiService() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        loadConfig();
    }

    private void loadConfig() {
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            Properties props = new Properties();
            props.load(fis);
            this.apiKey = props.getProperty("google.api.key");
        } catch (Exception e) {}
    }

    public String generateContent(String prompt) {
        try { return generateContentAsync(prompt).get(); } catch (Exception e) { return "Erreur IA."; }
    }

    public CompletableFuture<String> generateContentAsync(String prompt) {
        if (apiKey == null || apiKey.isEmpty()) return CompletableFuture.completedFuture("Erreur : Clé absente.");
        String jsonRequest = "{\"contents\": [{\"parts\":[{\"text\": \"" + prompt.replace("\"", "\\\"") + "\"}]}]}";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(API_URL + apiKey))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(jsonRequest)).build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) return extractText(response.body());
                    return "Erreur IA (Code " + response.statusCode() + ").";
                }).exceptionally(ex -> "Erreur connexion.");
    }

    private String extractText(String json) {
        try {
            int start = json.indexOf("\"text\": \"") + 9;
            int end = json.indexOf("\"", start);
            return json.substring(start, end).replace("\\n", "\n");
        } catch (Exception e) { return "Réponse illisible."; }
    }
}
