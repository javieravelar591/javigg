package dev.javis.javigg.summoner;

import java.util.List;

import dev.javis.javigg.match.dto.IMatchDto;
import dev.javis.javigg.riot.dto.Account;
import dev.javis.javigg.riot.dto.LeagueEntryDto;
import dev.javis.javigg.riot.dto.Summoner;
import dev.javis.javigg.stats.dto.ChampionStatsDto;
import dev.javis.javigg.stats.dto.RoleStatsDto;

/**
 * Response body of {@code GET /summoner}. {@code rankedSolo}/{@code rankedFlex} are null when the
 * player is unranked in that queue. {@code championStats} and {@code roleStats} are computed from
 * {@code matchDetails} (see {@code PlayerStatsService}), so they only cover as many recent games as
 * {@code matchHistory} does.
 */
public record SummonerProfileDto(
        Account account,
        Summoner summoner,
        List<String> matchHistory,
        List<IMatchDto.MatchDto> matchDetails,
        LeagueEntryDto rankedSolo,
        LeagueEntryDto rankedFlex,
        List<ChampionStatsDto> championStats,
        List<RoleStatsDto> roleStats
) { }
