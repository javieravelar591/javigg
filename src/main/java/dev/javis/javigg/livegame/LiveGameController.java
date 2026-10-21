package dev.javis.javigg.livegame;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.javis.javigg.riot.dto.LiveGameLobbyDto;
import dev.javis.javigg.service.LiveGameService;
import jakarta.validation.constraints.Pattern;

@RestController
public class LiveGameController {

    private final LiveGameService liveGameService;

    public LiveGameController(LiveGameService liveGameService) {
        this.liveGameService = liveGameService;
    }

    @GetMapping("/live-game")
    public LiveGameLobbyDto getLiveGame(@RequestParam @Pattern(regexp = "[A-Za-z0-9_-]{1,100}") String puuid) {
        return liveGameService.findLiveGameLobby(puuid).orElseThrow(NotInGameException::new);
    }
}
