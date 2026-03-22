package dev.javis.javigg.riot.dto;

public record LeagueEntryDto(
        String queueType,
        String tier,
        String rank,
        int leaguePoints,
        int wins,
        int losses
) {}
