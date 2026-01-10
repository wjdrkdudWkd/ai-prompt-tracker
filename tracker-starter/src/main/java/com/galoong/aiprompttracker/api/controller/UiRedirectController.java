package com.galoong.aiprompttracker.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to handle UI root path redirects.
 *
 * <p><b>Note:</b> This class is registered as a bean by ApiAutoConfiguration.
 * The @Controller annotation is still required for Spring MVC request mapping.
 */
@Controller
public class UiRedirectController {

    /**
     * Redirect /aiprompt-tracker/ to index.html
     */
    @GetMapping("/aiprompt-tracker/")
    public String redirectToIndex() {
        return "forward:/aiprompt-tracker/index.html";
    }

    /**
     * Redirect /aiprompt-tracker (without trailing slash) to index.html
     */
    @GetMapping("/aiprompt-tracker")
    public String redirectToIndexNoSlash() {
        return "forward:/aiprompt-tracker/index.html";
    }
}
