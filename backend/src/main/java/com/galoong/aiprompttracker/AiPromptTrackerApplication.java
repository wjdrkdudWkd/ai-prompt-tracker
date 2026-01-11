package com.galoong.aiprompttracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI Prompt Tracker Demo Application
 *
 * <p>Demo application showcasing AI Prompt Tracker Spring Boot Starter.
 * Uses @AIPrompt annotation to automatically track AI API calls.
 *
 * <p><b>Plug-and-Play Experience:</b>
 * No explicit @EnableJpaRepositories configuration required. The starter automatically
 * registers its base package into Spring Boot's AutoConfigurationPackages, enabling
 * automatic discovery of both starter and application repositories.
 *
 * @author galoong
 * @version 1.0.0
 */
@SpringBootApplication
public class AiPromptTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiPromptTrackerApplication.class, args);
    }
}
