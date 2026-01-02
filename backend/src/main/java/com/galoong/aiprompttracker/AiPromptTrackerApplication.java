package com.galoong.aiprompttracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * AI Prompt Tracker Application
 *
 * 모든 AI API(OpenAI, Anthropic Claude, Google Gemini 등)를
 * Swagger처럼 관리하는 통합 개발자 도구
 *
 * @author galoong
 * @version 1.0.0
 */
@SpringBootApplication
@EnableCaching
@EnableJpaRepositories
public class AiPromptTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiPromptTrackerApplication.class, args);
    }
}
