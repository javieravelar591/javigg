package dev.javis.javigg.riot.dto.currentgame;

import java.util.List;

public record CurrentGameParticipant(
        long championId,
        Perks perks,
        long profileIconId,
        boolean bot,
        long teamId,
        String puuid,
        long spell1Id,
        long spell2Id,
        List<GameCustomizationObject> gameCustomizationObjects
) {}
