package dev.javis.javigg.service;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.stereotype.Service;

import dev.javis.javigg.riot.Region;
import dev.javis.javigg.riot.RiotApiClient;
import dev.javis.javigg.riot.dto.LeagueEntryDto;
import dev.javis.javigg.riot.dto.RankedEntriesDto;

/** Riot League v4: ranked standings. Served from the same platform host as Summoner v4. */
@Service
public class LeagueService {

    private static final String SOLO_QUEUE = "RANKED_SOLO_5x5";
    private static final String FLEX_QUEUE = "RANKED_FLEX_SR";

    private final RiotApiClient riotApiClient;

    public LeagueService(RiotApiClient riotApiClient) {
        this.riotApiClient = riotApiClient;
    }

    /** Current-season solo queue and flex queue standings; either is empty if unranked in that queue. */
    public RankedEntriesDto getRankedEntries(String puuid, Region region) {
        LeagueEntryDto[] entries = riotApiClient.get(region.platformHost(), "/lol/league/v4/entries/by-puuid/{puuid}",
                LeagueEntryDto[].class, puuid);
        if (entries == null) return new RankedEntriesDto(null, null);

        return new RankedEntriesDto(
                findByQueue(entries, SOLO_QUEUE).orElse(null),
                findByQueue(entries, FLEX_QUEUE).orElse(null));
    }

    private Optional<LeagueEntryDto> findByQueue(LeagueEntryDto[] entries, String queueType) {
        return Arrays.stream(entries)
                .filter(e -> queueType.equals(e.queueType()))
                .findFirst();
    }
}
