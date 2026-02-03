package dev.javis.javigg.riot.dto.currentgame;

import java.util.List;

public record CurrentGameInfo(
        long gameId,
        String gameType,
        long gameStartTime,
        long mapId,
        long gameLength,
        String platformId,
        String gameMode,
        List<BannedChampion> bannedChampions,
        long gameQueueConfigId,
        Observer observers,
        List<CurrentGameParticipant> participants
) {}