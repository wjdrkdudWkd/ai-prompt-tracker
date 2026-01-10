package com.galoong.aiprompttracker.config;

import com.galoong.aiprompttracker.tracking.aspect.AIPromptAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * AOP configuration for AI Prompt Tracker.
 *
 * <p>Enables AspectJ auto-proxy and registers the @AIPrompt aspect.
 */
@Slf4j
@Configuration
@EnableAspectJAutoProxy
public class AopConfig {

    /**
     * AIPromptAspect - AOP aspect for tracking @AIPrompt annotated methods
     */
    @Bean
    @ConditionalOnMissingBean
    public AIPromptAspect aiPromptAspect() {
        log.info("Registering AIPromptAspect for @AIPrompt method tracking");
        return new AIPromptAspect();
    }
}
