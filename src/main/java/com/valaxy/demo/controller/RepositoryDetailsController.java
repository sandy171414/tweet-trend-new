package com.stalin.demo.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.kohsuke.github.GHRepositorySearchBuilder;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RepositoryDetailsController {

    @RequestMapping("/")
    public String getRepos() throws IOException {
        GitHub github = new GitHubBuilder().withPassword("valaxytech@gmail.com", "XXXXXXXX").build();
        GHRepositorySearchBuilder builder = github.searchRepositories();
        return "Greetings from Valaxy Technologies";
    }

    @GetMapping("/trends")
    public Map<String, String> getTwitterTrends(@RequestParam("placeid") String trendPlace, @RequestParam("count") String trendCount) {
        System.out.println("Mocking Twitter trends for testing...");

        Map<String, String> trendDetails = new HashMap<>();

        // Example mock data
        int count = Integer.parseInt(trendCount);
        for (int i = 1; i <= count; i++) {
            trendDetails.put("MockTrend" + i, "https://twitter.com/MockTrend" + i);
        }

        return trendDetails;
    }

}
