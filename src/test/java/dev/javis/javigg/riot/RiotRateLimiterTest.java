package dev.javis.javigg.riot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class RiotRateLimiterTest {

    /** Clock that only advances when the limiter "sleeps", so tests run instantly. */
    static class FakeClock extends Clock {
        long millis = 1_000_000;
        @Override public long millis() { return millis; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis); }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
    }

    static class TestLimiter extends RiotRateLimiter {
        final FakeClock clock;
        final List<Long> sleeps = new ArrayList<>();

        TestLimiter(String limits, Duration maxWait, FakeClock clock) {
            super(parse(limits), maxWait, clock);
            this.clock = clock;
        }

        @Override
        void sleep(long millis) {
            sleeps.add(millis);
            clock.millis += millis;
        }
    }

    @Test
    void allowsRequestsUpToTheLimitWithoutWaiting() {
        TestLimiter limiter = new TestLimiter("3:1", Duration.ofSeconds(10), new FakeClock());

        for (int i = 0; i < 3; i++) limiter.acquire("na1");

        assertThat(limiter.sleeps).isEmpty();
    }

    @Test
    void waitsForTheWindowToSlideOnceFull() {
        TestLimiter limiter = new TestLimiter("2:1", Duration.ofSeconds(10), new FakeClock());

        limiter.acquire("na1");
        limiter.clock.millis += 200;
        limiter.acquire("na1");
        limiter.acquire("na1");

        // The first request expires 1000ms after it was made, i.e. 800ms after the second.
        assertThat(limiter.sleeps).containsExactly(800L);
    }

    @Test
    void enforcesEveryWindow() {
        TestLimiter limiter = new TestLimiter("10:1,3:60", Duration.ofMinutes(2), new FakeClock());

        for (int i = 0; i < 4; i++) limiter.acquire("na1");

        assertThat(limiter.sleeps).containsExactly(60_000L);
    }

    @Test
    void tracksHostsIndependently() {
        TestLimiter limiter = new TestLimiter("1:1", Duration.ofSeconds(10), new FakeClock());

        limiter.acquire("na1.api.riotgames.com");
        limiter.acquire("americas.api.riotgames.com");

        assertThat(limiter.sleeps).isEmpty();
    }

    @Test
    void failsFastWhenTheWaitExceedsMaxWait() {
        TestLimiter limiter = new TestLimiter("1:120", Duration.ofSeconds(5), new FakeClock());
        limiter.acquire("na1");

        assertThatThrownBy(() -> limiter.acquire("na1"))
                .isInstanceOf(RiotApiException.class)
                .satisfies(e -> assertThat(((RiotApiException) e).isRateLimited()).isTrue());
        assertThat(limiter.sleeps).isEmpty();
    }

    @Test
    void rejectsMalformedLimits() {
        assertThatThrownBy(() -> RiotRateLimiter.parse("20/1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
