package com.aniplato.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;

/** Provides read-only, screen-aware help using the Gemini API. */
public final class GeminiAssistantService {

    private static final String CONFIG_RESOURCE = "/db.properties";
    private static final String DEFAULT_MODEL = "gemini-3.5-flash-lite";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
    private static final Pattern MODEL_NAME = Pattern.compile("[A-Za-z0-9._-]+");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public CompletableFuture<String> answer(String panel, String question) {
        return CompletableFuture.supplyAsync(() -> createRequest(panel, question))
                .thenCompose(request -> HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                .thenApply(this::readResponse);
    }

    private HttpRequest createRequest(String panel, String question) {
        Properties config = loadConfig();
        String apiKey = config.getProperty("ai.gemini.api-key", "").trim();
        if (apiKey.isEmpty()) {
            throw new IllegalStateException(
                    "Gemini is not configured. Add ai.gemini.api-key to src/main/resources/db.properties.");
        }

        String model = config.getProperty("ai.gemini.model", DEFAULT_MODEL).trim();
        if (!MODEL_NAME.matcher(model).matches()) {
            throw new IllegalStateException("The ai.gemini.model value in db.properties is invalid.");
        }

        JsonObject systemInstruction = new JsonObject();
        JsonArray systemParts = new JsonArray();
        JsonObject systemText = new JsonObject();
        systemText.addProperty("text", """
                You are ANIPLATO's read-only support assistant for a surplus-food coordination app in Metro Manila.
                Explain app workflows clearly and concisely. Do not claim to access account data, change records, or take actions.
                Use only these app facts; do not invent features or policies:
                - Food groups offer on listings and donors offer on requests. Items in the viewer's own city appear first, but offers can be sent across cities.
                - The post's poster can accept one offer; all other Offered offers on that post are declined.
                - At pickup, the donor confirms release first, then the food group confirms receipt; the listing becomes Rescued.
                - Listing details include food, quantity, use-by time, pickup window, and storage condition. Donors need an active account and current documents.
                - Admins check registration documents for expiry and whether the name and city match.
                - The food group's report is due seven days after receipt. Reports summarize listed, rescued, expired, and cancelled food.
                - Rescue rate is rescued kilograms divided by posted kilograms for closed, non-cancelled posts. For a partially filled request, only the accepted offer's kilograms count.
                - Admins may dismiss a safety notice or suspend the responsible account with a recorded reason; overdue reports do not cause automatic suspension.
                If a detail is uncertain, say so and direct the user to an administrator.
                The current screen is %s. Use this only to make the answer relevant; no account or database data is provided.
                """.formatted(panel));
        systemParts.add(systemText);
        systemInstruction.add("parts", systemParts);

        JsonObject userPart = new JsonObject();
        userPart.addProperty("text", question);
        JsonArray userParts = new JsonArray();
        userParts.add(userPart);
        JsonObject userContent = new JsonObject();
        userContent.addProperty("role", "user");
        userContent.add("parts", userParts);
        JsonArray contents = new JsonArray();
        contents.add(userContent);

        JsonObject requestBody = new JsonObject();
        requestBody.add("systemInstruction", systemInstruction);
        requestBody.add("contents", contents);
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.3);
        generationConfig.addProperty("maxOutputTokens", 512);
        JsonObject thinkingConfig = new JsonObject();
        thinkingConfig.addProperty("thinkingLevel", "low");
        generationConfig.add("thinkingConfig", thinkingConfig);
        requestBody.add("generationConfig", generationConfig);

        return HttpRequest.newBuilder(URI.create(API_URL.formatted(model)))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString(), StandardCharsets.UTF_8))
                .build();
    }

    private Properties loadConfig() {
        try (InputStream input = GeminiAssistantService.class.getResourceAsStream(CONFIG_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing src/main/resources/db.properties.");
            }
            Properties config = new Properties();
            config.load(input);
            return config;
        } catch (IOException e) {
            throw new CompletionException("Could not read src/main/resources/db.properties.", e);
        }
    }

    private String readResponse(HttpResponse<String> response) {
        JsonObject body = parseObject(response.body());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(apiError(response.statusCode(), body));
        }

        JsonArray candidates = body.has("candidates") && body.get("candidates").isJsonArray()
                ? body.getAsJsonArray("candidates") : new JsonArray();
        if (candidates.isEmpty() || !candidates.get(0).isJsonObject()) {
            throw new IllegalStateException("Gemini returned no answer. Try asking in a different way.");
        }

        JsonObject candidate = candidates.get(0).getAsJsonObject();
        JsonElement contentElement = candidate.get("content");
        if (contentElement == null || !contentElement.isJsonObject()) {
            throw new IllegalStateException("Gemini returned an unreadable answer. Please try again.");
        }

        JsonElement partsElement = contentElement.getAsJsonObject().get("parts");
        if (partsElement == null || !partsElement.isJsonArray()) {
            throw new IllegalStateException("Gemini returned an unreadable answer. Please try again.");
        }

        StringBuilder answer = new StringBuilder();
        for (JsonElement part : partsElement.getAsJsonArray()) {
            if (part.isJsonObject() && part.getAsJsonObject().has("text")) {
                JsonObject textPart = part.getAsJsonObject();
                JsonElement thought = textPart.get("thought");
                if (thought != null && thought.isJsonPrimitive() && thought.getAsBoolean()) continue;
                if (!answer.isEmpty()) answer.append('\n');
                answer.append(textPart.get("text").getAsString());
            }
        }
        if (answer.isEmpty()) {
            throw new IllegalStateException("Gemini returned an empty answer. Please try again.");
        }
        return answer.toString().trim();
    }

    private JsonObject parseObject(String json) {
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (parsed.isJsonObject()) return parsed.getAsJsonObject();
        } catch (JsonSyntaxException e) {
            throw new IllegalStateException("Gemini returned an invalid response.", e);
        }
        throw new IllegalStateException("Gemini returned an invalid response.");
    }

    private String apiError(int statusCode, JsonObject body) {
        JsonElement errorElement = body.get("error");
        if (errorElement != null && errorElement.isJsonObject()) {
            JsonElement message = errorElement.getAsJsonObject().get("message");
            if (message != null && message.isJsonPrimitive()) {
                String details = message.getAsString().trim();
                if (!details.isEmpty()) return "Gemini request failed (HTTP " + statusCode + "): " + details;
            }
        }
        return "Gemini request failed with HTTP " + statusCode + ". Check the API key and network connection.";
    }

    public static String explainFailure(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        if (cause instanceof HttpConnectTimeoutException || cause instanceof ConnectException
                || cause instanceof UnknownHostException) {
            return "Could not connect to Google Gemini. Check your internet connection, firewall, or proxy settings.";
        }
        if (cause instanceof HttpTimeoutException) {
            return "Gemini did not respond within " + REQUEST_TIMEOUT.toSeconds()
                    + " seconds. Please retry; if it keeps happening, check your network or try again later.";
        }
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }
}
