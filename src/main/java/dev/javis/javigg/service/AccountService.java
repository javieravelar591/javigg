package dev.javis.javigg.service;

import org.springframework.stereotype.Service;

import dev.javis.javigg.riot.Region;
import dev.javis.javigg.riot.RiotApiClient;
import dev.javis.javigg.riot.dto.Account;

/** Riot Account v1: Riot ID (gameName#tagLine) and PUUID lookups. */
@Service
public class AccountService {

    private final RiotApiClient riotApiClient;

    public AccountService(RiotApiClient riotApiClient) {
        this.riotApiClient = riotApiClient;
    }

    public Account getByRiotId(String gameName, String tagLine, Region region) {
        return riotApiClient.get(region.accountMatchHost(), "/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}",
                Account.class, gameName, tagLine);
    }

    public Account getByPuuid(String puuid, Region region) {
        return riotApiClient.get(region.accountMatchHost(), "/riot/account/v1/accounts/by-puuid/{puuid}",
                Account.class, puuid);
    }
}
