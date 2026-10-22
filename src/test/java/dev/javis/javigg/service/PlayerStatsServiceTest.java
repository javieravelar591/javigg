package dev.javis.javigg.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.javis.javigg.match.dto.IMatchDto;
import dev.javis.javigg.stats.dto.ChampionStatsDto;
import dev.javis.javigg.stats.dto.RoleStatsDto;

class PlayerStatsServiceTest {

    private static final String PUUID = "me";

    private final PlayerStatsService service = new PlayerStatsService();

    @Test
    void championBreakdownGroupsByChampionAndComputesWinRate() {
        List<IMatchDto.MatchDto> matches = List.of(
                match(participant(1, "Ahri", true, "MIDDLE")),
                match(participant(1, "Ahri", true, "MIDDLE")),
                match(participant(1, "Ahri", false, "MIDDLE")),
                match(participant(2, "Lux", true, "MIDDLE")));

        List<ChampionStatsDto> stats = service.championBreakdown(PUUID, matches);

        assertThat(stats).hasSize(2);
        ChampionStatsDto ahri = stats.get(0); // most-played first
        assertThat(ahri.championId()).isEqualTo(1);
        assertThat(ahri.games()).isEqualTo(3);
        assertThat(ahri.wins()).isEqualTo(2);
        assertThat(ahri.losses()).isEqualTo(1);
        assertThat(ahri.winRate()).isEqualTo(66.7);

        ChampionStatsDto lux = stats.get(1);
        assertThat(lux.championId()).isEqualTo(2);
        assertThat(lux.games()).isEqualTo(1);
        assertThat(lux.winRate()).isEqualTo(100.0);
    }

    @Test
    void championBreakdownCapsAtTopFiveByGamesPlayed() {
        List<IMatchDto.MatchDto> matches = List.of(
                match(participant(1, "A", true, "TOP")), match(participant(1, "A", true, "TOP")),
                match(participant(2, "B", true, "TOP")), match(participant(2, "B", true, "TOP")),
                match(participant(3, "C", true, "TOP")),
                match(participant(4, "D", true, "TOP")),
                match(participant(5, "E", true, "TOP")),
                match(participant(6, "F", true, "TOP")));

        List<ChampionStatsDto> stats = service.championBreakdown(PUUID, matches);

        assertThat(stats).hasSize(PlayerStatsService.TOP_CHAMPIONS);
        assertThat(stats.get(0).championId()).isIn(1, 2);
        assertThat(stats.get(1).championId()).isIn(1, 2);
    }

    @Test
    void championBreakdownIgnoresMatchesThePlayerDidNotAppearIn() {
        List<IMatchDto.MatchDto> matches = List.of(
                match(participant("someone-else", 1, "Ahri", true, "MIDDLE")));

        assertThat(service.championBreakdown(PUUID, matches)).isEmpty();
    }

    @Test
    void roleBreakdownGroupsByTeamPositionMostPlayedFirst() {
        List<IMatchDto.MatchDto> matches = List.of(
                match(participant(1, "Ahri", true, "MIDDLE")),
                match(participant(99, "Shen", false, "TOP")),
                match(participant(1, "Ahri", true, "MIDDLE")));

        List<RoleStatsDto> stats = service.roleBreakdown(PUUID, matches);

        assertThat(stats).hasSize(2);
        assertThat(stats.get(0).role()).isEqualTo("MIDDLE");
        assertThat(stats.get(0).games()).isEqualTo(2);
        assertThat(stats.get(0).winRate()).isEqualTo(100.0);
        assertThat(stats.get(1).role()).isEqualTo("TOP");
        assertThat(stats.get(1).winRate()).isEqualTo(0.0);
    }

    @Test
    void roleBreakdownSkipsBlankTeamPosition() {
        List<IMatchDto.MatchDto> matches = List.of(match(participant(1, "Teemo", true, "")));

        assertThat(service.roleBreakdown(PUUID, matches)).isEmpty();
    }

    private static IMatchDto.MatchDto match(IMatchDto.ParticipantDto participant) {
        IMatchDto.MetaDataDto metadata = new IMatchDto.MetaDataDto("2", "NA1_1", List.of(participant.puuid()));
        IMatchDto.InfoDto info = new IMatchDto.InfoDto("GameComplete", 1800, 0, 0,
                "CLASSIC", 11, 420, "14.1.1", List.of(participant));
        return new IMatchDto.MatchDto(metadata, info);
    }

    private static IMatchDto.ParticipantDto participant(int championId, String championName, boolean win, String teamPosition) {
        return participant(PUUID, championId, championName, win, teamPosition);
    }

    // IMatchDto.ParticipantDto is a 77-field record mirroring Riot's raw Match v5 payload; named
    // locals (rather than one long positional call) keep this alignment checkable at a glance.
    private static IMatchDto.ParticipantDto participant(String puuid, int championId, String championName,
                                                         boolean win, String teamPosition) {
        int assists = 0, baronKills = 0, bountyLevel = 0, champExperience = 0, champLevel = 18;
        int damageDealtToBuildings = 0, damageDealtToObjectives = 0, damageDealtToTurrets = 0, damageSelfMitigated = 0;
        int deaths = 0, detectorWardsPlaced = 0, doubleKills = 0, dragonKills = 0, goldEarned = 0, goldSpent = 0;
        int inhibitorKills = 0, inhibitorTakedowns = 0;
        int item0 = 0, item1 = 0, item2 = 0, item3 = 0, item4 = 0, item5 = 0, item6 = 0;
        int kills = 0;
        String lane = "";
        int largestMultiKill = 0, magicDamageDealt = 0, magicDamageDealtToChampions = 0, magicDamageTaken = 0;
        int neutralMinionsKilled = 0, nexusKills = 0, nexusTakedowns = 0, pentaKills = 0;
        int physicalDamageDealt = 0, physicalDamageDealtToChampions = 0, physicalDamageTaken = 0;
        int profileIcon = 0;
        int quadraKills = 0;
        String riotIdGameName = "", riotIdTagline = "", role = "";
        int sightWardsBoughtInGame = 0, teamId = 100;
        int timePlayed = 1800;
        int totalDamageDealt = 0, totalDamageDealtToChampions = 0, totalDamageShieldedOnTeammates = 0, totalDamageTaken = 0;
        int totalHeal = 0, totalHealsOnTeammates = 0, totalMinionsKilled = 0;
        int totalTimeCCDealt = 0, totalTimeSpentDead = 0, totalUnitsHealed = 0;
        int tripleKills = 0, trueDamageDealt = 0, trueDamageDealtToChampions = 0, trueDamageTaken = 0;
        int turretKills = 0, turretTakedowns = 0, visionScore = 0, visionWardsBoughtInGame = 0;
        int wardsKilled = 0, wardsPlaced = 0;
        String individualPosition = teamPosition;
        int killingSprees = 0;
        String summonerName = "";
        int summonerLevel = 30, summoner1Id = 4, summoner2Id = 14;

        return new IMatchDto.ParticipantDto(assists, baronKills, bountyLevel, champExperience, champLevel,
                championId, championName,
                damageDealtToBuildings, damageDealtToObjectives, damageDealtToTurrets, damageSelfMitigated,
                deaths, detectorWardsPlaced, doubleKills, dragonKills, goldEarned, goldSpent,
                inhibitorKills, inhibitorTakedowns,
                item0, item1, item2, item3, item4, item5, item6,
                kills, lane, largestMultiKill,
                magicDamageDealt, magicDamageDealtToChampions, magicDamageTaken,
                neutralMinionsKilled, nexusKills, nexusTakedowns, pentaKills,
                physicalDamageDealt, physicalDamageDealtToChampions, physicalDamageTaken,
                profileIcon, puuid, quadraKills, riotIdGameName, riotIdTagline, role,
                sightWardsBoughtInGame, teamId, teamPosition, timePlayed,
                totalDamageDealt, totalDamageDealtToChampions, totalDamageShieldedOnTeammates, totalDamageTaken,
                totalHeal, totalHealsOnTeammates, totalMinionsKilled, totalTimeCCDealt, totalTimeSpentDead, totalUnitsHealed,
                tripleKills, trueDamageDealt, trueDamageDealtToChampions, trueDamageTaken,
                turretKills, turretTakedowns, visionScore, visionWardsBoughtInGame, wardsKilled, wardsPlaced,
                win, individualPosition, killingSprees, summonerName, summonerLevel, summoner1Id, summoner2Id, null);
    }
}
