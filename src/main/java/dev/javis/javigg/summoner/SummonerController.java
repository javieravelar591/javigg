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
import dev.javis.javigg.riot.dto.LeagueEntryDto;
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
        LeagueEntryDto rankedSoloEntry = summonerService.getSoloQueueEntry(account.puuid()).orElse(null);

        var response = new java.util.HashMap<String, Object>();
        response.put("account", account);
        response.put("summoner", summoner);
        response.put("matchHistory", matchHistory);
        response.put("matchDetails", matchDetails);
        response.put("profileIconUrl", dataDragonService.getProfileIconUrl(String.valueOf(summoner.profileIconId())));
        response.put("rankedSolo", rankedSoloEntry);

        return ResponseEntity.ok(response);
    }
}
