package dev.javis.javigg.service;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.stereotype.Service;

import dev.javis.javigg.riot.Region;
import dev.javis.javigg.riot.RiotApiClient;
import dev.javis.javigg.riot.dto.LeagueEntryDto;

/** Riot League v4: ranked standings. Served from the same platform host as Summoner v4. */
@Service
public class LeagueService {

    private static final String SOLO_QUEUE = "RANKED_SOLO_5x5";

    private final RiotApiClient riotApiClient;

    public LeagueService(RiotApiClient riotApiClient) {
        this.riotApiClient = riotApiClient;
    }

    public Optional<LeagueEntryDto> getSoloQueueEntry(String puuid, Region region) {
        LeagueEntryDto[] entries = riotApiClient.get(region.platformHost(), "/lol/league/v4/entries/by-puuid/{puuid}",
                LeagueEntryDto[].class, puuid);
        if (entries == null) return Optional.empty();
        return Arrays.stream(entries)
                .filter(e -> SOLO_QUEUE.equals(e.queueType()))
                .findFirst();
    }
}
