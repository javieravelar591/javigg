package dev.javis.javigg.service;

import dev.javis.javigg.config.RiotExecutorConfig;
import dev.javis.javigg.match.dto.IMatchDto;
import dev.javis.javigg.riot.Region;
import dev.javis.javigg.riot.dto.Account;
import dev.javis.javigg.riot.dto.LiveGameLobbyDto;
import dev.javis.javigg.riot.dto.LivePlayerDto;
import dev.javis.javigg.riot.dto.currentgame.CurrentGameInfo;
import dev.javis.javigg.riot.dto.currentgame.CurrentGameParticipant;
import dev.javis.javigg.streak.dto.MiniStreakDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Builds the live-game lobby: every participant's name, recent streak and playstyle tags. */
@Service
public class LiveGameService {

    private static final Logger log = LoggerFactory.getLogger(LiveGameService.class);

    private static final int TAG_MATCH_COUNT = 5;
    private static final int STREAK_MATCH_COUNT = 3;

    private final SpectatorService spectatorService;
    private final AccountService accountService;
    private final MatchService matchService;
    private final StreakService streakService;
    private final TagService tagService;
    private final Executor executor;

    public LiveGameService(SpectatorService spectatorService,
                           AccountService accountService,
                           MatchService matchService,
                           StreakService streakService,
                           TagService tagService,
                           @Qualifier(RiotExecutorConfig.RIOT_EXECUTOR) Executor executor) {
        this.spectatorService = spectatorService;
        this.accountService = accountService;
        this.matchService = matchService;
        this.streakService = streakService;
        this.tagService = tagService;
        this.executor = executor;
    }

    /** Empty when the player is not currently in a game. */
    public Optional<LiveGameLobbyDto> findLiveGameLobby(String puuid, Region region) {
        return spectatorService.findActiveGame(puuid, region).map(game -> buildLobby(game, region));
    }

    private LiveGameLobbyDto buildLobby(CurrentGameInfo game, Region region) {
        List<CompletableFuture<LivePlayerDto>> players = game.participants().stream()
                .filter(p -> p.puuid() != null && !p.puuid().isBlank())
                .map(p -> buildPlayerAsync(p, region))
                .toList();

        return new LiveGameLobbyDto(game.gameId(), game.gameMode(),
                players.stream().map(CompletableFuture::join).toList());
    }

    // Composed rather than joined inside pool tasks, so the bounded executor can't deadlock on
    // tasks waiting for their own subtasks.
    private CompletableFuture<LivePlayerDto> buildPlayerAsync(CurrentGameParticipant p, Region region) {
        CompletableFuture<Account> account =
                CompletableFuture.supplyAsync(() -> accountService.getByPuuid(p.puuid(), region), executor);
        CompletableFuture<List<IMatchDto.MatchDto>> matches =
                matchService.getRecentMatchesAsync(p.puuid(), TAG_MATCH_COUNT, region);

        return account.thenCombine(matches, (acc, recent) -> toPlayerDto(p, acc, recent))
                .exceptionally(e -> {
                    log.warn("Live game lookup failed for one player, returning defaults: {}", e.getMessage());
                    return toPlayerDto(p, null, List.of());
                });
    }

    private LivePlayerDto toPlayerDto(CurrentGameParticipant p, Account account, List<IMatchDto.MatchDto> matches) {
        List<Boolean> lastResults = matches.stream()
                .limit(STREAK_MATCH_COUNT)
                .map(m -> m.participant(p.puuid()).map(IMatchDto.ParticipantDto::win).orElse(false))
                .toList();

        MiniStreakDto streak = streakService.calculateMiniStreak(lastResults);
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
    }
}
