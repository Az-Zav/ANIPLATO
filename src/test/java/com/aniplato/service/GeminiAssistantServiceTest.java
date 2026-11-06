package com.aniplato.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The assistant's setup problems and failure texts. Every case stops before a request is sent, so none needs the network. */
class GeminiAssistantServiceTest {

    private static final String KEY = "aniplato.ai.gemini.api-key";
    private static final String MODEL = "aniplato.ai.gemini.model";

    @AfterEach
    void clearSettings() {
        System.clearProperty(KEY);
        System.clearProperty(MODEL);
    }

    private String failureOf(CompletableFuture<String> future) {
        ExecutionException e = assertThrows(ExecutionException.class, future::get);
        return GeminiAssistantService.explainFailure(e);
    }

    @Test
    void withoutAKeyItSaysTheAssistantIsNotSetUp() {
        System.setProperty(KEY, " ");
        String text = failureOf(new GeminiAssistantService().answer("PROFILE", "How do I update my profile?"));
        assertTrue(text.contains("ai.gemini.api-key"), text);
    }

    @Test
    void aBadModelNameIsRefusedBeforeAnyRequest() {
        System.setProperty(KEY, "test-key-not-real");
        System.setProperty(MODEL, "bad model/name");
        String text = failureOf(new GeminiAssistantService().answer("PROFILE", "Hello"));
        assertTrue(text.contains("ai.gemini.model"), text);
    }

    @Test
    void noInternetIsExplainedInPlainWords() {
        assertTrue(GeminiAssistantService.explainFailure(new CompletionException(new UnknownHostException("x")))
                .contains("internet connection"));
        assertTrue(GeminiAssistantService.explainFailure(new ConnectException("refused")).contains("Could not connect"));
    }

    @Test
    void aSlowAnswerSaysHowLongItWaited() {
        assertTrue(GeminiAssistantService.explainFailure(new CompletionException(new HttpTimeoutException("t")))
                .contains("60 seconds"));
    }

    @Test
    void anyOtherFailureShowsItsMessageOrItsType() {
        assertTrue(GeminiAssistantService.explainFailure(new IllegalStateException("boom")).equals("boom"));
        assertTrue(GeminiAssistantService.explainFailure(new IllegalStateException()).equals("IllegalStateException"));
    }
}
