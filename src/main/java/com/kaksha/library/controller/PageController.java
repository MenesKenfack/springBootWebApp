package com.kaksha.library.controller;

import com.kaksha.library.service.LandingService;
import com.kaksha.library.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Controller for serving Thymeleaf view templates.
 * Handles all page routing for the web application.
 */
@Controller
@RequiredArgsConstructor
public class PageController {

    private final ResourceService resourceService;
    private final LandingService landingService;
    private static final int LANDING_LIMIT = 5;

    // Public pages
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("trendingResources", landingService.getTrendingNow(LANDING_LIMIT));
        model.addAttribute("bestSellingResources", landingService.getBestSelling(LANDING_LIMIT));
        model.addAttribute("popularResources", landingService.getPopularBooks(LANDING_LIMIT));
        model.addAttribute("newlyAddedResources", landingService.getNewlyAdded(LANDING_LIMIT));
        return "index";
    }

    @GetMapping("/index")
    public String indexPage() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    // Protected dashboard pages
    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/resources")
    public String resources(Model model) {
        List<String> authors = resourceService.getAvailableAuthors();
        model.addAttribute("authors", authors);
        return "resources";
    }

    @GetMapping("/purchases")
    public String purchases() {
        return "purchases";
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }

    @GetMapping("/my-list")
    public String myList() {
        return "my-list";
    }

    // Manager only pages
    @GetMapping("/analytics")
    public String analytics() {
        return "analytics";
    }

    @GetMapping("/users")
    public String users() {
        return "users";
    }

    // Librarian and Manager pages
    @GetMapping("/manage-resources")
    public String manageResources() {
        return "manage-resources";
    }

    @GetMapping("/catalogs")
    public String catalogs() {
        return "catalogs";
    }

    @GetMapping("/terms")
    public String terms() {
        return "terms";
    }

    @GetMapping("/backup")
    public String backup() {
        return "backup";
    }

    // Resource detail page
    @GetMapping("/resource/{id}")
    public String resourceDetail(@PathVariable Long id, Model model) {
        model.addAttribute("resource", landingService.getResourceDetail(id));
        model.addAttribute("relatedBooks", landingService.getRelatedBooksByAuthor(
            landingService.getResourceDetail(id).getAuthor(), 
            id, 
            4
        ));
        return "resource-detail";
    }
}
