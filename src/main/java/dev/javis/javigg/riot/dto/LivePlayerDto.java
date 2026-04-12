package dev.javis.javigg.riot.dto;

import dev.javis.javigg.streak.dto.MiniStreakDto;

import java.util.List;

public record LivePlayerDto(
    String puuid,
    String summonerName,
    long championId,
    long profileIconId,
    long teamId,
    MiniStreakDto streak,
    List<String> tags
) {}