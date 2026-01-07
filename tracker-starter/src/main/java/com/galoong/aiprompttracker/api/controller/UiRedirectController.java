package com.galoong.aiprompttracker.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to handle UI root path redirects.
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
