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

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Controller to handle React SPA routing for embedded dashboard.
 *
 * <p><b>Critical White-Screen Bug Fix:</b>
 * Previous implementation used catch-all /** mapping, which matched ALL requests
 * including static resources (_next/static/*.js). Even though the controller tried
 * to return 404 for static files, Spring MVC had already chosen this handler,
 * preventing ResourceHttpRequestHandler from serving the actual files.
 *
 * <p><b>Root Cause:</b>
 * Spring MVC handler matching order:
 * 1. Controllers (@RequestMapping/@GetMapping) - checked FIRST
 * 2. ResourceHttpRequestHandler - checked SECOND
 *
 * A catch-all /** in a controller matches everything, so ResourceHandler never runs.
 * Returning 404 from the controller doesn't delegate - it just terminates with 404.
 *
 * <p><b>Solution:</b>
 * Use explicit path mappings for SPA routes ONLY:
 * - /aiprompt-tracker (root)
 * - /aiprompt-tracker/ (root with slash)
 * - /aiprompt-tracker/dashboard, /aiprompt-tracker/dashboard/**
 * - /aiprompt-tracker/functions, /aiprompt-tracker/functions/**
 * - /aiprompt-tracker/calls, /aiprompt-tracker/calls/**
 * - /aiprompt-tracker/providers, /aiprompt-tracker/providers/**
 * - /aiprompt-tracker/settings, /aiprompt-tracker/settings/**
 *
 * By NOT using catch-all /**, we let Spring Boot's default ResourceHttpRequestHandler
 * serve static files (_next/**, *.js, *.css) without interference.
 *
 * <p><b>Request Flow:</b>
 * <ol>
 *   <li>/aiprompt-tracker/_next/static/*.js → NO MATCH → ResourceHandler serves file → 200 OK (JS)</li>
 *   <li>/aiprompt-tracker/*.js, *.css → NO MATCH → ResourceHandler serves file → 200 OK</li>
 *   <li>/aiprompt-tracker/api/** → NO MATCH → REST Controllers handle → 200 OK (JSON)</li>
 *   <li>/aiprompt-tracker/dashboard/ → MATCH → This controller → HTML (SPA)</li>
 * </ol>
 *
 * <p><b>Trade-off:</b>
 * We must explicitly list all SPA routes. If a new route is added to the React app,
 * it must be added here. However, this is acceptable because:
 * - Routes are stable (dashboard, functions, calls, providers, settings)
 * - Explicit > Magic (clear what paths are handled)
 * - Prevents bugs from catch-all matching unintended paths
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
     * SPA fallback: Serve index.html for known SPA routes
     *
     * <p>This explicitly maps ONLY the SPA routes, NOT a catch-all.
     * Static resources (_next/**, *.js, *.css) are NOT matched here,
     * allowing Spring Boot's ResourceHttpRequestHandler to serve them.
     *
     * <p>Mapped routes:
     * - /aiprompt-tracker (root, redirect to /dashboard)
     * - /aiprompt-tracker/ (root with slash)
     * - /aiprompt-tracker/dashboard/** (main dashboard and sub-routes)
     * - /aiprompt-tracker/functions/** (functions list and detail)
     * - /aiprompt-tracker/calls/** (calls list)
     * - /aiprompt-tracker/providers/** (providers list)
     * - /aiprompt-tracker/settings/** (settings page)
     *
     * <p>Zero-Config Principle:
     * - If index.html exists (React dashboard built): use it
     * - If index.html missing (no frontend build): fall back to dashboard-mvp.html
     */
    @GetMapping(
        value = {
            "/aiprompt-tracker",
            "/aiprompt-tracker/",
            "/aiprompt-tracker/dashboard",
            "/aiprompt-tracker/dashboard/**",
            "/aiprompt-tracker/functions",
            "/aiprompt-tracker/functions/**",
            "/aiprompt-tracker/calls",
            "/aiprompt-tracker/calls/**",
            "/aiprompt-tracker/providers",
            "/aiprompt-tracker/providers/**",
            "/aiprompt-tracker/settings",
            "/aiprompt-tracker/settings/**"
        }
    )
    public Object handleSpaRouting(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // No more path checking needed - this method only matches SPA routes now
        // Static resources (_next/**, *.js, *.css) never reach here

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
