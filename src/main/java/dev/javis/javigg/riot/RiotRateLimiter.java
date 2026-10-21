package dev.javis.javigg.riot;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Client-side enforcement of Riot's application rate limits so the key is never pushed into 429s
 * (repeated 429s can get a production key blacklisted).
 *
 * <p>Riot applies app limits per routing value (na1, americas, ...), so each host gets its own set
 * of sliding windows. Limits are configured as {@code requests:seconds} pairs, e.g.
 * {@code 20:1,100:120} for a development key or {@code 500:10,30000:600} for a production key.
 * Callers block until a slot frees up; if that would take longer than {@code maxWait}, the call
 * fails fast with a 429 {@link RiotApiException} instead of tying up request threads.
 */
@Component
public class RiotRateLimiter {

    private final List<Window> windowSpecs;
    private final Duration maxWait;
    private final Clock clock;
    private final Map<String, HostLimiter> hosts = new ConcurrentHashMap<>();

    @Autowired
    public RiotRateLimiter(@Value("${riot.api.rate-limits:20:1,100:120}") String limits,
                           @Value("${riot.api.rate-limit-max-wait:10s}") Duration maxWait) {
        this(parse(limits), maxWait, Clock.systemUTC());
    }

    RiotRateLimiter(List<Window> windowSpecs, Duration maxWait, Clock clock) {
        this.windowSpecs = windowSpecs;
        this.maxWait = maxWait;
        this.clock = clock;
    }

    /** Blocks until a request to {@code host} is allowed by every configured window. */
    public void acquire(String host) {
        HostLimiter limiter = hosts.computeIfAbsent(host, h -> new HostLimiter());
        long deadline = clock.millis() + maxWait.toMillis();

        while (true) {
            long waitMillis = limiter.tryAcquire(clock.millis());
            if (waitMillis == 0) return;

            if (clock.millis() + waitMillis > deadline) {
                throw RiotApiException.rateLimited(Duration.ofMillis(waitMillis));
            }
            sleep(waitMillis);
        }
    }

    void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RiotApiException(503, "Interrupted while waiting for rate limit", null, e);
        }
    }

    static List<Window> parse(String spec) {
        List<Window> windows = new ArrayList<>();
        for (String part : spec.split(",")) {
            String[] pieces = part.trim().split(":");
            if (pieces.length != 2) {
                throw new IllegalArgumentException("Invalid rate limit '" + part + "', expected requests:seconds");
            }
            windows.add(new Window(Integer.parseInt(pieces[0].trim()),
                    Duration.ofSeconds(Long.parseLong(pieces[1].trim()))));
        }
        return List.copyOf(windows);
    }

    record Window(int maxRequests, Duration length) { }

    /** Sliding-log limiter: remembers the timestamp of each request still inside a window. */
    private class HostLimiter {
        private final List<Deque<Long>> logs = windowSpecs.stream()
                .map(w -> (Deque<Long>) new ArrayDeque<Long>(w.maxRequests()))
                .toList();

        /** Records a request and returns 0, or returns how many ms to wait before trying again. */
        synchronized long tryAcquire(long now) {
            long wait = 0;
            for (int i = 0; i < windowSpecs.size(); i++) {
                Window window = windowSpecs.get(i);
                Deque<Long> log = logs.get(i);
                long windowMillis = window.length().toMillis();

                while (!log.isEmpty() && log.peekFirst() <= now - windowMillis) {
                    log.pollFirst();
                }
                if (log.size() >= window.maxRequests()) {
                    wait = Math.max(wait, log.peekFirst() + windowMillis - now);
                }
            }
            if (wait > 0) return wait;

            logs.forEach(log -> log.addLast(now));
            return 0;
        }
    }
}
