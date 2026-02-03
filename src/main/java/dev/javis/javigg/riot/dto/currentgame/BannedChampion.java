package dev.javis.javigg.riot.dto.currentgame;

public record BannedChampion(
    int pickTurn,
    long championId,
    long teamId
) {}