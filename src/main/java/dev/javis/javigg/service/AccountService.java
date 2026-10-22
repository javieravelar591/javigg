package dev.javis.javigg.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dev.javis.javigg.riot.RiotApiClient;
import dev.javis.javigg.riot.dto.Account;

/** Riot Account v1: Riot ID (gameName#tagLine) and PUUID lookups. */
@Service
public class AccountService {

    private final RiotApiClient riotApiClient;
    private final String accountApiUrl;

    public AccountService(RiotApiClient riotApiClient,
                          @Value("${riot.api.account.url}") String accountApiUrl) {
        this.riotApiClient = riotApiClient;
        this.accountApiUrl = accountApiUrl;
    }

    public Account getByRiotId(String gameName, String tagLine) {
        return riotApiClient.get(accountApiUrl, "/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}",
                Account.class, gameName, tagLine);
    }

    public Account getByPuuid(String puuid) {
        return riotApiClient.get(accountApiUrl, "/riot/account/v1/accounts/by-puuid/{puuid}",
                Account.class, puuid);
    }
}
