package dev.javis.javigg.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import dev.javis.javigg.match.dto.IMatchDto;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class MatchService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${riot.api.key}")
    private String apiKey;

    @Value("${riot.api.match.url}")
    private String matchApiUrl;

    public List<String> getMatchHistory(String puuid, int count) {
        String url = String.format(
                "%s/lol/match/v5/matches/by-puuid/%s/ids?start=0&count=%d&api_key=%s",
                matchApiUrl, puuid, count, apiKey
        );

        return restTemplate.getForObject(url, List.class);
    }

    public IMatchDto.MatchDto getMatchDetail(String matchId) {
        String url = String.format(
                "%s/lol/match/v5/matches/%s?api_key=%s",
                matchApiUrl, matchId, apiKey
        );

        return restTemplate.getForObject(url, IMatchDto.MatchDto.class);
    }

    public List<IMatchDto.MatchDto> getMatchDetails(List<String> matchIds) {
        List<CompletableFuture<IMatchDto.MatchDto>> futures = matchIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> getMatchDetail(id)))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Used by streak system
     */
    public List<Boolean> getLast3MatchResults(String puuid) {
        List<String> matchIds = getMatchHistory(puuid, 3);

        List<CompletableFuture<Boolean>> futures = matchIds.stream()
                .map(matchId -> CompletableFuture.supplyAsync(() -> {
                    IMatchDto.MatchDto match = getMatchDetail(matchId);
                    if (match == null) return null;
                    return match.info().participants().stream()
                            .filter(p -> puuid.equals(p.puuid()))
                            .findFirst()
                            .map(IMatchDto.ParticipantDto::win)
                            .orElse(false);
                }))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
