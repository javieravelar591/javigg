package dev.javis.javigg.riot.dto;

public record Summoner(
    String puuid,
    int profileIconId,
    long revisionDate,
    long summonerLevel
) {}
