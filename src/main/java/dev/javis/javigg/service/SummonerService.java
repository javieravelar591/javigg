package dev.javis.javigg.service;

import dev.javis.javigg.riot.dto.Account;
import dev.javis.javigg.riot.dto.LeagueEntryDto;
import dev.javis.javigg.riot.dto.Summoner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Optional;

@Service
public class SummonerService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${riot.api.key}")
    private String apiKey;

    @Value("${riot.api.account.url}")
    private String accountApiUrl;

    @Value("${riot.api.summoner.url}")
    private String summonerApiUrl;

    public Account getAccountByRiotId(String gameName, String tagLine) {
        String url = String.format(
                "%s/riot/account/v1/accounts/by-riot-id/%s/%s?api_key=%s",
                accountApiUrl, gameName, tagLine, apiKey
        );

        return restTemplate.getForObject(url, Account.class);
    }

    public Account getAccountByPuuid(String puuid) {
        String url = String.format(
                "%s/riot/account/v1/accounts/by-puuid/%s?api_key=%s",
                accountApiUrl, puuid, apiKey
        );

        return restTemplate.getForObject(url, Account.class);
    }

    public Summoner getSummonerByPuuid(String puuid) {
        String url = String.format(
                "%s/lol/summoner/v4/summoners/by-puuid/%s?api_key=%s",
                summonerApiUrl, puuid, apiKey
        );

        return restTemplate.getForObject(url, Summoner.class);
    }

    public Optional<LeagueEntryDto> getSoloQueueEntry(String puuid) {
        String url = String.format(
                "%s/lol/league/v4/entries/by-puuid/%s?api_key=%s",
                summonerApiUrl, puuid, apiKey
        );

        LeagueEntryDto[] entries = restTemplate.getForObject(url, LeagueEntryDto[].class);
        if (entries == null) return Optional.empty();
        return Arrays.stream(entries)
                .filter(e -> "RANKED_SOLO_5x5".equals(e.queueType()))
                .findFirst();
    }
}
