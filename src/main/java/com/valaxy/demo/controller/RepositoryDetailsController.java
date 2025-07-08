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
        return "wellcome DEV";
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

<<<<<<< HEAD
	@RequestMapping("/")
	public String getRepos() throws IOException {
		GitHub github = new GitHubBuilder().withPassword("valaxytech@gmail.com", "XXXXXXXX").build();
		GHRepositorySearchBuilder builder = github.searchRepositories();
		return "this is dev branch boom";
	}

	@GetMapping("/trends")
	public Map<String, String> getTwitterTrends(@RequestParam("placeid") String trendPlace, @RequestParam("count") String trendCount) {
		String consumerKey = env.getProperty("CONSUMER_KEY");
		String consumerSecret = env.getProperty("CONSUMER_SECRET");
		String accessToken = env.getProperty("ACCESS_TOKEN");
		String accessTokenSecret = env.getProperty("ACCESS_TOKEN_SECRET");
		System.out.println("consumerKey "+consumerKey+" consumerSecret "+consumerSecret+" accessToken "+accessToken+" accessTokenSecret "+accessTokenSecret);		
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
		        .setOAuthConsumerKey(consumerKey)
				.setOAuthConsumerSecret(consumerSecret)
				.setOAuthAccessToken(accessToken)
				.setOAuthAccessTokenSecret(accessTokenSecret);
		TwitterFactory tf = new TwitterFactory(cb.build());
		System.out.println("Twitter Factory "+tf);
		System.out.println("Code testing purpose ");
		Twitter twitter = tf.getInstance();
		System.out.println("Twitter object "+twitter);
		Map<String, String> trendDetails = new HashMap<String, String>();
		try {
			Trends trends = twitter.getPlaceTrends(Integer.parseInt(trendPlace));
			System.out.println("After API call");
			int count = 0;
			for (Trend trend : trends.getTrends()) {
				if (count < Integer.parseInt(trendCount)) {
					trendDetails.put(trend.getName(), trend.getURL());
					count++;
				}
			}
		} catch (TwitterException e) {
			trendDetails.put("test", "MyTweet");
            //trendDetails.put("Twitter Exception", e.getMessage());
			System.out.println("Twitter exception "+e.getMessage());

		}catch (Exception e) {
			trendDetails.put("test", "MyTweet");
            System.out.println("Exception "+e.getMessage());
		}
		return trendDetails;
	}
=======
        return trendDetails;
    }
>>>>>>> main

}
