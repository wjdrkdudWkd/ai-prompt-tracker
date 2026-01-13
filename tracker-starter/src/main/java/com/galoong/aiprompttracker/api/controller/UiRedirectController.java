package com.galoong.aiprompttracker.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

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
     * SPA fallback: Forward all non-API, non-static requests to index.html
     *
     * This handles:
     * - /aiprompt-tracker/ → index.html
     * - /aiprompt-tracker/dashboard/ → index.html (React Router takes over)
     * - /aiprompt-tracker/functions/ → index.html
     * etc.
     *
     * Excludes:
     * - /aiprompt-tracker/api/** (handled by REST controllers)
     * - *.js, *.css, *.png, etc. (static assets)
     */
    @GetMapping({
        "/aiprompt-tracker",
        "/aiprompt-tracker/",
        "/aiprompt-tracker/**"
    })
    public String handleSpaRouting(HttpServletRequest request) {
        String path = request.getRequestURI();

        // DO NOT intercept API requests
        if (path.startsWith("/aiprompt-tracker/api/")) {
            return null; // Let Spring MVC continue to REST controllers
        }

        // DO NOT intercept static assets (these are served by ResourceHttpRequestHandler)
        // Files with extensions are assumed to be static assets
        if (path.contains(".") && !path.endsWith("/")) {
            // Has extension and not a directory - let static resource handler serve it
            int lastSlash = path.lastIndexOf('/');
            int lastDot = path.lastIndexOf('.');
            if (lastDot > lastSlash) {
                // Extension comes after last slash - this is a file request
                return null;
            }
        }

        // All other paths: forward to React SPA index.html
        return "forward:/aiprompt-tracker/index.html";
    }
}
