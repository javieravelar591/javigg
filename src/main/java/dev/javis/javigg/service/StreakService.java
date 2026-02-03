package dev.javis.javigg.service;

import org.springframework.stereotype.Service;

import dev.javis.javigg.streak.dto.MiniStreakDto;

import java.util.List;

@Service
public class StreakService {

    public MiniStreakDto calculateMiniStreak(List<Boolean> lastResults) {
        if (lastResults == null || lastResults.isEmpty()) {
            return new MiniStreakDto(0, 0, "NEUTRAL");
        }

        int wins = 0;
        int losses = 0;

        for (Boolean win : lastResults) {
            if (win != null && win) wins++;
            else losses++;
        }

        String status = "NEUTRAL";
        if (wins == lastResults.size()) status = "HOT";      // e.g., won last 3
        else if (losses == lastResults.size()) status = "COLD"; // e.g., lost last 3

        return new MiniStreakDto(wins, losses, status);
    }
}
