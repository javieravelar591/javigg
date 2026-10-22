package dev.javis.javigg.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import dev.javis.javigg.config.RiotExecutorConfig;
import dev.javis.javigg.match.dto.IMatchDto;
import dev.javis.javigg.riot.Region;
import dev.javis.javigg.riot.dto.Account;
import dev.javis.javigg.riot.dto.RankedEntriesDto;
import dev.javis.javigg.riot.dto.Summoner;
import dev.javis.javigg.summoner.SummonerProfileDto;

/** Aggregates everything the summoner profile page shows into one response. */
@Service
public class ProfileService {

    static final int MATCH_HISTORY_COUNT = 20;

    private final AccountService accountService;
    private final SummonerService summonerService;
    private final LeagueService leagueService;
    private final MatchService matchService;
    private final PlayerStatsService playerStatsService;
    private final Executor executor;

    public ProfileService(AccountService accountService,
                          SummonerService summonerService,
                          LeagueService leagueService,
                          MatchService matchService,
                          PlayerStatsService playerStatsService,
                          @Qualifier(RiotExecutorConfig.RIOT_EXECUTOR) Executor executor) {
        this.accountService = accountService;
        this.summonerService = summonerService;
        this.leagueService = leagueService;
        this.matchService = matchService;
        this.playerStatsService = playerStatsService;
        this.executor = executor;
    }

    public SummonerProfileDto getProfile(String gameName, String tagLine, Region region) {
        Account account = accountService.getByRiotId(gameName, tagLine, region);
        String puuid = account.puuid();

        // Summoner, ranked and match lookups are independent, so run them concurrently.
        CompletableFuture<Summoner> summoner =
                CompletableFuture.supplyAsync(() -> summonerService.getByPuuid(puuid, region), executor);
        CompletableFuture<RankedEntriesDto> ranked =
                CompletableFuture.supplyAsync(() -> leagueService.getRankedEntries(puuid, region), executor);
        CompletableFuture<List<String>> matchIds =
                CompletableFuture.supplyAsync(() -> matchService.getMatchIds(puuid, MATCH_HISTORY_COUNT, region), executor);
        CompletableFuture<List<IMatchDto.MatchDto>> matches =
                matchIds.thenCompose(ids -> matchService.getMatchesAsync(ids, region));

        try {
            List<IMatchDto.MatchDto> matchDetails = matches.join();
            RankedEntriesDto rankedEntries = ranked.join();

            return new SummonerProfileDto(
                    account,
                    summoner.join(),
                    matchIds.join(),
                    matchDetails,
                    rankedEntries.solo(),
                    rankedEntries.flex(),
                    playerStatsService.championBreakdown(puuid, matchDetails),
                    playerStatsService.roleBreakdown(puuid, matchDetails));
        } catch (CompletionException e) {
            // Surface the original RiotApiException so the error handler can map it.
            if (e.getCause() instanceof RuntimeException cause) throw cause;
            throw e;
        }
    }
}
