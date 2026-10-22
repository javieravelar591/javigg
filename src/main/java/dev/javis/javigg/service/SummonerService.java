package dev.javis.javigg.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dev.javis.javigg.riot.RiotApiClient;
import dev.javis.javigg.riot.dto.Summoner;

/** Riot Summoner v4: profile icon and level. */
@Service
public class SummonerService {

    private final RiotApiClient riotApiClient;
    private final String summonerApiUrl;

    public SummonerService(RiotApiClient riotApiClient,
                           @Value("${riot.api.summoner.url}") String summonerApiUrl) {
        this.riotApiClient = riotApiClient;
        this.summonerApiUrl = summonerApiUrl;
    }

    public Summoner getByPuuid(String puuid) {
        return riotApiClient.get(summonerApiUrl, "/lol/summoner/v4/summoners/by-puuid/{puuid}",
                Summoner.class, puuid);
    }
}
