package dev.javis.javigg.user;

import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.deser.DataFormatReaders.Match;

import dev.javis.javigg.service.DataDragonService;

import org.springframework.ui.Model;
import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Controller
public class SummonerController {

    @Autowired
    private DataDragonService dataDragonService;

    private final RestTemplate restTemplate;

    private static final Logger logger = LoggerFactory.getLogger(SummonerController.class);

    @Value("${riot.api.key}")
    String apiKey;

    @Value("${riot.api.account.url}")
    private String accountApiUrl;

    @Value("${riot.api.summoner.url}")
    private String summonerApiUrl;

    @Value("${riot.api.match.url}")
    private String matchApriUrl;
    
    
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
            System.out.println(summonerDto.profileIconId());

            // Get match history
            List<String> matchHistory = getMatchHistory(accountDto.puuid());
            // Map<String, Object> champData = dataDragonService.getChampData();

            // Get profile Icon
            String profileIconData = dataDragonService.getProfileIconUrl(String.valueOf(summonerDto.profileIconId()));

            System.out.println(summonerDto);
            model.addAttribute("summoner", summonerDto);
            model.addAttribute("gameName", gameName);
            model.addAttribute("tagLine", tagLine);
            model.addAttribute("matchHistory", matchHistory);
            // model.addAttribute("champData", champData);
            model.addAttribute("profileIconUrl", profileIconData);

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

    public List<String> getMatchHistory(String puuid) {
        try {
        String matchesUrl = String.format("%s/lol/match/v5/matches/by-puuid/%s/ids/?start=0&count=20&api_key=%s",
                                            matchApriUrl, puuid, apiKey);
        return restTemplate.getForObject(matchesUrl, List.class);
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
