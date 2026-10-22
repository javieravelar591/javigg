package dev.javis.javigg.service;

import org.springframework.stereotype.Service;

import dev.javis.javigg.riot.Region;
import dev.javis.javigg.riot.RiotApiClient;
import dev.javis.javigg.riot.dto.Summoner;

/** Riot Summoner v4: profile icon and level. */
@Service
public class SummonerService {

    private final RiotApiClient riotApiClient;

    public SummonerService(RiotApiClient riotApiClient) {
        this.riotApiClient = riotApiClient;
    }

    public Summoner getByPuuid(String puuid, Region region) {
        return riotApiClient.get(region.platformHost(), "/lol/summoner/v4/summoners/by-puuid/{puuid}",
                Summoner.class, puuid);
    }
}
