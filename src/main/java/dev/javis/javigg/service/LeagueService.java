package dev.javis.javigg.service;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dev.javis.javigg.riot.RiotApiClient;
import dev.javis.javigg.riot.dto.LeagueEntryDto;

/** Riot League v4: ranked standings. */
@Service
public class LeagueService {

    private static final String SOLO_QUEUE = "RANKED_SOLO_5x5";

    private final RiotApiClient riotApiClient;
    private final String leagueApiUrl;

    // League v4 is served from the same platform host as Summoner v4.
    public LeagueService(RiotApiClient riotApiClient,
                         @Value("${riot.api.league.url:${riot.api.summoner.url}}") String leagueApiUrl) {
        this.riotApiClient = riotApiClient;
        this.leagueApiUrl = leagueApiUrl;
    }

    public Optional<LeagueEntryDto> getSoloQueueEntry(String puuid) {
        LeagueEntryDto[] entries = riotApiClient.get(leagueApiUrl, "/lol/league/v4/entries/by-puuid/{puuid}",
                LeagueEntryDto[].class, puuid);
        if (entries == null) return Optional.empty();
        return Arrays.stream(entries)
                .filter(e -> SOLO_QUEUE.equals(e.queueType()))
                .findFirst();
    }
}
