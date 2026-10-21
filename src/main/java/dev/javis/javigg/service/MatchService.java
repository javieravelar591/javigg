package dev.javis.javigg.service;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import dev.javis.javigg.config.RiotExecutorConfig;
import dev.javis.javigg.match.dto.IMatchDto;
import dev.javis.javigg.riot.RiotApiClient;

/** Riot Match v5: match IDs and match details. */
@Service
public class MatchService {

    private final RiotApiClient riotApiClient;
    private final Executor executor;
    private final String matchApiUrl;

    // Finished matches never change, so cached details save most of the Riot calls behind profile
    // views and live-game lookups.
    private final Cache<String, IMatchDto.MatchDto> matchCache = Caffeine.newBuilder()
            .maximumSize(2_000)
            .expireAfterAccess(Duration.ofHours(1))
            .build();

    public MatchService(RiotApiClient riotApiClient,
                        @Qualifier(RiotExecutorConfig.RIOT_EXECUTOR) Executor executor,
                        @Value("${riot.api.match.url}") String matchApiUrl) {
        this.riotApiClient = riotApiClient;
        this.executor = executor;
        this.matchApiUrl = matchApiUrl;
    }

    public List<String> getMatchIds(String puuid, int count) {
        String[] ids = riotApiClient.get(matchApiUrl,
                "/lol/match/v5/matches/by-puuid/{puuid}/ids?start=0&count={count}",
                String[].class, puuid, count);
        return ids == null ? List.of() : Arrays.asList(ids);
    }

    public IMatchDto.MatchDto getMatch(String matchId) {
        IMatchDto.MatchDto cached = matchCache.getIfPresent(matchId);
        if (cached != null) return cached;

        IMatchDto.MatchDto match = riotApiClient.get(matchApiUrl, "/lol/match/v5/matches/{matchId}",
                IMatchDto.MatchDto.class, matchId);
        if (match != null) matchCache.put(matchId, match);
        return match;
    }

    /** Fetches match details in parallel, preserving the order of {@code matchIds}. */
    public CompletableFuture<List<IMatchDto.MatchDto>> getMatchesAsync(List<String> matchIds) {
        List<CompletableFuture<IMatchDto.MatchDto>> futures = matchIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> getMatch(id), executor))
                .toList();

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(ignored -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .toList());
    }

    /** Most recent {@code count} matches for a player, newest first. */
    public CompletableFuture<List<IMatchDto.MatchDto>> getRecentMatchesAsync(String puuid, int count) {
        return CompletableFuture.supplyAsync(() -> getMatchIds(puuid, count), executor)
                .thenCompose(this::getMatchesAsync);
    }
}
