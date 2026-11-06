package com.aniplato.service;

import com.aniplato.util.AppConfig;
import com.aniplato.util.KnowledgeBase;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/** Provides read-only, screen-aware help using the Gemini API. */
public final class GeminiAssistantService {

    private static final String DEFAULT_MODEL = "gemini-3.5-flash-lite";
    private static final int MAX_HISTORY_TURNS = 6;
    private static final String NL = System.lineSeparator();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
    private static final Pattern MODEL_NAME = Pattern.compile("[A-Za-z0-9._-]+");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /** One earlier message of the chat, kept so a follow-up such as "and then?" makes sense. */
    public record Turn(boolean fromUser, String text) { }

    /**
     * Asks Gemini on a background thread. {@code screen} is the screen key now open and {@code role} the signed-in role;
     * they only make the answer relevant (no account or database data is ever sent). {@code history} holds the earlier
     * messages, oldest first; only the last few are sent.
     */
    public CompletableFuture<String> answer(String screen, String role, String question, List<Turn> history) {
        return CompletableFuture.supplyAsync(() -> createRequest(screen, role, question, history))
                .thenCompose(request -> HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                .thenApply(this::readResponse);
    }

    private HttpRequest createRequest(String screen, String role, String question, List<Turn> history) {
        String apiKey = AppConfig.get("ai.gemini.api-key", "");   // AppConfig also finds a packaged app's settings file
        if (apiKey.isEmpty()) {
            throw new IllegalStateException(
                    "The assistant has no Gemini key yet. Add ai.gemini.api-key to the db.properties settings file.");
        }

        String model = AppConfig.get("ai.gemini.model", DEFAULT_MODEL);
        if (!MODEL_NAME.matcher(model).matches()) {
            throw new IllegalStateException("The ai.gemini.model value in db.properties is invalid.");
        }

        return HttpRequest.newBuilder(URI.create(API_URL.formatted(model)))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(buildBody(screen, role, question, history).toString(), StandardCharsets.UTF_8))
                .build();
    }

    /** The request body: the instructions and knowledge as the system prompt, then the recent chat, then the question. */
    static JsonObject buildBody(String screen, String role, String question, List<Turn> history) {
        JsonObject body = new JsonObject();
        body.add("systemInstruction", content(null, systemPrompt(screen, role)));

        JsonArray contents = new JsonArray();
        List<Turn> recent = history == null ? List.of()
                : history.subList(Math.max(0, history.size() - MAX_HISTORY_TURNS), history.size());
        int first = 0;
        while (first < recent.size() && !recent.get(first).fromUser()) first++;   // a chat must open with the user
        for (Turn turn : recent.subList(first, recent.size())) {
            contents.add(content(turn.fromUser() ? "user" : "model", turn.text()));
        }
        contents.add(content("user", question));
        body.add("contents", contents);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.3);
        generationConfig.addProperty("maxOutputTokens", 2048);   // room for the model's own thinking as well as a short answer
        JsonObject thinkingConfig = new JsonObject();
        thinkingConfig.addProperty("thinkingLevel", "low");
        generationConfig.add("thinkingConfig", thinkingConfig);
        body.add("generationConfig", generationConfig);
        return body;
    }

    /** The instructions, then the knowledge fenced off, then where the user is. Nothing else about the user. */
    static String systemPrompt(String screen, String role) {
        String where = screen == null || screen.isBlank() ? "no particular" : screen.replace('-', ' ');
        String who = role == null || role.isBlank() ? "unknown role" : role;
        return String.join(NL,
                KnowledgeBase.instructions().strip(),
                "",
                "KNOWLEDGE (the only facts you may use):",
                "<knowledge>",
                KnowledgeBase.knowledge(),
                "</knowledge>",
                "",
                "CONTEXT (not a question): the user has the \"" + where + "\" screen open and is signed in as: " + who
                        + ". No account or database data is provided.");
    }

    private static JsonObject content(String role, String text) {
        JsonObject part = new JsonObject();
        part.addProperty("text", text);
        JsonArray parts = new JsonArray();
        parts.add(part);
        JsonObject content = new JsonObject();
        if (role != null) content.addProperty("role", role);
        content.add("parts", parts);
        return content;
    }

    private String readResponse(HttpResponse<String> response) {
        return parseAnswer(response.statusCode(), response.body());
    }

    /** The answer text in a Gemini reply, or an {@link IllegalStateException} that says plainly why there is none. */
    static String parseAnswer(int statusCode, String json) {
        JsonObject body = parseObject(json);
        if (statusCode < 200 || statusCode >= 300) {
            throw new IllegalStateException(apiError(statusCode, body));
        }

        JsonArray candidates = body.has("candidates") && body.get("candidates").isJsonArray()
                ? body.getAsJsonArray("candidates") : new JsonArray();
        if (candidates.isEmpty() || !candidates.get(0).isJsonObject()) {
            throw new IllegalStateException(noAnswer(promptBlockReason(body), null));
        }

        JsonObject candidate = candidates.get(0).getAsJsonObject();
        String finish = text(candidate.get("finishReason"));
        StringBuilder answer = new StringBuilder();
        JsonElement contentElement = candidate.get("content");
        JsonElement partsElement = contentElement != null && contentElement.isJsonObject()
                ? contentElement.getAsJsonObject().get("parts") : null;
        if (partsElement != null && partsElement.isJsonArray()) {
            for (JsonElement part : partsElement.getAsJsonArray()) {
                if (part.isJsonObject() && part.getAsJsonObject().has("text")) {
                    JsonObject textPart = part.getAsJsonObject();
                    JsonElement thought = textPart.get("thought");
                    if (thought != null && thought.isJsonPrimitive() && thought.getAsBoolean()) continue;
                    if (!answer.isEmpty()) answer.append('\n');
                    answer.append(textPart.get("text").getAsString());
                }
            }
        }
        if (answer.isEmpty()) throw new IllegalStateException(noAnswer(null, finish));
        return plain(answer.toString());
    }

    /** The chat bubbles show plain text, so Markdown the model slipped in is tidied: no bold marks, bullets as dots. */
    static String plain(String markdown) {
        return markdown.replace("**", "").replace("__", "")
                .replaceAll("(?m)^\\s*[*-]\\s+", "\u2022 ")
                .replaceAll("(?m)^#{1,6}\\s+", "")
                .replace("`", "")
                .trim();
    }

    private static String noAnswer(String blockReason, String finishReason) {
        if (blockReason != null) {
            return "Gemini would not answer that (" + blockReason + "). Try asking about using ANIPLATO.";
        }
        if ("MAX_TOKENS".equals(finishReason)) {
            return "Gemini ran out of room before it started answering (MAX_TOKENS). Try a shorter question.";
        }
        if (finishReason != null && !"STOP".equals(finishReason)) {
            return "Gemini stopped without an answer (" + finishReason + "). Try asking in a different way.";
        }
        return "Gemini returned an empty answer (" + (finishReason == null ? "no reason given" : finishReason)
                + "). Please try again.";
    }

    private static String promptBlockReason(JsonObject body) {
        JsonElement feedback = body.get("promptFeedback");
        return feedback != null && feedback.isJsonObject() ? text(feedback.getAsJsonObject().get("blockReason")) : null;
    }

    private static String text(JsonElement element) {
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    private static JsonObject parseObject(String json) {
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (parsed.isJsonObject()) return parsed.getAsJsonObject();
        } catch (JsonSyntaxException e) {
            throw new IllegalStateException("Gemini returned an invalid response.", e);
        }
        throw new IllegalStateException("Gemini returned an invalid response.");
    }

    private static String apiError(int statusCode, JsonObject body) {
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
