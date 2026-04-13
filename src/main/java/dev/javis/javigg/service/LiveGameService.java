package dev.javis.javigg.service;

import dev.javis.javigg.match.dto.IMatchDto;
import dev.javis.javigg.riot.RiotApiClient;
import dev.javis.javigg.riot.dto.currentgame.CurrentGameInfo;
import dev.javis.javigg.riot.dto.currentgame.CurrentGameParticipant;
import dev.javis.javigg.riot.dto.LiveGameLobbyDto;
import dev.javis.javigg.riot.dto.LivePlayerDto;
import dev.javis.javigg.riot.dto.Account;
import dev.javis.javigg.streak.dto.MiniStreakDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class LiveGameService {

    private final MatchService matchService;
    private final StreakService streakService;
    private final TagService tagService;
    private final RiotApiClient riotApiClient;

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
        CurrentGameInfo game = riotApiClient.getActiveGame(puuid);
        if (game == null) return null;

        List<LivePlayerDto> players = game.participants().stream()
                .filter(p -> p.puuid() != null && !p.puuid().isBlank())
                .map(p -> CompletableFuture.supplyAsync(() -> buildPlayerDto(p)))
                .toList()
                .stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        return new LiveGameLobbyDto(game.gameId(), game.gameMode(), players);
    }

    private LivePlayerDto buildPlayerDto(CurrentGameParticipant p) {
        try {
            // account lookup and match fetching run in parallel
            CompletableFuture<Account> accountFuture =
                    CompletableFuture.supplyAsync(() -> riotApiClient.getAccountByPuuid(p.puuid()));

            CompletableFuture<List<IMatchDto.MatchDto>> matchesFuture =
                    CompletableFuture.supplyAsync(() -> {
                        List<String> ids = matchService.getMatchHistory(p.puuid(), 5);
                        return matchService.getMatchDetails(ids);
                    });

            Account account = accountFuture.join();
            List<IMatchDto.MatchDto> matches = matchesFuture.join();

            // streak from first 3, tags from all 5
            List<Boolean> last3 = matches.stream()
                    .limit(3)
                    .map(m -> m.info().participants().stream()
                            .filter(part -> p.puuid().equals(part.puuid()))
                            .findFirst()
                            .map(IMatchDto.ParticipantDto::win)
                            .orElse(false))
                    .collect(Collectors.toList());

            MiniStreakDto streak = streakService.calculateMiniStreak(last3);
            List<String> tags = tagService.generateTags(p.puuid(), matches);

            return new LivePlayerDto(
                    p.puuid(),
                    account != null ? account.gameName() : null,
                    p.championId(),
                    p.profileIconId(),
                    p.teamId(),
                    streak,
                    tags
            );
        } catch (Exception e) {
            return new LivePlayerDto(
                    p.puuid(),
                    null,
                    p.championId(),
                    p.profileIconId(),
                    p.teamId(),
                    streakService.calculateMiniStreak(List.of()),
                    List.of()
            );
        }
    }
}
