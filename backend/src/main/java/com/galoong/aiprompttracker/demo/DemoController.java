package com.galoong.aiprompttracker.demo;

import com.galoong.aiprompttracker.core.annotation.AIPrompt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Demo controller showcasing AI Prompt Tracker functionality.
 *
 * <p>All methods annotated with @AIPrompt will automatically track:
 * <ul>
 *   <li>Execution metadata (method name, category, timestamps)</li>
 *   <li>HTTP calls made within the execution context</li>
 *   <li>AI API usage metrics (tokens, costs, models)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/demo")
@RequiredArgsConstructor
public class DemoController {

    private final WebClient.Builder webClientBuilder;

    @Value("${server.port:8080}")
    private int serverPort;

    /**
     * Simple demo endpoint that triggers tracking.
     */
    @GetMapping("/trigger")
    @AIPrompt(name = "demo-trigger", category = "demo")
    public Map<String, Object> triggerTracking() {
        log.info("Demo tracking triggered");
        return Map.of(
            "status", "success",
            "message", "Tracking triggered successfully",
            "timestamp", LocalDateTime.now().toString(),
            "info", "Check /aiprompt-tracker for dashboard"
        );
    }

    /**
     * Simulates a chat completion call to mock OpenAI API.
     * This demonstrates real HTTP call tracking.
     */
    @PostMapping("/chat")
    @AIPrompt(name = "demo-chat", category = "chat")
    public Mono<Map<String, Object>> demoChat(@RequestBody Map<String, String> request) {
        String prompt = request.getOrDefault("prompt", "Hello!");
        log.info("Demo chat called with prompt: {}", prompt);

        // Call mock OpenAI API
        WebClient webClient = webClientBuilder
            .baseUrl("http://localhost:" + serverPort)
            .build();

        return webClient.post()
            .uri("/mock/openai/v1/chat/completions")
            .bodyValue(Map.of(
                "model", "gpt-3.5-turbo",
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                )
            ))
            .retrieve()
            .bodyToMono(Map.class)
            .map(apiResponse -> Map.of(
                "status", "success",
                "prompt", prompt,
                "api_response", apiResponse,
                "timestamp", LocalDateTime.now().toString(),
                "tracked", "This HTTP call was automatically tracked by @AIPrompt"
            ))
            .onErrorResume(error -> {
                log.error("Error calling mock OpenAI API", error);
                return Mono.just(Map.of(
                    "status", "error",
                    "message", error.getMessage()
                ));
            });
    }


    /**
     * Demonstrates batch processing with tracking.
     */
    @PostMapping("/batch")
    @AIPrompt(name = "demo-batch", category = "batch")
    public Map<String, Object> demoBatch(@RequestBody Map<String, List<String>> request) {
        List<String> prompts = request.getOrDefault("prompts", List.of("Hello", "Hi", "Hey"));
        log.info("Demo batch processing {} prompts", prompts.size());

        List<Map<String, String>> results = prompts.stream()
                .map(prompt -> Map.of(
                    "prompt", prompt,
                    "response", generateMockResponse(prompt)
                ))
                .toList();

        return Map.of(
            "status", "success",
            "processed", prompts.size(),
            "results", results,
            "timestamp", LocalDateTime.now().toString()
        );
    }

    /**
     * Health check endpoint (not tracked).
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
            "status", "ok",
            "message", "Demo controller is running",
            "dashboard", "http://localhost:8080/aiprompt-tracker"
        );
    }

    private String generateMockResponse(String prompt) {
        String[] responses = {
            "I understand your request: " + prompt,
            "That's an interesting question about: " + prompt,
            "Let me help you with: " + prompt,
            "Based on your input '" + prompt + "', here's what I think...",
            "Regarding '" + prompt + "', I can tell you that..."
        };
        return responses[ThreadLocalRandom.current().nextInt(responses.length)];
    }
}
