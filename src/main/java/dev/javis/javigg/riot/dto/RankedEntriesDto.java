package dev.javis.javigg.riot.dto;

/** A player's current-season ranked standings. Either field is null if unranked in that queue. */
public record RankedEntriesDto(
        LeagueEntryDto solo,
        LeagueEntryDto flex
) {}
