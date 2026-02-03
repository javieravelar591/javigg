package dev.javis.javigg.riot.dto;

import dev.javis.javigg.streak.dto.MiniStreakDto;

public record LivePlayerDto(
    String puuid,
    String summonerName,
    long championId,
    long profileIconId,
    long teamId,
    MiniStreakDto streak
) {}