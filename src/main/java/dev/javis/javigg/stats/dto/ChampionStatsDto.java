package dev.javis.javigg.stats.dto;

/** A player's record on one champion across the sampled recent matches. */
public record ChampionStatsDto(
        int championId,
        String championName,
        int games,
        int wins,
        int losses,
        double winRate
) {}
