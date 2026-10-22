package dev.javis.javigg.summoner;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.javis.javigg.riot.Region;
import dev.javis.javigg.riot.RiotApiException;
import dev.javis.javigg.riot.dto.Account;
import dev.javis.javigg.riot.dto.Summoner;
import dev.javis.javigg.service.ProfileService;

@WebMvcTest(SummonerController.class)
class SummonerControllerTest {

    @Autowired MockMvc mvc;
    @MockBean ProfileService profileService;

    @Test
    void returnsProfileWithTheShapeTheClientExpects() throws Exception {
        when(profileService.getProfile("Javi", "NA1", Region.NA1)).thenReturn(new SummonerProfileDto(
                new Account("abc", "Javi", "NA1"),
                new Summoner("abc", 29, 0, 300),
                List.of(), List.of(), null));

        mvc.perform(get("/summoner").param("gameName", "Javi").param("tagLine", "NA1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.puuid").value("abc"))
                .andExpect(jsonPath("$.summoner.summonerLevel").value(300))
                .andExpect(jsonPath("$.matchDetails").isArray())
                .andExpect(jsonPath("$.rankedSolo").doesNotExist());
    }

    @Test
    void unknownSummonerIs404() throws Exception {
        when(profileService.getProfile("Nobody", "NA1", Region.NA1)).thenThrow(new RiotApiException(404, "not found"));

        mvc.perform(get("/summoner").param("gameName", "Nobody").param("tagLine", "NA1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void acceptsAndForwardsAnExplicitNonNaRegion() throws Exception {
        when(profileService.getProfile("Javi", "EUW", Region.EUW1)).thenReturn(new SummonerProfileDto(
                new Account("abc", "Javi", "EUW"),
                new Summoner("abc", 29, 0, 300),
                List.of(), List.of(), null));

        mvc.perform(get("/summoner").param("gameName", "Javi").param("tagLine", "EUW").param("region", "euw1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.account.puuid").value("abc"));
    }

    @Test
    void rejectsInvalidRegionCode() throws Exception {
        mvc.perform(get("/summoner").param("gameName", "Javi").param("tagLine", "NA1").param("region", "not-a-region"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsOverlongTagLine() throws Exception {
        mvc.perform(get("/summoner").param("gameName", "Javi").param("tagLine", "TOOLONGTAG"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsPathCharactersInRiotId() throws Exception {
        mvc.perform(get("/summoner").param("gameName", "zz").param("tagLine", "../.."))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/summoner").param("gameName", "a/b").param("tagLine", "NA1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptsUnicodeAndSpacesInGameName() throws Exception {
        mvc.perform(get("/summoner").param("gameName", "Dôûblêlift Fan").param("tagLine", "NA1"))
                .andExpect(status().isOk());
    }

    @Test
    void missingParamIs400() throws Exception {
        mvc.perform(get("/summoner").param("gameName", "Javi"))
                .andExpect(status().isBadRequest());
    }
}
