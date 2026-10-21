package dev.javis.javigg.riot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import dev.javis.javigg.riot.dto.Account;

@RestClientTest(RiotApiClient.class)
@Import(RiotRateLimiter.class)
class RiotApiClientTest {

    private static final String BASE = "https://americas.api.riotgames.com";
    private static final String PATH = "/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}";
    private static final String ACCOUNT_JSON = """
            {"puuid":"abc","gameName":"Javi","tagLine":"NA1"}""";

    @Autowired RiotApiClient client;
    @Autowired MockRestServiceServer server;

    @Test
    void sendsKeyAsHeaderNotQueryParam() {
        server.expect(requestTo(BASE + "/riot/account/v1/accounts/by-riot-id/Javi/NA1"))
                .andExpect(header("X-Riot-Token", "RGAPI-test"))
                .andRespond(withSuccess(ACCOUNT_JSON, MediaType.APPLICATION_JSON));

        Account account = client.get(BASE, PATH, Account.class, "Javi", "NA1");

        assertThat(account.puuid()).isEqualTo("abc");
        server.verify();
    }

    @Test
    void encodesUserInputSoItCannotEscapeItsPathSegment() {
        server.expect(requestTo(BASE + "/riot/account/v1/accounts/by-riot-id/..%2F..%2Flol%20x/%3Fa%3D1"))
                .andRespond(withSuccess(ACCOUNT_JSON, MediaType.APPLICATION_JSON));

        client.get(BASE, PATH, Account.class, "../../lol x", "?a=1");

        server.verify();
    }

    @Test
    void retriesAfter429ThenSucceeds() {
        HttpHeaders retryAfter = new HttpHeaders();
        retryAfter.add("Retry-After", "0");
        server.expect(requestTo(BASE + "/riot/account/v1/accounts/by-riot-id/Javi/NA1"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(retryAfter));
        server.expect(requestTo(BASE + "/riot/account/v1/accounts/by-riot-id/Javi/NA1"))
                .andRespond(withSuccess(ACCOUNT_JSON, MediaType.APPLICATION_JSON));

        Account account = client.get(BASE, PATH, Account.class, "Javi", "NA1");

        assertThat(account.gameName()).isEqualTo("Javi");
        server.verify();
    }

    @Test
    void givesUpAfterRepeated429s() {
        HttpHeaders retryAfter = new HttpHeaders();
        retryAfter.add("Retry-After", "0");
        server.expect(times(RiotApiClient.MAX_RETRIES + 1), requestTo(BASE + "/riot/account/v1/accounts/by-riot-id/Javi/NA1"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(retryAfter));

        assertThatThrownBy(() -> client.get(BASE, PATH, Account.class, "Javi", "NA1"))
                .isInstanceOf(RiotApiException.class)
                .satisfies(e -> assertThat(((RiotApiException) e).isRateLimited()).isTrue());
        server.verify();
    }

    @Test
    void doesNotRetryWhenRetryAfterIsTooLong() {
        HttpHeaders retryAfter = new HttpHeaders();
        retryAfter.add("Retry-After", "60");
        server.expect(times(1), requestTo(BASE + "/riot/account/v1/accounts/by-riot-id/Javi/NA1"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).headers(retryAfter));

        assertThatThrownBy(() -> client.get(BASE, PATH, Account.class, "Javi", "NA1"))
                .isInstanceOf(RiotApiException.class);
        server.verify();
    }

    @Test
    void translates404() {
        server.expect(requestTo(BASE + "/riot/account/v1/accounts/by-riot-id/Nobody/NA1"))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.get(BASE, PATH, Account.class, "Nobody", "NA1"))
                .isInstanceOf(RiotApiException.class)
                .satisfies(e -> assertThat(((RiotApiException) e).isNotFound()).isTrue());
    }
}
