package dev.javis.javigg.riot.dto;

import java.util.List;

public record LiveGameLobbyDto(
    long gameId,
    String gameMode,
    List<LivePlayerDto> players
) {}
