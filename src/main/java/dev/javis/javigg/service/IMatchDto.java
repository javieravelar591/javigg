package dev.javis.javigg.service;

import java.util.List;

public interface IMatchDto {
    record MatchDto(MetaDataDto metadata, InfoDto info) { }
    record MetaDataDto(String dataVersion, String matchId, List<String> participants) { }
    record InfoDto(String endOfGameResult, long gameDuration, String gameMode) { }
    record ParticipantDto(int damageDealtToBuildings,
                            int damageDealtToObjectives,
                            int damageDealtToTurrets,
                            int damageSelfMitigated,
                            int deaths,
                            int detectorWardsPlaced,
                            int goldEarned,
                            int goldSpent,
                            String individualPosition,
                            int item1,
                            int item2,
                            int item3,
                            int item4,
                            int item5,
                            int item6,
                            int totalDamageDealt,
                            int totalDamageDealtToChampions,
                            int totalDamageShieldedOnTeammates,
                            int totalDamageTaken,
                            int trueDamageDealt,
                            int trueDamageDealtToChampions,
                            int magicDamageDealt,
                            int magicDamageDealtToChampions,
                            int physicalDamageDealt,
                            int physicalDamageDealtToChampion
                            ) { }
}
