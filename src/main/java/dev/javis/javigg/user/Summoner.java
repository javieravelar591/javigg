package dev.javis.javigg.user;

public record Summoner(
    String id,
    String accountId,
    String puuid,
    String gameName,
    int profileIconId,
    long revisionDate,
    long summonerLevel
) {}
