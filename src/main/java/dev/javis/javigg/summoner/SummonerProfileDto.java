package dev.javis.javigg.summoner;

import java.util.List;

import dev.javis.javigg.match.dto.IMatchDto;
import dev.javis.javigg.riot.dto.Account;
import dev.javis.javigg.riot.dto.LeagueEntryDto;
import dev.javis.javigg.riot.dto.Summoner;

/** Response body of {@code GET /summoner}. {@code rankedSolo} is null when the player is unranked. */
public record SummonerProfileDto(
        Account account,
        Summoner summoner,
        List<String> matchHistory,
        List<IMatchDto.MatchDto> matchDetails,
        LeagueEntryDto rankedSolo
) { }
