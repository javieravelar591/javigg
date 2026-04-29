package dev.javis.javigg.service;

import dev.javis.javigg.service.LiveGameService;
import dev.javis.javigg.riot.dto.LiveGameLobbyDto;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LiveGameController {

    private final LiveGameService liveGameService;

    public LiveGameController(LiveGameService liveGameService) {
        this.liveGameService = liveGameService;
    }

    @GetMapping("/live-game")
    public LiveGameLobbyDto getLiveGame(@RequestParam String puuid) {
        // Call the service to build the lobby
        LiveGameLobbyDto lobby = liveGameService.buildLiveGameLobby(puuid);

        if (lobby == null) {
            // Player not in game
            throw new RuntimeException("Player is not currently in a live game");
        }

        return lobby;
    }
}

