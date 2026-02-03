package dev.javis.javigg.summoner;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.javis.javigg.datadragon.DataDragonService;
import dev.javis.javigg.match.dto.IMatchDto;
import dev.javis.javigg.riot.dto.Account;
import dev.javis.javigg.riot.dto.Summoner;
import dev.javis.javigg.service.MatchService;
import dev.javis.javigg.service.SummonerService;

@RestController
public class SummonerController {

    private final SummonerService summonerService;
    private final MatchService matchService;
    private final DataDragonService dataDragonService;

    public SummonerController(SummonerService summonerService,
                              MatchService matchService,
                              DataDragonService dataDragonService) {
        this.summonerService = summonerService;
        this.matchService = matchService;
        this.dataDragonService = dataDragonService;
    }

    @GetMapping("/summoner")
    public ResponseEntity<?> getSummoner(
            @RequestParam String gameName,
            @RequestParam String tagLine
    ) {

        Account account = summonerService.getAccountByRiotId(gameName, tagLine);
        Summoner summoner = summonerService.getSummonerByPuuid(account.puuid());

        List<String> matchHistory = matchService.getMatchHistory(account.puuid(), 20);
        List<IMatchDto.MatchDto> matchDetails = matchService.getMatchDetails(matchHistory);

        return ResponseEntity.ok(Map.of(
                "account", account,
                "summoner", summoner,
                "matchHistory", matchHistory,
                "matchDetails", matchDetails,
                "profileIconUrl",
                dataDragonService.getProfileIconUrl(String.valueOf(summoner.profileIconId()))
        ));
    }
}
