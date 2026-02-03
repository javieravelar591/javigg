package dev.javis.javigg.riot;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import dev.javis.javigg.match.dto.IMatchDto;
import dev.javis.javigg.riot.dto.Account;
import dev.javis.javigg.riot.dto.Summoner;
import dev.javis.javigg.riot.dto.currentgame.CurrentGameInfo;

@Service
public class RiotApiClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${riot.api.key}")
    private String apiKey;

    @Value("${riot.api.account.url}")
    private String accountApiUrl;

    @Value("${riot.api.summoner.url}")
    private String summonerApiUrl;

    @Value("${riot.api.match.url}")
    private String matchApiUrl;

    @Value("${riot.api.spectator.url}")
    private String spectatorApiUrl;

    public Account getAccount(String gameName, String tagLine) {
        String url = String.format("%s/riot/account/v1/accounts/by-riot-id/%s/%s?api_key=%s",
                accountApiUrl, gameName, tagLine, apiKey);
        return restTemplate.getForObject(url, Account.class);
    }

    public Account getAccountByPuuid(String puuid) {
        String accountUrl = String.format("%s/riot/account/v1/accounts/by-puuid/%s?api_key=%s", 
                        accountApiUrl, puuid, apiKey);
        return restTemplate.getForObject(accountUrl, Account.class);
    }

    public Summoner getSummonerByPuuid(String puuid) {
        String url = String.format("%s/lol/summoner/v4/summoners/by-puuid/%s?api_key=%s",
                summonerApiUrl, puuid, apiKey);
        return restTemplate.getForObject(url, Summoner.class);
    }

    public List<String> getMatchIds(String puuid) {
        String url = String.format("%s/lol/match/v5/matches/by-puuid/%s/ids?start=0&count=20&api_key=%s",
                matchApiUrl, puuid, apiKey);
        return restTemplate.getForObject(url, List.class);
    }

    public IMatchDto.MatchDto getMatch(String matchId) {
        String url = String.format("%s/lol/match/v5/matches/%s?api_key=%s",
                matchApiUrl, matchId, apiKey);
        return restTemplate.getForObject(url, IMatchDto.MatchDto.class);
    }

    public CurrentGameInfo getActiveGame(String puuid) {
        try {
            String url = String.format(
                    "%s/lol/spectator/v5/active-games/by-summoner/%s?api_key=%s",
                    spectatorApiUrl, puuid, apiKey
            );

            return restTemplate.getForObject(url, CurrentGameInfo.class);

        } catch (Exception e) {
            // player not in game or API error
            return null;
        }
    }
}