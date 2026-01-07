package com.galoong.aiprompttracker.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Web MVC configuration for serving the embedded AI Prompt Tracker UI.
 *
 * Serves static assets at /aiprompt-tracker/** with SPA fallback routing.
 * API requests under /aiprompt-tracker/api/** are not affected.
 */
@Slf4j
@Configuration
public class AiPromptTrackerWebMvcConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static UI assets at /aiprompt-tracker/**
        registry.addResourceHandler("/aiprompt-tracker/**")
                .addResourceLocations("classpath:/META-INF/resources/aiprompt-tracker/")
                .resourceChain(true)
                .addResolver(new SpaFallbackResourceResolver());

        log.info("AI Prompt Tracker: Registered UI resource handler at /aiprompt-tracker/**");
    }

    /**
     * Custom resource resolver that implements SPA fallback routing.
     * Returns index.html for non-existent resources EXCEPT:
     * - API paths (/aiprompt-tracker/api/**)
     * - Static assets (*.js, *.css, *.png, etc.)
     */
    private static class SpaFallbackResourceResolver extends PathResourceResolver {

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            // Handle root path (empty or ".")
            if (resourcePath.isEmpty() || ".".equals(resourcePath)) {
                Resource indexHtml = new ClassPathResource("/META-INF/resources/aiprompt-tracker/index.html");
                if (indexHtml.exists()) {
                    log.debug("SPA fallback: serving index.html for root path");
                    return indexHtml;
                }
            }

            Resource requestedResource = location.createRelative(resourcePath);

            // If resource exists and is readable, return it
            if (requestedResource.exists() && requestedResource.isReadable()) {
                return requestedResource;
            }

            // Don't fallback for API requests - let them 404 or be handled by controllers
            if (resourcePath.startsWith("api/")) {
                return null;
            }

            // Don't fallback for static asset extensions
            if (isStaticAsset(resourcePath)) {
                return null;
            }

            // SPA fallback: return index.html for client-side routing
            Resource indexHtml = new ClassPathResource("/META-INF/resources/aiprompt-tracker/index.html");
            if (indexHtml.exists()) {
                log.debug("SPA fallback: serving index.html for path: {}", resourcePath);
                return indexHtml;
            }

            return null;
        }

        private boolean isStaticAsset(String path) {
            String lowerPath = path.toLowerCase();
            return lowerPath.endsWith(".js") ||
                   lowerPath.endsWith(".css") ||
                   lowerPath.endsWith(".png") ||
                   lowerPath.endsWith(".jpg") ||
                   lowerPath.endsWith(".jpeg") ||
                   lowerPath.endsWith(".gif") ||
                   lowerPath.endsWith(".svg") ||
                   lowerPath.endsWith(".woff") ||
                   lowerPath.endsWith(".woff2") ||
                   lowerPath.endsWith(".ttf") ||
                   lowerPath.endsWith(".eot") ||
                   lowerPath.endsWith(".ico") ||
                   lowerPath.endsWith(".json") ||
                   lowerPath.endsWith(".map");
        }
    }
}
