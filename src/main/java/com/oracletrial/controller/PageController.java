package com.oracletrial.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the single-page web UI.
 *
 * <p>All actual work (upload, ask) happens through the REST APIs, called
 * from the page's own JavaScript. This controller only renders the page
 * shell, so it stays a plain Thymeleaf view - no React, no separate
 * frontend project.</p>
 */
@Controller
public class PageController {

    /**
     * Renders the main "Oracle's Trial" page.
     *
     * @return the name of the Thymeleaf template to render
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }
}
