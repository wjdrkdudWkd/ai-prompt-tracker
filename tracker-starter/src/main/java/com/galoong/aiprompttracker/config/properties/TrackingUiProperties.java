package com.galoong.aiprompttracker.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for AI Prompt Tracker embedded UI.
 *
 * <p>Controls whether the embedded React dashboard is served.
 * When disabled, only the API endpoints remain accessible.
 *
 * <p><b>Usage:</b>
 * <pre>
 * # application.yml
 * ai-prompts:
 *   tracking:
 *     ui:
 *       enabled: true  # default: serve embedded dashboard
 * </pre>
 *
 * <p><b>Embedded Mode (default):</b>
 * - enabled: true
 * - Dashboard served at /aiprompt-tracker/
 * - API at /aiprompt-tracker/api/**
 *
 * <p><b>API-Only Mode:</b>
 * - enabled: false
 * - Dashboard disabled (404 or info page)
 * - API still accessible at /aiprompt-tracker/api/**
 * - Use with standalone UI deployment
 *
 * @see com.galoong.aiprompttracker.api.controller.UiRedirectController
 */
@ConfigurationProperties(prefix = "ai-prompts.tracking.ui")
public class TrackingUiProperties {

    /**
     * Enable or disable the embedded React dashboard.
     *
     * <p>Default: {@code true} (embedded UI enabled)
     *
     * <p>When {@code false}:
     * <ul>
     *   <li>Embedded dashboard is not served</li>
     *   <li>Accessing /aiprompt-tracker/ returns 404 or info message</li>
     *   <li>API endpoints remain fully functional</li>
     *   <li>Suitable for standalone UI deployments</li>
     * </ul>
     */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
