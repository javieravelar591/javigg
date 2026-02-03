package dev.javis.javigg.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import dev.javis.javigg.match.dto.IMatchDto;

import java.util.ArrayList;
import java.util.List;

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
        List<IMatchDto.MatchDto> matches = new ArrayList<>();

        for (String matchId : matchIds) {
            IMatchDto.MatchDto match = getMatchDetail(matchId);
            if (match != null) matches.add(match);
        }

        return matches;
    }

    /**
     * Used by streak system
     */
    public List<Boolean> getLast3MatchResults(String puuid) {
        List<String> matchIds = getMatchHistory(puuid, 3);
        List<Boolean> results = new ArrayList<>();

        for (String matchId : matchIds) {
            IMatchDto.MatchDto match = getMatchDetail(matchId);

            if (match == null) continue;

            boolean win = match.info().participants().stream()
                    .filter(p -> puuid.equals(p.puuid()))
                    .findFirst()
                    .map(IMatchDto.ParticipantDto::win)
                    .orElse(false);

            results.add(win);
        }

        return results;
    }
}
