package dev.javis.javigg.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import dev.javis.javigg.match.dto.IMatchDto;
import dev.javis.javigg.stats.dto.ChampionStatsDto;
import dev.javis.javigg.stats.dto.RoleStatsDto;

/** Pure-logic aggregation of a player's recent matches into per-champion and per-role records. */
@Service
public class PlayerStatsService {

    static final int TOP_CHAMPIONS = 5;

    /** Most-played champions first, capped to the top {@value #TOP_CHAMPIONS}. */
    public List<ChampionStatsDto> championBreakdown(String puuid, List<IMatchDto.MatchDto> matches) {
        record Champion(int championId, String championName) { }

        Map<Champion, List<Boolean>> resultsByChampion = matches.stream()
                .flatMap(m -> m.participant(puuid).stream())
                .collect(Collectors.groupingBy(
                        p -> new Champion(p.championId(), p.championName()),
                        Collectors.mapping(IMatchDto.ParticipantDto::win, Collectors.toList())));

        return resultsByChampion.entrySet().stream()
                .map(e -> new ChampionStatsDto(e.getKey().championId(), e.getKey().championName(),
                        e.getValue().size(), wins(e.getValue()), losses(e.getValue()), winRate(e.getValue())))
                .sorted(Comparator.comparingInt(ChampionStatsDto::games).reversed())
                .limit(TOP_CHAMPIONS)
                .toList();
    }

    /** Every role the player appeared in (Riot's {@code teamPosition}), most-played first. */
    public List<RoleStatsDto> roleBreakdown(String puuid, List<IMatchDto.MatchDto> matches) {
        Map<String, List<Boolean>> resultsByRole = matches.stream()
                .flatMap(m -> m.participant(puuid).stream())
                .filter(p -> p.teamPosition() != null && !p.teamPosition().isBlank())
                .collect(Collectors.groupingBy(IMatchDto.ParticipantDto::teamPosition,
                        Collectors.mapping(IMatchDto.ParticipantDto::win, Collectors.toList())));

        return resultsByRole.entrySet().stream()
                .map(e -> new RoleStatsDto(e.getKey(), e.getValue().size(), wins(e.getValue()),
                        losses(e.getValue()), winRate(e.getValue())))
                .sorted(Comparator.comparingInt(RoleStatsDto::games).reversed())
                .toList();
    }

    private static int wins(List<Boolean> results) {
        return (int) results.stream().filter(Boolean::booleanValue).count();
    }

    private static int losses(List<Boolean> results) {
        return results.size() - wins(results);
    }

    private static double winRate(List<Boolean> results) {
        if (results.isEmpty()) return 0.0;
        return Math.round(wins(results) * 1000.0 / results.size()) / 10.0;
    }
}
