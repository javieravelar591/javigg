package dev.javis.javigg.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dev.javis.javigg.riot.RiotApiClient;
import dev.javis.javigg.riot.RiotApiException;
import dev.javis.javigg.riot.dto.currentgame.CurrentGameInfo;

/** Riot Spectator v5: games in progress. */
@Service
public class SpectatorService {

    private final RiotApiClient riotApiClient;
    private final String spectatorApiUrl;

    public SpectatorService(RiotApiClient riotApiClient,
                            @Value("${riot.api.spectator.url}") String spectatorApiUrl) {
        this.riotApiClient = riotApiClient;
        this.spectatorApiUrl = spectatorApiUrl;
    }

    /** Empty when the player is not in a game; other Riot failures propagate. */
    public Optional<CurrentGameInfo> findActiveGame(String puuid) {
        try {
            return Optional.ofNullable(riotApiClient.get(spectatorApiUrl,
                    "/lol/spectator/v5/active-games/by-summoner/{puuid}", CurrentGameInfo.class, puuid));
        } catch (RiotApiException e) {
            if (e.isNotFound()) return Optional.empty();
            throw e;
        }
    }
}
