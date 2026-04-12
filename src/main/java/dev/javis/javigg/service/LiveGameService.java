package dev.javis.javigg.service;

import dev.javis.javigg.riot.RiotApiClient;
import dev.javis.javigg.riot.dto.currentgame.CurrentGameInfo;
import dev.javis.javigg.riot.dto.currentgame.CurrentGameParticipant;
import dev.javis.javigg.riot.dto.LiveGameLobbyDto;
import dev.javis.javigg.riot.dto.LivePlayerDto;
import dev.javis.javigg.riot.dto.Account;
import dev.javis.javigg.streak.dto.MiniStreakDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LiveGameService {

    private final MatchService matchService;
    private final StreakService streakService;
    private final TagService tagService;
    private final RiotApiClient riotApiClient; // low-level HTTP calls to Riot API

    public LiveGameService(MatchService matchService,
                           StreakService streakService,
                           TagService tagService,
                           RiotApiClient riotApiClient) {
        this.matchService = matchService;
        this.streakService = streakService;
        this.tagService = tagService;
        this.riotApiClient = riotApiClient;
    }


    public LiveGameLobbyDto buildLiveGameLobby(String puuid) {

        // 1. Get current game info
        CurrentGameInfo game = riotApiClient.getActiveGame(puuid);
        if (game == null) return null; // not in a game

        List<LivePlayerDto> players = new ArrayList<>();


        // 2. For each participant, get last 3 results + streak
        for (CurrentGameParticipant p : game.participants()) {
            if (p.puuid() == null || p.puuid().isBlank()) continue;

            try {
                Account account = riotApiClient.getAccountByPuuid(p.puuid());
                List<Boolean> last3Results = matchService.getLast3MatchResults(p.puuid());
                MiniStreakDto streak = streakService.calculateMiniStreak(last3Results);
                List<String> tags = tagService.generateTags(p.puuid());

                players.add(new LivePlayerDto(
                        p.puuid(),
                        account != null ? account.gameName() : null,
                        p.championId(),
                        p.profileIconId(),
                        p.teamId(),
                        streak,
                        tags
                ));
            } catch (Exception e) {
                players.add(new LivePlayerDto(
                        p.puuid(),
                        null,
                        p.championId(),
                        p.profileIconId(),
                        p.teamId(),
                        streakService.calculateMiniStreak(List.of()),
                        List.of()
                ));
            }
        }

        // 3. Return full lobby DTO
        return new LiveGameLobbyDto(
                game.gameId(),
                game.gameMode(),
                players
        );
    }
}
