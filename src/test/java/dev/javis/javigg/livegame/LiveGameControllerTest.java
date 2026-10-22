package dev.javis.javigg.livegame;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.javis.javigg.riot.Region;
import dev.javis.javigg.riot.RiotApiException;
import dev.javis.javigg.riot.dto.LiveGameLobbyDto;
import dev.javis.javigg.service.LiveGameService;

@WebMvcTest(LiveGameController.class)
class LiveGameControllerTest {

    @Autowired MockMvc mvc;
    @MockBean LiveGameService liveGameService;

    @Test
    void returnsLobby() throws Exception {
        when(liveGameService.findLiveGameLobby("abc-123", Region.NA1))
                .thenReturn(Optional.of(new LiveGameLobbyDto(42, "CLASSIC", List.of())));

        mvc.perform(get("/live-game").param("puuid", "abc-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(42));
    }

    @Test
    void notInGameIs404() throws Exception {
        when(liveGameService.findLiveGameLobby("abc", Region.NA1)).thenReturn(Optional.empty());

        mvc.perform(get("/live-game").param("puuid", "abc"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Player is not currently in a game"));
    }

    @Test
    void rejectsMalformedPuuid() throws Exception {
        mvc.perform(get("/live-game").param("puuid", "../summoner"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rateLimitIs503WithRetryAfter() throws Exception {
        when(liveGameService.findLiveGameLobby("abc", Region.NA1))
                .thenThrow(RiotApiException.rateLimited(Duration.ofSeconds(7)));

        mvc.perform(get("/live-game").param("puuid", "abc"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "7"));
    }

    @Test
    void forwardsAnExplicitRegion() throws Exception {
        when(liveGameService.findLiveGameLobby("abc-123", Region.KR))
                .thenReturn(Optional.of(new LiveGameLobbyDto(42, "CLASSIC", List.of())));

        mvc.perform(get("/live-game").param("puuid", "abc-123").param("region", "KR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(42));
    }

    @Test
    void rejectsInvalidRegionCode() throws Exception {
        mvc.perform(get("/live-game").param("puuid", "abc-123").param("region", "zz9"))
                .andExpect(status().isBadRequest());
    }
}
