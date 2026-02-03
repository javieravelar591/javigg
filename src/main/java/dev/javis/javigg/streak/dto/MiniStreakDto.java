package dev.javis.javigg.streak.dto;

public record MiniStreakDto(
        int winStreak,
        int lossStreak,
        String status // "HOT", "COLD", "NEUTRAL"
) {}