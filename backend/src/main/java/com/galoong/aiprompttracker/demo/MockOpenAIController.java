package com.galoong.aiprompttracker.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mock OpenAI API controller for demo purposes.
 *
 * <p>Provides fake OpenAI-compatible responses to demonstrate tracking without API keys.
 */
@Slf4j
@RestController
@RequestMapping("/mock/openai")
public class MockOpenAIController {

    @PostMapping("/v1/chat/completions")
    public Map<String, Object> chatCompletions(@RequestBody Map<String, Object> request) {
        log.info("Mock OpenAI chat completions called");

        String model = (String) request.getOrDefault("model", "gpt-3.5-turbo");

        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) request.getOrDefault("messages", List.of());

        String userMessage = messages.isEmpty() ? "Hello" :
            messages.get(messages.size() - 1).getOrDefault("content", "Hello");

        String response = generateMockResponse(userMessage);

        int promptTokens = estimateTokens(userMessage);
        int completionTokens = estimateTokens(response);

        return Map.of(
            "id", "chatcmpl-" + UUID.randomUUID().toString().substring(0, 8),
            "object", "chat.completion",
            "created", Instant.now().getEpochSecond(),
            "model", model,
            "choices", List.of(
                Map.of(
                    "index", 0,
                    "message", Map.of(
                        "role", "assistant",
                        "content", response
                    ),
                    "finish_reason", "stop"
                )
            ),
            "usage", Map.of(
                "prompt_tokens", promptTokens,
                "completion_tokens", completionTokens,
                "total_tokens", promptTokens + completionTokens
            )
        );
    }

    @PostMapping("/v1/completions")
    public Map<String, Object> completions(@RequestBody Map<String, Object> request) {
        log.info("Mock OpenAI completions called");

        String model = (String) request.getOrDefault("model", "text-davinci-003");
        String prompt = (String) request.getOrDefault("prompt", "Hello");

        String response = generateMockResponse(prompt);

        int promptTokens = estimateTokens(prompt);
        int completionTokens = estimateTokens(response);

        return Map.of(
            "id", "cmpl-" + UUID.randomUUID().toString().substring(0, 8),
            "object", "text_completion",
            "created", Instant.now().getEpochSecond(),
            "model", model,
            "choices", List.of(
                Map.of(
                    "text", response,
                    "index", 0,
                    "finish_reason", "stop"
                )
            ),
            "usage", Map.of(
                "prompt_tokens", promptTokens,
                "completion_tokens", completionTokens,
                "total_tokens", promptTokens + completionTokens
            )
        );
    }

    private String generateMockResponse(String prompt) {
        String[] responses = {
            "This is a mock AI response from the demo API. In a real scenario, this would be an actual OpenAI response.",
            "Hello! I'm a simulated AI assistant created for demonstration purposes. How can I help you today?",
            "Thank you for your question. This is a mock response generated to demonstrate the AI Prompt Tracker functionality.",
            "I understand your request. This simulated response helps showcase how the tracker captures and stores AI interactions.",
            "Great question! This demo response illustrates the tracking of API calls, tokens, and execution flows."
        };

        int index = Math.abs(prompt.hashCode() % responses.length);
        return responses[index];
    }

    private int estimateTokens(String text) {
        // Rough estimation: ~4 characters per token
        return Math.max(1, text.length() / 4);
    }
}
