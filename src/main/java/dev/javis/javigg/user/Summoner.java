package dev.javis.javigg.user;

public record Summoner(
    String id,
    String accountId,
    String puuid,
    String name,
    int profileIconId,
    long revisionDate,
    long summonerLevel
) {}
