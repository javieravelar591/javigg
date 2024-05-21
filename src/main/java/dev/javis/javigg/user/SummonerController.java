package dev.javis.javigg.user;

import java.util.List;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.ui.Model;
import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Controller
public class SummonerController {

    private static final Logger logger = LoggerFactory.getLogger(SummonerController.class);

    @Value("${riot.api.key}")
    String apiKey;

    @Value("${riot.api.account.url}")
    private String accountApiUrl;

    @Value("${riot.api.summoner.url}")
    private String summonerApiUrl;
    

    private final RestTemplate restTemplate;
    
    public SummonerController() {
        this.restTemplate = new RestTemplate();
    }

    @PostConstruct
    public void init() {
        System.out.println("API Key: " + apiKey);
        System.out.println("Account API URL: " + accountApiUrl);
        System.out.println("Summoner API URL: " + summonerApiUrl);
    }

    // landing page
    @RequestMapping({"/", "/home"})
    public String home() {
        return "home";
    }

    @GetMapping("/summoner")
    public String getSummoner(@RequestParam String gameName, @RequestParam String tagLine, Model model) {
        try {
            // Get puuid
            String accountUrl = String.format("%s/riot/account/v1/accounts/by-riot-id/%s/%s?api_key=%s", 
                                              accountApiUrl, gameName, tagLine, apiKey);
            Account accountDto = restTemplate.getForObject(accountUrl, Account.class);
            System.out.println(accountDto);

            if (accountDto == null || accountDto.puuid() == null) {
                throw new RuntimeException("Unable to fetch account details.");
            }

            // Get summoner info
            String summonerUrl = String.format("%s/lol/summoner/v4/summoners/by-puuid/%s?api_key=%s",
                                               summonerApiUrl, accountDto.puuid(), apiKey);
            Summoner summonerDto = restTemplate.getForObject(summonerUrl, Summoner.class);
            System.out.println(summonerDto);
            model.addAttribute("summoner", summonerDto);
            model.addAttribute("gameName", gameName);
            model.addAttribute("tagLine", tagLine);
            System.out.println(model);
            return "home"; // Return to the same index page with summoner info populated
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error during API call: {}", e.getStatusCode(), e);
            logger.error("Response body: {}", e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during API call", e);
            throw new RuntimeException("Unexpected error during API call", e);
        }
    }
}
