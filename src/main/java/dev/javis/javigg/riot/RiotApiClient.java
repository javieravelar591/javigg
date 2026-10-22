package dev.javis.javigg.riot;

import java.net.URI;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The only class that talks HTTP to Riot. It handles authentication, URL encoding, client-side
 * rate limiting, 429 retries, and translating failures into {@link RiotApiException}. Services own
 * the endpoint paths and response types.
 */
@Component
public class RiotApiClient {

    private static final Logger log = LoggerFactory.getLogger(RiotApiClient.class);

    static final int MAX_RETRIES = 2;
    static final Duration MAX_RETRY_WAIT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_RETRY_AFTER = Duration.ofSeconds(1);

    private final RestTemplate restTemplate;
    private final RiotRateLimiter rateLimiter;

    public RiotApiClient(RestTemplateBuilder builder,
                         RiotRateLimiter rateLimiter,
                         @Value("${riot.api.key}") String apiKey) {
        // The key goes in a header, not the query string, so it can't leak into logs or exception
        // messages that include the request URL.
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .defaultHeader("X-Riot-Token", apiKey)
                .build();
        this.rateLimiter = rateLimiter;
    }

    /**
     * GETs {@code baseUrl + pathTemplate}, e.g.
     * {@code get(url, "/lol/match/v5/matches/{id}", MatchDto.class, matchId)}. URI variables are
     * strictly encoded, so user input like "a/../b" stays inside its path segment.
     */
    public <T> T get(String baseUrl, String pathTemplate, Class<T> responseType, Object... uriVariables) {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl + pathTemplate)
                .encode()
                .buildAndExpand(uriVariables)
                .toUri();

        for (int attempt = 0; ; attempt++) {
            rateLimiter.acquire(uri.getHost());
            try {
                return restTemplate.getForObject(uri, responseType);
            } catch (HttpClientErrorException.TooManyRequests e) {
                Duration retryAfter = retryAfter(e);
                if (attempt >= MAX_RETRIES || retryAfter.compareTo(MAX_RETRY_WAIT) > 0) {
                    log.warn("Riot 429 on {} (limit type: {}), giving up after {} attempts",
                            pathTemplate, header(e, "X-Rate-Limit-Type"), attempt + 1);
                    throw new RiotApiException(429, "Riot API rate limit reached", retryAfter, e);
                }
                log.info("Riot 429 on {}, retrying in {}", pathTemplate, retryAfter);
                rateLimiter.sleep(retryAfter.toMillis());
            } catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();
                if (status == 401 || status == 403) {
                    // Riot also returns 403 for unknown paths, so this isn't always a bad key.
                    log.error("Riot returned {} on {}: expired/invalid riot.api.key, or an unknown endpoint",
                            status, pathTemplate);
                }
                throw new RiotApiException(status, "Riot API returned " + status + " for " + pathTemplate, null, e);
            } catch (ResourceAccessException e) {
                throw new RiotApiException(503, "Riot API unreachable for " + pathTemplate, null, e);
            }
        }
    }

    private static String header(HttpClientErrorException e, String name) {
        return e.getResponseHeaders() == null ? null : e.getResponseHeaders().getFirst(name);
    }

    private static Duration retryAfter(HttpClientErrorException e) {
        String header = header(e, "Retry-After");
        if (header == null) return DEFAULT_RETRY_AFTER;
        try {
            return Duration.ofSeconds(Long.parseLong(header.trim()));
        } catch (NumberFormatException ignored) {
            return DEFAULT_RETRY_AFTER;
        }
    }
}
