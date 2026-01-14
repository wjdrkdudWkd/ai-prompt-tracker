package com.galoong.aiprompttracker.api.controller;

import com.galoong.aiprompttracker.config.properties.TrackingUiProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Controller to handle React SPA routing for embedded dashboard.
 *
 * <p><b>Problem Solved:</b>
 * This controller uses catch-all /** mapping to handle SPA client-side routes.
 * However, this caused a critical bug: static resources (_next/static/*.js) were
 * returning HTML instead of JavaScript, causing a white screen in consumer projects.
 *
 * <p><b>Solution:</b>
 * Spring MVC checks @GetMapping in controllers BEFORE resource handlers. Since we
 * need the catch-all /** to handle SPA routes (like /dashboard/, /functions/), we
 * can't avoid matching static resources at the mapping level.
 *
 * Instead, this controller explicitly checks for static resource patterns FIRST
 * (before any other logic) and returns 404 immediately. This prevents serving HTML
 * when the browser expects JavaScript/CSS/images/etc.
 *
 * <p><b>Request Flow:</b>
 * <ol>
 *   <li>/aiprompt-tracker/_next/** → Controller returns 404 (not HTML)</li>
 *   <li>/aiprompt-tracker/*.js, *.css, etc. → Controller returns 404 (not HTML)</li>
 *   <li>/aiprompt-tracker/api/** → Controller returns null (delegates to REST controllers)</li>
 *   <li>/aiprompt-tracker/, /dashboard/, /functions/ → Controller serves index.html (SPA routing)</li>
 * </ol>
 *
 * <p><b>Why 404 for Static Resources?</b>
 * In production builds (CI), React static files exist in META-INF/resources/ and
 * Spring Boot's default ResourceHttpRequestHandler serves them. If that handler
 * doesn't find the file, it returns 404.
 *
 * By returning 404 here (instead of HTML), we prevent the controller from interfering
 * with resource serving. If the file exists, Spring's resource handler serves it.
 * If it doesn't exist, we return 404 (not HTML pretending to be JavaScript).
 *
 * <p><b>Zero-Config Behavior:</b>
 * - With frontend built: Static resources served normally, SPA routes work
 * - Without frontend built: Static resources 404, SPA routes fall back to dashboard-mvp.html
 *
 * <p><b>Note:</b> This class is registered as a bean by ApiAutoConfiguration.
 * The @Controller annotation is still required for Spring MVC request mapping.
 */
@Controller
public class UiRedirectController {

    private final TrackingUiProperties uiProperties;

    public UiRedirectController(TrackingUiProperties uiProperties) {
        this.uiProperties = uiProperties;
    }

    /**
     * SPA fallback: Forward all non-API, non-static requests to dashboard HTML
     *
     * This handles:
     * - /aiprompt-tracker/ → index.html (React, preferred) or dashboard-mvp.html (vanilla fallback)
     * - /aiprompt-tracker/dashboard/ → index.html (React Router takes over)
     * - /aiprompt-tracker/functions/ → index.html
     * etc.
     *
     * Excludes:
     * - /aiprompt-tracker/api/** (handled by REST controllers)
     * - *.js, *.css, *.png, etc. (static assets)
     *
     * Zero-Config Principle:
     * - If index.html exists (React dashboard built): use it
     * - If index.html missing (no frontend build): fall back to dashboard-mvp.html
     */
    @GetMapping(
        value = {
            "/aiprompt-tracker",
            "/aiprompt-tracker/",
            "/aiprompt-tracker/**"
        }
    )
    public Object handleSpaRouting(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String path = request.getRequestURI();

        // CRITICAL: DO NOT HANDLE STATIC RESOURCES
        // Return 404 immediately for any static resource request
        // This prevents the controller from serving HTML when browser expects JavaScript/CSS/etc.

        // 1. _next/** paths (Next.js static output) - Block at controller level
        if (path.startsWith("/aiprompt-tracker/_next/")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        // 2. Files with extensions - check if it's a static asset
        if (path.contains(".") && !path.endsWith("/")) {
            int lastSlash = path.lastIndexOf('/');
            int lastDot = path.lastIndexOf('.');
            if (lastDot > lastSlash) {
                String extension = path.substring(lastDot).toLowerCase();
                // List of static asset extensions that MUST NOT return HTML
                if (extension.matches("\\.(js|css|map|txt|ico|png|jpg|jpeg|svg|gif|woff2|woff|ttf|json|xml|html|htm)")) {
                    // This is a static asset request - return 404 instead of HTML
                    // In production with frontend built, these files WILL exist in META-INF/resources
                    // and Spring Boot's default ResourceHttpRequestHandler will serve them
                    // But if we got here, it means the resource doesn't exist
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return null;
                }
            }
        }

        // 3. DO NOT intercept API requests
        if (path.startsWith("/aiprompt-tracker/api/")) {
            return null; // Let Spring MVC continue to REST controllers
        }

        // 4. Check if UI is disabled (API-only mode)
        if (!uiProperties.isEnabled()) {
            String message = "<!DOCTYPE html>" +
                "<html><head><title>AI Prompt Tracker - API Only Mode</title></head>" +
                "<body style='font-family: system-ui; padding: 40px; max-width: 800px; margin: 0 auto;'>" +
                "<h1>AI Prompt Tracker - API Only Mode</h1>" +
                "<p>The embedded dashboard is disabled.</p>" +
                "<p>API endpoints are still accessible at <code>/aiprompt-tracker/api/**</code></p>" +
                "<h2>To enable the embedded UI:</h2>" +
                "<pre style='background: #f5f5f5; padding: 15px; border-radius: 5px;'>" +
                "ai-prompts:\n" +
                "  tracking:\n" +
                "    ui:\n" +
                "      enabled: true  # Enable embedded dashboard" +
                "</pre>" +
                "<h2>For standalone UI deployment:</h2>" +
                "<p>Deploy the frontend separately and set <code>NEXT_PUBLIC_API_BASE_URL</code> to point to this server.</p>" +
                "</body></html>";

            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.TEXT_HTML)
                .body(message);
        }

        // All SPA routes (no extension, or directory paths): serve dashboard HTML
        // Prefer React dashboard (index.html), fall back to vanilla (dashboard-mvp.html)
        Resource indexHtml = new ClassPathResource("META-INF/resources/aiprompt-tracker/index.html");
        if (indexHtml.exists()) {
            // React dashboard available
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(indexHtml);
        }

        // Fallback to vanilla dashboard (always present)
        Resource fallbackHtml = new ClassPathResource("META-INF/resources/aiprompt-tracker/dashboard-mvp.html");
        if (fallbackHtml.exists()) {
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(fallbackHtml);
        }

        // Neither dashboard available (should never happen)
        return ResponseEntity.notFound().build();
    }
}
