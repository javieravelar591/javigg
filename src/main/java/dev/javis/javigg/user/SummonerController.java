package dev.javis.javigg.user;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
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
  
    @GetMapping({"/", "/home"})
    String home() {
        return "Main Page";
    }

    @GetMapping("/summoner/{gameName}/{tagLine}")
    public Summoner getSummoner(@PathVariable String gameName, @PathVariable String tagLine) {
        String accountUrl = String.format("%s/riot/account/v1/accounts/by-riot-id/%s/%s?api_key=%s", 
                                          accountApiUrl, gameName, tagLine, apiKey);
        Account accountDto = restTemplate.getForObject(accountUrl, Account.class);
        System.out.println(accountDto);

        if (accountDto == null || accountDto.puuid() == null) {
            throw new RuntimeException("Unable to fetch account details.");
        }

        // Step 2: Get Summoner information using PUUID
        String summonerUrl = String.format("%s/lol/summoner/v4/summoners/by-puuid/%s?api_key=%s",
                                           summonerApiUrl, accountDto.puuid(), apiKey);
        return restTemplate.getForObject(summonerUrl, Summoner.class);
    }   
}
