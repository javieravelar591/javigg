package dev.javis.javigg.stats.dto;

/** A player's record on one role (Riot's {@code teamPosition}) across the sampled recent matches. */
public record RoleStatsDto(
        String role,
        int games,
        int wins,
        int losses,
        double winRate
) {}
