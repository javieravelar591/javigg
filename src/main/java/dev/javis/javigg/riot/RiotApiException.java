package dev.javis.javigg.riot;

import java.time.Duration;

/**
 * A failed call to the Riot API. {@code status} is the HTTP status Riot returned, or 503 when the
 * request never reached Riot (timeout, connection error, local rate limit exhausted).
 */
public class RiotApiException extends RuntimeException {

    private final int status;
    private final Duration retryAfter;

    public RiotApiException(int status, String message, Duration retryAfter, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.retryAfter = retryAfter;
    }

    public RiotApiException(int status, String message) {
        this(status, message, null, null);
    }

    public static RiotApiException rateLimited(Duration retryAfter) {
        return new RiotApiException(429, "Riot API rate limit reached", retryAfter, null);
    }

    public int status() {
        return status;
    }

    /** How long the caller should wait before retrying, if known. */
    public Duration retryAfter() {
        return retryAfter;
    }

    public boolean isNotFound() {
        return status == 404;
    }

    public boolean isRateLimited() {
        return status == 429;
    }
}
