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
import dev.javis.javigg.riot.dto.LeagueEntryDto;
import dev.javis.javigg.riot.dto.Summoner;
import dev.javis.javigg.summoner.SummonerProfileDto;

/** Aggregates everything the summoner profile page shows into one response. */
@Service
public class ProfileService {

    static final int MATCH_HISTORY_COUNT = 10;

    private final AccountService accountService;
    private final SummonerService summonerService;
    private final LeagueService leagueService;
    private final MatchService matchService;
    private final Executor executor;

    public ProfileService(AccountService accountService,
                          SummonerService summonerService,
                          LeagueService leagueService,
                          MatchService matchService,
                          @Qualifier(RiotExecutorConfig.RIOT_EXECUTOR) Executor executor) {
        this.accountService = accountService;
        this.summonerService = summonerService;
        this.leagueService = leagueService;
        this.matchService = matchService;
        this.executor = executor;
    }

    public SummonerProfileDto getProfile(String gameName, String tagLine, Region region) {
        Account account = accountService.getByRiotId(gameName, tagLine, region);
        String puuid = account.puuid();

        // Summoner, ranked and match lookups are independent, so run them concurrently.
        CompletableFuture<Summoner> summoner =
                CompletableFuture.supplyAsync(() -> summonerService.getByPuuid(puuid, region), executor);
        CompletableFuture<LeagueEntryDto> rankedSolo =
                CompletableFuture.supplyAsync(() -> leagueService.getSoloQueueEntry(puuid, region).orElse(null), executor);
        CompletableFuture<List<String>> matchIds =
                CompletableFuture.supplyAsync(() -> matchService.getMatchIds(puuid, MATCH_HISTORY_COUNT, region), executor);
        CompletableFuture<List<IMatchDto.MatchDto>> matches =
                matchIds.thenCompose(ids -> matchService.getMatchesAsync(ids, region));

        try {
            return new SummonerProfileDto(account, summoner.join(), matchIds.join(), matches.join(), rankedSolo.join());
        } catch (CompletionException e) {
            // Surface the original RiotApiException so the error handler can map it.
            if (e.getCause() instanceof RuntimeException cause) throw cause;
            throw e;
        }
    }
}
