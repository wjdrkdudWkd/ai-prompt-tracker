package com.galoong.aiprompttracker.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Controller to handle React SPA routing for embedded dashboard.
 *
 * <p><b>Strategy:</b>
 * - /aiprompt-tracker/ → forward to index.html
 * - /aiprompt-tracker/dashboard/, /functions/, etc. → forward to index.html (SPA routing)
 * - /aiprompt-tracker/api/** → NOT handled here (REST API)
 * - Static assets (.js, .css, .png, etc.) → served by Spring Boot static resource handling
 *
 * <p><b>Rationale:</b>
 * Next.js static export with trailingSlash:true generates /dashboard/index.html, /functions/index.html.
 * Spring Boot serves these files directly. This controller handles fallback for:
 * 1. Root path /aiprompt-tracker/
 * 2. Any path without extension (SPA client-side routes)
 *
 * <p><b>Note:</b> This class is registered as a bean by ApiAutoConfiguration.
 * The @Controller annotation is still required for Spring MVC request mapping.
 */
@Controller
public class UiRedirectController {

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
    public Object handleSpaRouting(HttpServletRequest request) throws IOException {
        String path = request.getRequestURI();

        // DO NOT intercept API requests
        if (path.startsWith("/aiprompt-tracker/api/")) {
            return null; // Let Spring MVC continue to REST controllers
        }

        // DO NOT intercept static assets (let Spring Boot serve them)
        // Files with extensions OTHER than HTML routes are static assets
        if (path.contains(".") && !path.endsWith("/")) {
            int lastSlash = path.lastIndexOf('/');
            int lastDot = path.lastIndexOf('.');
            if (lastDot > lastSlash) {
                // Has extension after last slash
                String extension = path.substring(lastDot);
                // Only intercept routes without extensions or directory-like paths
                // Let .js, .css, .png, .ico, etc. be served by static resource handler
                if (!extension.equals(".html") && !extension.equals(".htm")) {
                    return null; // Not an HTML file, let static handler serve it
                }
                // .html files: let them be served by static handler too
                return null;
            }
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
